package org.example.backend_barberia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend_barberia.entity.Role;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String username;
    private String fullName;
    private Role role;
    private String message;
    
    // Indica si el usuario ya tiene una sesión activa en otro dispositivo
    @Builder.Default
    private Boolean hasActiveSession = false;
}
