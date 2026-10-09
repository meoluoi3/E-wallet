package uet.com.eWallet.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uet.com.eWallet.data.entity.Wallet;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUser_Id(UUID userId);

    Optional<Wallet> findByWalletNumber(String walletNumber);

    boolean existsByUser_Id(UUID userId);

    boolean existsByWalletNumber(String walletNumber);
}
