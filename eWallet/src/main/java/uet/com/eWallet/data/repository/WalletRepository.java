package uet.com.eWallet.data.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uet.com.eWallet.data.entity.Wallet;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUser_Id(UUID userId);

    @Query("select w.id from Wallet w where w.user.id = :userId")
    Optional<UUID> findIdByUserId(@Param("userId") UUID userId);

    /** SELECT ... FOR UPDATE: concurrent transactions wait here until this one commits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByUser_Id(UUID userId);

    boolean existsByWalletNumber(String walletNumber);
}
