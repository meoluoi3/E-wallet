package uet.com.eWallet.business.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uet.com.eWallet.data.entity.Transaction;
import uet.com.eWallet.data.entity.TransactionType;

import java.util.Optional;

public interface TransactionPort {
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(Long id);

    Optional<Transaction> findByCode(String transactionCode);

    Page<Transaction> getTransactionsByWallet(Long walletId, Pageable pageable);

    Page<Transaction> getTransactionsByWalletAndType(Long walletId, TransactionType type, Pageable pageable);
}