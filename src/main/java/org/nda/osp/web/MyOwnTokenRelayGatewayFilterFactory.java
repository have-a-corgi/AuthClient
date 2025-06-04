package org.nda.osp.web;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class MyOwnTokenRelayGatewayFilterFactory extends AbstractGatewayFilterFactory<AbstractGatewayFilterFactory.NameConfig> {
    private final ObjectProvider<ReactiveOAuth2AuthorizedClientManager> clientManagerProvider;

    public MyOwnTokenRelayGatewayFilterFactory(ObjectProvider<ReactiveOAuth2AuthorizedClientManager> clientManagerProvider) {
        super(AbstractGatewayFilterFactory.NameConfig.class);
        this.clientManagerProvider = clientManagerProvider;
    }

    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("name");
    }

    public GatewayFilter apply() {
        return this.apply((AbstractGatewayFilterFactory.NameConfig) null);
    }

    public GatewayFilter apply(AbstractGatewayFilterFactory.NameConfig config) {
        String defaultClientRegistrationId = config == null ? null : config.getName();
        log.trace("I have got name {}", defaultClientRegistrationId);
        return (exchange, chain) -> {
            log.trace("WORK THERE");
            Mono<ServerWebExchange> exchangeMono = exchange.getPrincipal().filter((principal) -> {
                        log.trace("Principal {} {}", principal, (principal instanceof Authentication));
                        return (principal instanceof Authentication);
                    }).cast(Authentication.class)
                    .flatMap((principal) -> this.authorizationRequest(defaultClientRegistrationId, principal))
                    .flatMap(this::authorizedClient).map(OAuth2AuthorizedClient::getAccessToken)
                    .map((token) -> {
                        log.trace("Token: {}", token);
                        return this.withBearerAuth(exchange, token);
                    })
                    .defaultIfEmpty(exchange);
            log.trace("WORK LATER");
            Objects.requireNonNull(chain);
            return exchangeMono.flatMap(chain::filter);
        };
    }

    private Mono<OAuth2AuthorizeRequest> authorizationRequest(String defaultClientRegistrationId, Authentication principal) {
        String clientRegistrationId = defaultClientRegistrationId;
        if (defaultClientRegistrationId == null && principal instanceof OAuth2AuthenticationToken) {
            clientRegistrationId = ((OAuth2AuthenticationToken) principal).getAuthorizedClientRegistrationId();
        }

        return Mono.justOrEmpty(clientRegistrationId).map(OAuth2AuthorizeRequest::withClientRegistrationId).map((builder) -> builder.principal(principal).build());
    }

    private Mono<OAuth2AuthorizedClient> authorizedClient(OAuth2AuthorizeRequest request) {
        ReactiveOAuth2AuthorizedClientManager clientManager = (ReactiveOAuth2AuthorizedClientManager) this.clientManagerProvider.getIfAvailable();
        return clientManager == null ? Mono.error(new IllegalStateException("No ReactiveOAuth2AuthorizedClientManager bean was found. Did you include the org.springframework.boot:spring-boot-starter-oauth2-client dependency?")) : clientManager.authorize(request);
    }

    private ServerWebExchange withBearerAuth(ServerWebExchange exchange, OAuth2AccessToken accessToken) {
        log.trace("I have got access token {}", accessToken.getTokenValue());
        return exchange.mutate().request((r) -> r.headers((headers) -> headers.setBearerAuth(accessToken.getTokenValue()))).build();
    }
}

