package uet.com.eWallet.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uet.com.eWallet.api.dto.request.RegisterRequest;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication APIs for login and register")
public class AuthController {

    @Operation(summary = "Register a new account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type")
    })
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@Valid @RequestBody RegisterRequest request) {
        return "Received registration request for: " + request.username();
    }
}