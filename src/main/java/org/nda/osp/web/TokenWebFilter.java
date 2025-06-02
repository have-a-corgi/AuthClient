package org.nda.osp.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Component
public class TokenWebFilter implements GlobalFilter {

    private final ReactiveClientRegistrationRepository repository;
    private final WebClient tokenWebClient;

    public TokenWebFilter(@Qualifier("tokenWebClient") WebClient tokenWebClient,
                          ReactiveClientRegistrationRepository repository) {
        this.tokenWebClient = tokenWebClient;
        this.repository = repository;
    }

    private Mono<TokenDto> token() {
        Mono<ClientRegistration> gateway = repository.findByRegistrationId("gateway");
        return gateway.flatMap(c ->
                tokenWebClient.post().uri(c.getProviderDetails().getTokenUri())
                        .header("Authorization", "Basic " +
                                Base64.getEncoder().encodeToString((c.getClientId()+":"+c.getClientSecret()).getBytes()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters.fromValue(
                                "grant_type="+c.getAuthorizationGrantType().getValue()
                                        +"&scope="+String.join(" ", c.getScopes())))
                        .retrieve().bodyToMono(TokenDto.class)
        );

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