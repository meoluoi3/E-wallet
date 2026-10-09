package uet.com.eWallet.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 1)
        String username,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 1)
        String password
) {}