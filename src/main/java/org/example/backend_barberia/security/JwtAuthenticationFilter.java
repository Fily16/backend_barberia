package org.example.backend_barberia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.entity.User;
import org.example.backend_barberia.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    // Rutas que NO deben ser bloqueadas por sesión inválida
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/validate"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String requestURI = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Si no hay header de autorización, continuar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        try {
            username = jwtService.extractUsername(jwt);
            log.info("=== JWT FILTER ===");
            log.info("Request URI: {}", requestURI);
            log.info("Username from token: {}", username);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Verificar que el sessionId del token coincida con el almacenado
                    String tokenSessionId = jwtService.extractSessionId(jwt);
                    User user = userRepository.findByUsername(username).orElse(null);
                    
                    log.info("Token session ID: {}", tokenSessionId);
                    log.info("DB session ID: {}", user != null ? user.getCurrentSessionToken() : "USER NOT FOUND");
                    
                    boolean sessionValid = user != null && tokenSessionId != null && 
                            tokenSessionId.equals(user.getCurrentSessionToken());
                    
                    if (sessionValid) {
                        log.info("SESSION VALID - IDs match");
                        
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        // Continuar normalmente
                        filterChain.doFilter(request, response);
                        return;
                        
                    } else {
                        // Session inválida
                        log.warn("SESSION INVALID - IDs DO NOT MATCH!");
                        log.warn("Token ID: {} vs DB ID: {}", tokenSessionId, user != null ? user.getCurrentSessionToken() : "null");
                        
                        // Si es una ruta pública, NO bloquear - dejar que el controller maneje
                        if (isPublicPath(requestURI)) {
                            log.info("Public path - NOT blocking, letting controller handle");
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        // Para rutas protegidas, retornar 401
                        log.warn("Protected path - returning 401");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Sesión iniciada en otro dispositivo\",\"code\":\"SESSION_INVALID\"}");
                        return;
                    }
                } else {
                    log.warn("Token is not valid (expired or invalid signature)");
                }
            }
        } catch (Exception e) {
            log.error("JWT Filter error: {}", e.getMessage());
            
            // Si es ruta pública, continuar aunque el token sea inválido
            if (isPublicPath(requestURI)) {
                log.info("Public path with invalid token - continuing");
                filterChain.doFilter(request, response);
                return;
            }
            
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Token inválido o expirado\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String requestURI) {
        return PUBLIC_PATHS.stream().anyMatch(requestURI::startsWith);
    }
}
