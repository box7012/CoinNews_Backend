package app.message.demo1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Log log = LogFactory.getLog(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // @Override
    // protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    //         throws ServletException, IOException {

    //     String token = getJwtFromRequest(request);
    //     log.info("token" + token);

    //     if (token != null && jwtUtil.isTokenValid(token, jwtUtil.extractUsername(token))) {

    //         String username = jwtUtil.extractUsername(token);
    //         log.info("Authenticated user: " + username);  // 로그 추가
    //         // JWT가 유효하면 인증 설정
    //         UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
    //                 jwtUtil.extractUsername(token), null, new ArrayList<>());
    //         SecurityContextHolder.getContext().setAuthentication(authentication);
    //     }

    //     filterChain.doFilter(request, response);
    // }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getJwtFromRequest(request);
        log.info("token: " + token);

        if (token != null && jwtUtil.isTokenValid(token, jwtUtil.extractUsername(token))) {
            String username = jwtUtil.extractUsername(token);
            log.info("doFilterInternal Authenticated user: " + username);

            // UserDetails 객체로 로드
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 올바른 UserDetails 객체를 principal로 사용
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        String bearerToken = request.getHeader("Authorization");


        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        log.info("여기서 null이 나오나?");
        return null;
    }
}