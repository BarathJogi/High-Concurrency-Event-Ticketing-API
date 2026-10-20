package com.barath.ticketingapi.service;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;


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

        // Prevent 500 crashes by checking for duplicates cleanly
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        // Create the user entity
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password())); // NEVER store plain text!
        user.setRole(UserRole.USER); // Hardcoded safely on the server side

        // Save to database
        userRepository.save(user);

        // Generate the JWT token
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // Let Spring Security check the password against the hashed DB password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // If we reach this line, the password was correct. Fetch the user.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Generate and return a new JWT token
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}
