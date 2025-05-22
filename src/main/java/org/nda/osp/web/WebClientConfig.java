package org.nda.osp.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("tokenWebClient")
    public WebClient webClient() {
        return WebClient.create("http://127.0.0.1:9000");
    }
    
}
