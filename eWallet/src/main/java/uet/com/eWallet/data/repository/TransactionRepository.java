package uet.com.eWallet.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uet.com.eWallet.data.entity.Transaction;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

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
            @Param("walletId") UUID walletId,
            Pageable pageable
    );
}
