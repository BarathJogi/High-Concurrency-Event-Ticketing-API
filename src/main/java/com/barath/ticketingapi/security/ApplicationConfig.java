package com.barath.ticketingapi.security;

import com.barath.ticketingapi.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // 1. Fetch your custom database user
            com.barath.ticketingapi.model.User dbUser = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found in database"));

            // 2. Map it explicitly to Spring Security's required UserDetails object
            return org.springframework.security.core.userdetails.User.builder()
                    .username(dbUser.getEmail())
                    .password(dbUser.getPasswordHash())
                    .roles(dbUser.getRole().name())
                    .build();
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Pass the service directly into the constructor (Fixes the 0 argument error)
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());

        // Keep the password encoder setter
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}