package uet.com.eWallet.business.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uet.com.eWallet.data.entity.User;
import uet.com.eWallet.data.entity.Wallet;
import uet.com.eWallet.data.entity.WalletStatus;
import uet.com.eWallet.data.repository.WalletRepository;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;
import uet.com.eWallet.security.SecurityUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WalletService {

    WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(User user) {
        if (user == null || user.getId() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        if (walletRepository.existsByUser_Id(user.getId())) {
            throw new AppException(ErrorCode.WALLET_ALREADY_EXISTS);
        }

        String walletNumber;
        do {
            walletNumber = "W" + UUID.randomUUID().toString().replace("-", "");
        } while (walletRepository.existsByWalletNumber(walletNumber));

        Wallet wallet = Wallet.builder()
                .user(user)
                .walletNumber(walletNumber)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .status(WalletStatus.ACTIVE)
                .version(0L)
                .build();

        return walletRepository.save(wallet);
    }

    public Wallet getWalletByUserId(UUID userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        return walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    }

    public Wallet getWalletById(UUID walletId) {
        if (walletId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    }

    public Wallet getMyWallet() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return getWalletByUserId(currentUserId);
    }

    public UUID getMyWalletId() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return walletRepository.findIdByUserId(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    }

    /**
     * Locks the wallets in a fixed (id) order so two opposite transfers cannot deadlock.
     * Call it before loading the wallets: a wallet already loaded in this transaction keeps its stale balance.
     */
    @Transactional
    public void lockForUpdate(UUID... walletIds) {
        Arrays.stream(walletIds).sorted().forEach(this::getWalletForUpdate);
    }

    @Transactional
    public Wallet credit(UUID walletId, BigDecimal amount) {
        validateAmount(amount);
        Wallet wallet = getWalletForUpdate(walletId);
        requireActive(wallet);

        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        wallet.setBalance(balance.add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(UUID walletId, BigDecimal amount) {
        validateAmount(amount);
        Wallet wallet = getWalletForUpdate(walletId);
        requireActive(wallet);

        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        wallet.setBalance(balance.subtract(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet closeWallet(UUID walletId) {
        Wallet wallet = getWalletById(walletId);
        requireActive(wallet);

        if (wallet.getBalance() == null || wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AppException(ErrorCode.WALLET_NOT_EMPTY);
        }

        wallet.setStatus(WalletStatus.CLOSED);
        return walletRepository.save(wallet);
    }

    private Wallet getWalletForUpdate(UUID walletId) {
        if (walletId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.INVALID_AMOUNT);
        }
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_NOT_ACTIVE);
        }
    }
}
