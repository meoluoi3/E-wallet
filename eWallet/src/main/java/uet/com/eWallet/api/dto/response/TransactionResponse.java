package uet.com.eWallet.api.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uet.com.eWallet.data.entity.TransactionStatus;
import uet.com.eWallet.data.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {

    UUID id;

    String transactionCode;

    UUID senderWalletId;

    UUID receiverWalletId;

    BigDecimal amount;

    String currency;

    TransactionType type;

    TransactionStatus status;

    String description;

    String failureReason;

    Instant createdAt;

    Instant completedAt;
}