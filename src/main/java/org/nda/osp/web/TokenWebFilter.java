package org.nda.osp.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenWebFilter implements GlobalFilter {

    private final WebClient tokenWebClient;

    public TokenWebFilter(@Qualifier("tokenWebClient") WebClient tokenWebClient) {
        this.tokenWebClient = tokenWebClient;
    }

    private Mono<TokenDto> token() {
        return tokenWebClient.post().uri("/oauth2/token")
                .header("Authorization", "Basic " + "bWVzc2FnaW5nLWNsaWVudDpzZWNyZXQ=")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue("grant_type=client_credentials&scope=openid read"))
                .retrieve().bodyToMono(TokenDto.class);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return token().map(TokenDto::getAccessToken)
                .map(t->withUserDetailsHeader(exchange, t))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange withUserDetailsHeader(ServerWebExchange exchange, String token) {
        return exchange.mutate()
                .request(r -> r.headers(h -> h.setBearerAuth(token)))
                .build();
    }

    @Getter
    @Setter
    public static class TokenDto {
        @JsonProperty("access_token")
        private String accessToken;
    }
}