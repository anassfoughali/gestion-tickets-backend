package com.gestiontickets.tickets.service;

import com.gestiontickets.tickets.dto.LoginRequest;
import com.gestiontickets.tickets.dto.LoginResponse;
import com.gestiontickets.tickets.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final JwtUtil jwtUtil;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AuthService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.getUsername()) ||
                !adminPassword.equals(request.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Identifiants incorrects"
            );
        }
        String token = jwtUtil.generateToken(request.getUsername(), "ADMIN");
        return new LoginResponse(token, "ADMIN", "Administrateur");
    }
}