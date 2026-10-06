package uet.com.eWallet.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uet.com.eWallet.data.entity.Transaction;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
