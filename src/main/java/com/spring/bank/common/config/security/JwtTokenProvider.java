package com.spring.bank.common.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "nexwallet.runtime.role", havingValue = "legacy-domain")
public class JwtTokenProvider {

    private final Algorithm algorithm;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String clientId, String clientName) {
        return JWT.create()
                .withSubject(clientId)
                .withClaim("name", clientName)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(expirationSeconds))
                .sign(algorithm);
    }

    public DecodedJWT validate(String token) throws JWTVerificationException {
        return JWT.require(algorithm).build().verify(token);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
