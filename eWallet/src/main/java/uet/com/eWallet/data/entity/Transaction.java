package uet.com.eWallet.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_sender", columnList = "sender_wallet_id"),
        @Index(name = "idx_transactions_receiver", columnList = "receiver_wallet_id"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at"),
        @Index(name = "idx_transactions_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 64)
    String transactionCode;

    @Column(name = "idempotency_key", unique = true, length = 100)
    String idempotencyKey;

    @Column(name = "sender_wallet_id")
    UUID senderWalletId; // Null with TransactionType DEPOSIT

    @Column(name = "receiver_wallet_id", nullable = false)
    UUID receiverWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionStatus status;

    @Column(name = "sender_balance_after", precision = 19, scale = 4)
    BigDecimal senderBalanceAfter; // Null with TransactionType DEPOSIT

    @Column(name = "receiver_balance_after", precision = 19, scale = 4)
    BigDecimal receiverBalanceAfter;

    @Column(length = 255)
    String description;

    @Column(name = "failure_reason", length = 255)
    String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "completed_at")
    Instant completedAt;
}