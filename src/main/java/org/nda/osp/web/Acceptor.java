package org.nda.osp.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@RequiredArgsConstructor
@RestController
public class Acceptor {

    private final WebClient webClient;

    @GetMapping("/hello")
    public ResponseEntity<Object> hello(
            @RegisteredOAuth2AuthorizedClient("messaging-client")
            OAuth2AuthorizedClient authorizedClient
    ) {
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        String[] block = webClient.get().uri("/messages")
                .headers(h->h.setBearerAuth(accessToken.getTokenValue()))
                .retrieve().bodyToMono(String[].class).block();
        return ResponseEntity.ok(block);
    }
}
