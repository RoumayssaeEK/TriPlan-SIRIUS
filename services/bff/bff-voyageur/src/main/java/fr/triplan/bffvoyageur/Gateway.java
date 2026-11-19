package fr.triplan.bffvoyageur;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class Gateway {

    private static final Logger log = LoggerFactory.getLogger(Gateway.class);

    private final RestClient rest;

    public Gateway(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.rest = builder.requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String trace = header("X-Trace-Id");
                    if (trace != null) {
                        request.getHeaders().add("X-Trace-Id", trace);
                        request.getHeaders().add("X-User", header("X-User"));
                        request.getHeaders().add("X-Role", header("X-Role"));
                    }
                    request.getHeaders().add("X-Via", "BFF-VOYAGEUR");
                    log.info("[BFF-VOYAGEUR] -> appel {} {} trace={}", request.getMethod(), request.getURI(), trace);
                    var response = execution.execute(request, body);
                    log.info("[BFF-VOYAGEUR] <- retour {} status={} trace={}", request.getURI(), response.getStatusCode().value(), trace);
                    return response;
                })
                .build();
    }

    private String header(String nom) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            return req.getHeader(nom);
        }
        return null;
    }

    public JsonNode get(String url, Object... vars) {
        return rest.get().uri(url, vars).retrieve().body(JsonNode.class);
    }

    public JsonNode getWithParams(String url, Map<String, String> params) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(url);
        for (Map.Entry<String, String> e : params.entrySet()) {
            b.queryParam(e.getKey(), e.getValue());
        }
        URI uri = b.build().encode().toUri();
        return rest.get().uri(uri).retrieve().body(JsonNode.class);
    }

    public JsonNode post(String url, Object body, Object... vars) {
        return rest.post().uri(url, vars).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
    }

    public JsonNode put(String url, Object body, Object... vars) {
        return rest.put().uri(url, vars).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
    }

    public void delete(String url, Object... vars) {
        rest.delete().uri(url, vars).retrieve().toBodilessEntity();
    }

    public JsonNode deleteJson(String url, Object... vars) {
        return rest.delete().uri(url, vars).retrieve().body(JsonNode.class);
    }
}
