package com.designmd.designapi.security;

import com.designmd.designapi.token.ValidatedTokenRepository;
import com.designmd.designapi.user.Role;
import com.nimbusds.jose.JWSAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final String[] PUBLIC_ENDPOINT = {"/users","/auth/token","/auth/introspect","/auth/logout"};
    private final ValidatedTokenRepository validatedTokenRepository;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                .anyRequest().permitAll()
        );
//        httpSecurity.oauth2ResourceServer(oauth2 ->oauth2
//                .jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder())
//                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
//
//                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));
        httpSecurity.csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.disable());
        return httpSecurity.build();
    }
    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter (){
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec =
                new SecretKeySpec(
                        SIGNER_KEY.getBytes(),
                        "HS512"
                );

        NimbusJwtDecoder delegate =
                NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();

        return token -> {
            var jwt = delegate.decode(token);

            String jwtId =
                    jwt.getClaimAsString(
                            JwtClaimNames.JTI
                    );

            if (jwtId != null &&
                    validatedTokenRepository
                            .existsById(jwtId)) {

                throw new JwtException(
                        "Token has been revoked"
                );
            }

            return jwt;
        };
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }
}

