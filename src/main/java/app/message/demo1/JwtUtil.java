package app.message.demo1;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;

import org.springframework.stereotype.Component;

import java.util.Date;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private static final long EXPIRATION_TIME = 86400000; // 1일

    // secretKeyFor로 안전한 비밀 키 생성
    private SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 토큰 생성
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey)  // 안전한 비밀 키 사용
                .compact();
    }

    // 토큰 검증
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()  // 최신 방식으로 변경
                .setSigningKey(secretKey)  // 안전한 비밀 키 사용
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 사용자 정보 추출 (username)
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // 토큰이 유효한지 검사
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equals(username) && !isTokenExpired(token);
    }

    // 토큰 만료 여부 검사
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
