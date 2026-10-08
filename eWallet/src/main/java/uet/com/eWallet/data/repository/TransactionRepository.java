package uet.com.eWallet.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uet.com.eWallet.data.entity.Transaction;
import uet.com.eWallet.data.entity.TransactionType;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds a transaction by its publicly exposed transaction code.
     *
     * The transaction code is unique and can be used to retrieve a specific
     * transaction without exposing the database ID.
     */
    Optional<Transaction> findByTransactionCode(String transactionCode);

    /**
     * Retrieves all transactions associated with a wallet, either as the
     * sender or the receiver, ordered from newest to oldest.
     *
     * Pagination is used to avoid loading the entire transaction history
     * into memory at once.
     */
    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.senderWalletId = :walletId
               OR t.receiverWalletId = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByWalletId(
            @Param("walletId") Long walletId,
            Pageable pageable
    );

    /**
     * Retrieves transactions associated with a wallet and filtered by
     * transaction type, ordered from newest to oldest.
     *
     * Pagination is used to efficiently handle large transaction histories.
     */
    @Query("""
            SELECT t
            FROM Transaction t
            WHERE (t.senderWalletId = :walletId
               OR t.receiverWalletId = :walletId)
              AND t.type = :type
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByWalletIdAndType(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            Pageable pageable
    );
}

