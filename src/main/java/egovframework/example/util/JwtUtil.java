package egovframework.example.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
	private final Key key;
    private final long accessExpSeconds;
    private final long refreshExpSeconds;
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-seconds}") long accessExpSeconds,
            @Value("${jwt.refresh-exp-seconds}") long refreshExpSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpSeconds = accessExpSeconds;
        this.refreshExpSeconds = refreshExpSeconds;
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, "access", accessExpSeconds);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, "refresh", refreshExpSeconds);
    }

    private String createToken(Long memberId, String type, long expSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expSeconds);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))        
                .claim("type", type)                        
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public Long getMemberId(String token) {
        String sub = parseClaims(token).getSubject();
        return Long.parseLong(sub);
    }

    public String getTokenType(String token) {
        Object t = parseClaims(token).get("type");
        return t == null ? null : String.valueOf(t);
    }
    
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTokenType(token));
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getTokenType(token));
    }

    public Instant getExpiresAt(String token) {
        Date exp = parseClaims(token).getExpiration();
        return exp.toInstant();
    }
    
    public long getRefreshExpSeconds() {
        return refreshExpSeconds;
    }

    public Instant getRefreshExpiresAt() {
        return Instant.now().plusSeconds(refreshExpSeconds);
    }
}
