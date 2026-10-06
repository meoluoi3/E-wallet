package uet.com.eWallet.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uet.com.eWallet.api.dto.request.RegisterRequest;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication APIs for login and register")
public class AuthController {

    @Operation(summary = "Register a new account")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@Valid @RequestBody RegisterRequest request) {
        // Return a temporary string to build the API skeleton.
        // The actual business logic will be implemented later by the Business layer.
        return "Received registration request for: " + request.username();
    }
}