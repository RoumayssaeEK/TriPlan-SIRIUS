package fr.triplan.bffvoyageur;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String trace = req.getHeader("X-Trace-Id");
        String user = req.getHeader("X-User");
        String role = req.getHeader("X-Role");
        log.info("[BFF-VOYAGEUR] <- {} {} depuis {} user={} role={} trace={}", req.getMethod(), req.getRequestURI(), req.getRemoteAddr(), user, role, trace);
        chain.doFilter(req, res);
        log.info("[BFF-VOYAGEUR] -> reponse {} {} status={} trace={}", req.getMethod(), req.getRequestURI(), res.getStatus(), trace);
    }
}
