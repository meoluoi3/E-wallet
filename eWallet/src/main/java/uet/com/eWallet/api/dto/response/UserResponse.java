package uet.com.eWallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import uet.com.eWallet.data.entity.UserStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    UUID id;

    String username;

    String fullName;

    String phone;

    String email;

    UserStatus status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    BigDecimal balance;

    Instant createdAt;
}
