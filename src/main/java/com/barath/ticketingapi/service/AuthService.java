package com.barath.ticketingapi.service;

import org.springframework.stereotype.Service;
import com.barath.ticketingapi.dto.AuthResponse;
import com.barath.ticketingapi.dto.LoginRequest;
import com.barath.ticketingapi.dto.RegisterRequest;
import com.barath.ticketingapi.model.User;
import com.barath.ticketingapi.model.UserRole;
import com.barath.ticketingapi.repository.UserRepository;
import com.barath.ticketingapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        // 1. Create the user entity
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password())); // NEVER store plain text!
        user.setRole(UserRole.valueOf(request.role().toUpperCase()));

        // 2. Save to database
        userRepository.save(user);

        // 3. Generate the JWT token
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Let Spring Security check the password against the hashed DB password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // 2. If we reach this line, the password was correct. Fetch the user.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate and return a new JWT token
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}
