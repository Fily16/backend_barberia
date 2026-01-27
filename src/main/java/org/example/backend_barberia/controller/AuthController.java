package org.example.backend_barberia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.request.LoginRequest;
import org.example.backend_barberia.dto.response.ApiResponse;
import org.example.backend_barberia.dto.response.AuthResponse;
import org.example.backend_barberia.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        
        // Si tiene sesión activa, retornar con éxito pero indicando la sesión activa
        if (response.getHasActiveSession() != null && response.getHasActiveSession()) {
            return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Login exitoso", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            authService.logout(userDetails.getUsername());
        }
        return ResponseEntity.ok(ApiResponse.success("Logout exitoso", null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<AuthResponse>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Token no proporcionado"));
        }
        
        String token = authHeader.substring(7);
        AuthResponse response = authService.validateToken(token);
        
        if (response.getToken() != null) {
            return ResponseEntity.ok(ApiResponse.success("Token válido", response));
        } else {
            // Verificar si es por sesión en otro dispositivo
            String message = response.getMessage();
            if (message != null && message.contains("otro dispositivo")) {
                // Retornar 401 con código especial
                ApiResponse<AuthResponse> errorResponse = ApiResponse.error(message);
                return ResponseEntity.status(401)
                        .body(errorResponse);
            }
            
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(response.getMessage()));
        }
    }
}
