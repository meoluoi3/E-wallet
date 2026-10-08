package uet.com.eWallet.data.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uet.com.eWallet.business.port.TransactionPort;
import uet.com.eWallet.data.entity.Transaction;
import uet.com.eWallet.data.entity.TransactionType;
import uet.com.eWallet.data.repository.TransactionRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TransactionAdapter implements TransactionPort {

    private final TransactionRepository transactionRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    @Override
    public Optional<Transaction> findByCode(String transactionCode) {
        return transactionRepository.findByTransactionCode(transactionCode);
    }

    @Override
    public Page<Transaction> getTransactionsByWallet(Long walletId, Pageable pageable) {
        return transactionRepository.findAllByWalletId(walletId, pageable);
    }

    @Override
    public Page<Transaction> getTransactionsByWalletAndType(Long walletId, TransactionType type, Pageable pageable) {
        return transactionRepository.findAllByWalletIdAndType(walletId, type, pageable);
    }
}