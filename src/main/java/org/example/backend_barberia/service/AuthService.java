package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.dto.request.LoginRequest;
import org.example.backend_barberia.dto.response.AuthResponse;
import org.example.backend_barberia.entity.User;
import org.example.backend_barberia.repository.UserRepository;
import org.example.backend_barberia.security.CustomUserDetails;
import org.example.backend_barberia.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Username: {}", request.getUsername());
        log.info("ForceLogin: {}", request.getForceLogin());
        
        // Primero autenticar para verificar credenciales
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        log.info("User found: {}", user.getUsername());
        log.info("Current session token in DB: {}", user.getCurrentSessionToken());

        // Verificar si ya tiene una sesión activa
        boolean hasActiveSession = user.getCurrentSessionToken() != null && !user.getCurrentSessionToken().isEmpty();
        log.info("Has active session: {}", hasActiveSession);
        
        // Si tiene sesión activa y NO es forceLogin, bloquear
        if (hasActiveSession && (request.getForceLogin() == null || !request.getForceLogin())) {
            log.info("BLOCKED: User has active session and forceLogin is false/null");
            return AuthResponse.builder()
                    .token(null)
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .message("Ya tienes una sesión activa en otro dispositivo. ¿Deseas cerrarla para continuar?")
                    .hasActiveSession(true)
                    .build();
        }

        // Si llega aquí, proceder con el login (ya sea nuevo o forceLogin)
        log.info("PROCEEDING with login (new session or forceLogin)");
        
        String token = jwtService.generateToken(userDetails);
        String sessionId = jwtService.extractSessionId(token);
        
        log.info("New session ID generated: {}", sessionId);
        
        // Actualizar el sessionToken del usuario
        user.setCurrentSessionToken(sessionId);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        // Verificar que se guardó
        User savedUser = userRepository.findByUsername(request.getUsername()).orElseThrow();
        log.info("Session ID saved in DB: {}", savedUser.getCurrentSessionToken());
        log.info("=== LOGIN SUCCESS ===");

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .message("Login exitoso")
                .hasActiveSession(false)
                .build();
    }

    @Transactional
    public void logout(String username) {
        log.info("=== LOGOUT ===");
        log.info("Username: {}", username);
        
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            log.info("Clearing session token for user: {}", username);
            user.setCurrentSessionToken(null);
            userRepository.save(user);
            log.info("Session token cleared");
        }
    }

    public AuthResponse validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            
            if (user == null) {
                return AuthResponse.builder()
                        .message("Usuario no encontrado")
                        .hasActiveSession(false)
                        .build();
            }
            
            String tokenSessionId = jwtService.extractSessionId(token);
            if (tokenSessionId == null || !tokenSessionId.equals(user.getCurrentSessionToken())) {
                return AuthResponse.builder()
                        .message("Sesión cerrada. Se inició sesión en otro dispositivo.")
                        .hasActiveSession(false)
                        .build();
            }
            
            return AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .message("Token válido")
                    .hasActiveSession(false)
                    .build();
        } catch (Exception e) {
            return AuthResponse.builder()
                    .message("Token inválido")
                    .hasActiveSession(false)
                    .build();
        }
    }
}
