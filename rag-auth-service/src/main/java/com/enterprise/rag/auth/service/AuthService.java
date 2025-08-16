package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.security.JwtService;
import com.enterprise.rag.shared.dto.UserDto;
import com.enterprise.rag.shared.entity.User;
import com.enterprise.rag.shared.exception.UserNotFoundException;
import com.enterprise.rag.shared.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        logger.info("Attempting login for email: {}", request.email());

        try {
            User user = userService.findActiveByEmail(request.email());
            
            if (!SecurityUtils.verifyPassword(request.password(), user.getPasswordHash())) {
                throw new BadCredentialsException("Invalid credentials");
            }

            if (!user.isActive()) {
                throw new BadCredentialsException("Account is not active");
            }

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            userService.updateLastLogin(user.getId());

            UserDto.UserResponse userResponse = new UserDto.UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                null // tenant summary not needed for login response
            );

            logger.info("Successful login for user: {}", user.getEmail());

            return new UserDto.LoginResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiration(),
                userResponse
            );

        } catch (UserNotFoundException e) {
            logger.warn("Login attempt with non-existent email: {}", request.email());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    public UserDto.LoginResponse refreshToken(String refreshToken) {
        logger.debug("Attempting token refresh");

        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            String username = jwtService.extractUsername(refreshToken);
            User user = userService.findActiveByEmail(username);

            if (!jwtService.isTokenValid(refreshToken, username)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            UserDto.UserResponse userResponse = new UserDto.UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                null
            );

            logger.debug("Successful token refresh for user: {}", user.getEmail());

            return new UserDto.LoginResponse(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpiration(),
                userResponse
            );

        } catch (Exception e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            User user = userService.findActiveByEmail(username);
            return jwtService.isTokenValid(token, username) && user.isActive();
        } catch (Exception e) {
            return false;
        }
    }
}