package com.barath.ticketingapi.dto;


import com.barath.ticketingapi.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank
                              @Size(min = 2, max = 50)
                              String name,
                              @Size(min = 2, max = 255)
                              @NotBlank
                              @Email
                              String email,
                              @Size(min = 8, max = 50)
                              @NotBlank
                              String password) {

}
