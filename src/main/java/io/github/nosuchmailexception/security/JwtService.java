package io.github.nosuchmailexception.security;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private final String secretKey = "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=";

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .issuer("Stormpath")
                .subject("msilverman")
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                .compact();
    }

    public Authentication parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            List<GrantedAuthority> authorities = ((List<?>) claims.get("roles")).stream()
                    .map(role -> new SimpleGrantedAuthority(role.toString()))
                    .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(username, null, authorities);

        } catch (ExpiredJwtException e) {
            logger.info("JWT token expired: {}", e.getMessage());
            throw new CredentialsExpiredException("JWT token expired", e);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            logger.warn("Invalid JWT signature or malformed token: {}", e.getMessage());
            throw new BadCredentialsException("Invalid JWT token", e);
        } catch (JwtException e) {
            logger.warn("Failed to parse JWT token: {}", e.getMessage());
            throw new BadCredentialsException("Invalid JWT token", e);
        }
    }
}
