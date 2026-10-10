package uet.com.eWallet.business.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import uet.com.eWallet.data.entity.User;
import uet.com.eWallet.data.entity.UserStatus;
import uet.com.eWallet.data.entity.Wallet;
import uet.com.eWallet.data.entity.WalletStatus;
import uet.com.eWallet.data.repository.WalletRepository;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;
import uet.com.eWallet.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    WalletRepository walletRepository;

    @InjectMocks
    WalletService walletService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createWalletCreatesActiveVndWalletWithZeroBalance() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.createWallet(user);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).existsByUser_Id(user.getId());
        verify(walletRepository).save(captor.capture());
        assertSame(result, captor.getValue());
        assertSame(user, result.getUser());
        assertTrue(result.getWalletNumber().matches("W[0-9a-f]{32}"));
        verify(walletRepository).existsByWalletNumber(result.getWalletNumber());
        assertEquals(0, result.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("VND", result.getCurrency());
        assertEquals(WalletStatus.ACTIVE, result.getStatus());
        assertEquals(0L, result.getVersion());
    }

    @Test
    void createWalletRejectsExistingWallet() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(walletRepository.existsByUser_Id(user.getId())).thenReturn(true);

        assertError(ErrorCode.WALLET_ALREADY_EXISTS, () -> walletService.createWallet(user));

        verify(walletRepository, never()).save(any());
        verify(walletRepository, never()).existsByWalletNumber(anyString());
    }

    @Test
    void createWalletRejectsUserWithoutId() {
        assertError(ErrorCode.INVALID_INPUT, () -> walletService.createWallet(User.builder().build()));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void createWalletRejectsNullUser() {
        assertError(ErrorCode.INVALID_INPUT, () -> walletService.createWallet(null));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void createWalletRetriesWalletNumberCollision() {
        User user = User.builder().id(UUID.randomUUID()).build();
        when(walletRepository.existsByWalletNumber(anyString())).thenReturn(true, false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        walletService.createWallet(user);

        verify(walletRepository, times(2)).existsByWalletNumber(anyString());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void getWalletByUserIdReturnsWallet() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = wallet("0", WalletStatus.ACTIVE);
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        assertSame(wallet, walletService.getWalletByUserId(userId));
    }

    @Test
    void getWalletByUserIdRejectsMissingWallet() {
        UUID userId = UUID.randomUUID();

        assertError(ErrorCode.WALLET_NOT_FOUND, () -> walletService.getWalletByUserId(userId));

        verify(walletRepository).findByUser_Id(userId);
    }

    @Test
    void getWalletByUserIdRejectsNullId() {
        assertError(ErrorCode.INVALID_INPUT, () -> walletService.getWalletByUserId(null));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void getWalletByIdReturnsWallet() {
        Wallet wallet = wallet("0", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertSame(wallet, walletService.getWalletById(wallet.getId()));
    }

    @Test
    void getWalletByIdRejectsNullId() {
        assertError(ErrorCode.INVALID_INPUT, () -> walletService.getWalletById(null));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void getMyWalletUsesAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .username("wallet-user")
                .status(UserStatus.ACTIVE)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Wallet wallet = wallet("0", WalletStatus.ACTIVE);
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));

        assertSame(wallet, walletService.getMyWallet());
        verify(walletRepository).findByUser_Id(userId);
    }

    @Test
    void getMyWalletRejectsUnauthenticatedUser() {
        assertError(ErrorCode.UNAUTHENTICATED, () -> walletService.getMyWallet());
        verifyNoInteractions(walletRepository);
    }

    @Test
    void creditAddsPositiveAmount() {
        Wallet wallet = wallet("100000", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.credit(wallet.getId(), new BigDecimal("50000"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("150000")));
        verify(walletRepository).save(wallet);
    }

    @Test
    void creditTreatsNullBalanceAsZero() {
        Wallet wallet = wallet("0", WalletStatus.ACTIVE);
        wallet.setBalance(null);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.credit(wallet.getId(), new BigDecimal("50000"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("50000")));
        verify(walletRepository).save(wallet);
    }

    @Test
    void creditRejectsZeroAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.credit(UUID.randomUUID(), BigDecimal.ZERO));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void creditRejectsNullAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.credit(UUID.randomUUID(), null));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void creditRejectsNegativeAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.credit(UUID.randomUUID(), new BigDecimal("-1")));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void creditRejectsClosedWallet() {
        Wallet wallet = wallet("0", WalletStatus.CLOSED);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertError(ErrorCode.WALLET_NOT_ACTIVE,
                () -> walletService.credit(wallet.getId(), new BigDecimal("1")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void debitSubtractsValidAmount() {
        Wallet wallet = wallet("100000", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.debit(wallet.getId(), new BigDecimal("40000"));

        assertEquals(0, result.getBalance().compareTo(new BigDecimal("60000")));
        verify(walletRepository).save(wallet);
    }

    @Test
    void debitRejectsZeroAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.debit(UUID.randomUUID(), BigDecimal.ZERO));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void debitRejectsNullAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.debit(UUID.randomUUID(), null));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void debitRejectsNegativeAmount() {
        assertError(ErrorCode.INVALID_AMOUNT, () -> walletService.debit(UUID.randomUUID(), new BigDecimal("-1")));
        verifyNoInteractions(walletRepository);
    }

    @Test
    void debitRejectsInsufficientBalance() {
        Wallet wallet = wallet("100000", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertError(ErrorCode.INSUFFICIENT_BALANCE,
                () -> walletService.debit(wallet.getId(), new BigDecimal("100001")));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("100000")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void debitEntireBalanceLeavesZero() {
        Wallet wallet = wallet("100000", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.debit(wallet.getId(), new BigDecimal("100000"));

        assertEquals(0, result.getBalance().compareTo(BigDecimal.ZERO));
        verify(walletRepository).save(wallet);
    }

    @Test
    void debitRejectsClosedWallet() {
        Wallet wallet = wallet("100000", WalletStatus.CLOSED);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertError(ErrorCode.WALLET_NOT_ACTIVE,
                () -> walletService.debit(wallet.getId(), new BigDecimal("1")));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void closeWalletWithZeroBalance() {
        Wallet wallet = wallet("0.0000", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        Wallet result = walletService.closeWallet(wallet.getId());

        assertEquals(WalletStatus.CLOSED, result.getStatus());
        verify(walletRepository).save(wallet);
    }

    @Test
    void closeWalletRejectsPositiveBalance() {
        Wallet wallet = wallet("1", WalletStatus.ACTIVE);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertError(ErrorCode.WALLET_NOT_EMPTY, () -> walletService.closeWallet(wallet.getId()));

        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void closeWalletRejectsAlreadyClosedWallet() {
        Wallet wallet = wallet("0", WalletStatus.CLOSED);
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        assertError(ErrorCode.WALLET_NOT_ACTIVE, () -> walletService.closeWallet(wallet.getId()));

        verify(walletRepository, never()).save(any());
    }

    private Wallet wallet(String balance, WalletStatus status) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .balance(new BigDecimal(balance))
                .status(status)
                .build();
    }

    private void assertError(ErrorCode expected, Executable action) {
        AppException exception = assertThrows(AppException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }
}
