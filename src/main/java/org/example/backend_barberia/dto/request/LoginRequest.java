package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "El usuario es requerido")
    private String username;
    
    @NotBlank(message = "La contraseña es requerida")
    private String password;
    
    // Si es true, cierra la sesión anterior y permite el nuevo login
    private Boolean forceLogin = false;
}
