package de.innologic.i18nservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtSecurityProperties jwtProperties,
            JwtPrincipalAccessor principalAccessor,
            ProblemResponseWriter problemResponseWriter,
            @Value("${app.runtime.public:false}") boolean runtimePublic
    ) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        problemResponseWriter.unauthorized(response, "Missing or invalid JWT"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        problemResponseWriter.forbidden(response, "Insufficient permissions")));

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll();
            if (runtimePublic) {
                auth.requestMatchers(HttpMethod.GET, "/api/v1/*/translations/*").permitAll();
            }
            auth.requestMatchers(HttpMethod.GET, "/api/v1/**").hasAuthority("SCOPE_i18n:read");
            auth.requestMatchers(HttpMethod.POST, "/api/v1/**").hasAuthority("SCOPE_i18n:admin");
            auth.requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAuthority("SCOPE_i18n:admin");
            auth.requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAuthority("SCOPE_i18n:admin");
            auth.anyRequest().authenticated();
        });

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        http.addFilterAfter(new JwtActorFilter(principalAccessor), BearerTokenAuthenticationFilter.class);
        http.addFilterAfter(new JwtClaimsEnforcementFilter(jwtProperties, problemResponseWriter), JwtActorFilter.class);
        http.addFilterAfter(new TenantIsolationFilter(principalAccessor, problemResponseWriter, runtimePublic), JwtClaimsEnforcementFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(JwtSecurityProperties properties) {
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator(properties),
                issuerValidator(properties),
                audienceValidator(properties),
                requiredClaimsValidator()
        );

        if (StringUtils.hasText(properties.getJwksUri())) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwksUri()).build();
            decoder.setJwtValidator(validator);
            return decoder;
        }
        if (StringUtils.hasText(properties.getIssuerUri())) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(properties.getIssuerUri()).build();
            decoder.setJwtValidator(validator);
            return decoder;
        }
        return token -> {
                throw new JwtException("JWT decoder is not configured. Set security.jwt.issuer-uri or security.jwt.jwks-uri.");
            };
    }

    private OAuth2TokenValidator<Jwt> timestampValidator(JwtSecurityProperties properties) {
        return new JwtTimestampValidator(Duration.ofSeconds(Math.max(0, properties.getClockSkewSeconds())));
    }

    private OAuth2TokenValidator<Jwt> issuerValidator(JwtSecurityProperties properties) {
        String issuer = StringUtils.hasText(properties.getIssuerUri()) ? properties.getIssuerUri() : properties.getIssuer();
        return jwt -> {
            if (!StringUtils.hasText(issuer)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Issuer is not configured", null));
            }
            if (jwt.getIssuer() == null || !issuer.equals(jwt.getIssuer().toString())) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid issuer", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(JwtSecurityProperties properties) {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            if (audience == null || audience.stream().noneMatch(properties.getAudience()::equals)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator() {
        return jwt -> {
            if (jwt.getIssuedAt() == null || jwt.getExpiresAt() == null) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required time claims", null));
            }
            if (!StringUtils.hasText(jwt.getSubject())) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing sub", null));
            }
            if (!StringUtils.hasText(jwt.getClaimAsString("jti"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing jti", null));
            }
            if (!StringUtils.hasText(jwt.getClaimAsString("tenant_id"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing tenant_id", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
            Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }
            List<String> scp = jwt.getClaimAsStringList("scp");
            if (scp != null) {
                scp.stream()
                        .filter(StringUtils::hasText)
                        .map(scope -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + scope))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }
}
