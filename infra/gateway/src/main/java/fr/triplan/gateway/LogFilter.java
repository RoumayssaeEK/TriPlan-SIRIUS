package fr.triplan.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LogFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String trace = UUID.randomUUID().toString().substring(0, 8);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Trace-Id", trace)
                .header("X-Via", "API-GATEWAY")
                .build();

        log.info("[API-GATEWAY] <- {} {} trace={}", request.getMethod(), request.getURI().getPath(), trace);

        return chain.filter(exchange.mutate().request(request).build())
                .then(Mono.fromRunnable(() ->
                        log.info("[API-GATEWAY] -> reponse {} trace={}", exchange.getResponse().getStatusCode(), trace)));
    }
}
