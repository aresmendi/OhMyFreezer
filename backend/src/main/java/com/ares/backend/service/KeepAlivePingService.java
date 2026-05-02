package com.ares.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class KeepAlivePingService {

    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 600_000) // cada 10 minutos
    public void ping() {
        if (renderUrl == null || renderUrl.isBlank()) {
            return;
        }
        try {
            restTemplate.getForObject(renderUrl + "/ping", String.class);
            log.debug("Keep-alive ping OK → {}/ping", renderUrl);
        } catch (Exception e) {
            log.warn("Keep-alive ping falló: {}", e.getMessage());
        }
    }
}
