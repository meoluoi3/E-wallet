package uet.com.eWallet.api.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uet.com.eWallet.data.entity.UserStatus;

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

    Instant createdAt;
}