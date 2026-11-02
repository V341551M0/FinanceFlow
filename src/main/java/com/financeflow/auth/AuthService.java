package com.financeflow.auth;

import com.financeflow.auth.dto.*;
import com.financeflow.common.exception.BusinessException;
import com.financeflow.security.JwtService;
import com.financeflow.user.User;
import com.financeflow.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       JwtService jwt, AuthenticationManager authManager) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.authManager = authManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }
        var user = User.builder()
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .passwordHash(encoder.encode(req.password()))
                .build();
        users.save(user);
        return issueTokens(user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(
                req.email().trim().toLowerCase(), req.password()));
        return issueTokens(req.email().trim().toLowerCase());
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwt.isTokenValid(refreshToken, "refresh")) {
            throw new BusinessException("Refresh token inválido ou expirado.");
        }
        var email = jwt.extractUsername(refreshToken);
        return issueTokens(email);
    }

    private AuthResponse issueTokens(String email) {
        return AuthResponse.bearer(jwt.generateToken(email), jwt.generateRefreshToken(email));
    }
}