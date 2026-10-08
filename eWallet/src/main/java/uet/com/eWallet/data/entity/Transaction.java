package uet.com.eWallet.data.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_sender", columnList = "sender_wallet_id"),
        @Index(name = "idx_transactions_receiver", columnList = "receiver_wallet_id"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 64)
    String transactionCode;

    @Column(name = "sender_wallet_id")
    Long senderWalletId; // Null with TransactionType Deposit

    @Column(name = "receiver_wallet_id", nullable = false)
    Long receiverWalletId;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    TransactionStatus status;

    @Column(length = 255)
    String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}