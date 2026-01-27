package org.example.backend_barberia.exception;

public class SessionInvalidException extends RuntimeException {
    
    public SessionInvalidException() {
        super("Sesión iniciada en otro dispositivo");
    }
    
    public SessionInvalidException(String message) {
        super(message);
    }
}
