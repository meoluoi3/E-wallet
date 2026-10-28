package uet.com.eWallet.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uet.com.eWallet.api.dto.request.DepositRequest;
import uet.com.eWallet.api.dto.request.TransferRequest;
import uet.com.eWallet.api.dto.response.PageResponse;
import uet.com.eWallet.api.dto.response.TransactionResponse;
import uet.com.eWallet.data.entity.Transaction;
import uet.com.eWallet.data.entity.TransactionStatus;
import uet.com.eWallet.data.entity.TransactionType;
import uet.com.eWallet.data.entity.Wallet;
import uet.com.eWallet.data.repository.TransactionRepository;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;
import uet.com.eWallet.mapper.TransactionMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    WalletService walletService;

    @Mock
    TransactionMapper transactionMapper;

    @InjectMocks
    TransactionService transactionService;

    Wallet senderWallet;
    Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        senderWallet = Wallet.builder().id(UUID.randomUUID()).balance(new BigDecimal("1000")).build();
        receiverWallet = Wallet.builder().id(UUID.randomUUID()).balance(new BigDecimal("500")).build();
    }

    // ---------- transfer ----------

    @Test
    void transferMovesMoneyAndRecordsSuccessfulTransaction() {
        BigDecimal amount = new BigDecimal("300");
        Wallet senderAfter = Wallet.builder().id(senderWallet.getId()).balance(new BigDecimal("700")).build();
        Wallet receiverAfter = Wallet.builder().id(receiverWallet.getId()).balance(new BigDecimal("800")).build();

        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());
        when(walletService.debit(senderWallet.getId(), amount)).thenReturn(senderAfter);
        when(walletService.credit(receiverWallet.getId(), amount)).thenReturn(receiverAfter);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        TransactionResponse mapped = TransactionResponse.builder().build();
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(mapped);

        TransactionResponse result = transactionService.transfer(transferRequest(receiverWallet.getId(), amount));

        assertSame(mapped, result);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(TransactionType.TRANSFER, saved.getType());
        assertEquals(TransactionStatus.SUCCESS, saved.getStatus());
        assertEquals(senderWallet.getId(), saved.getSenderWalletId());
        assertEquals(receiverWallet.getId(), saved.getReceiverWalletId());
        assertEquals(0, amount.compareTo(saved.getAmount()));
        assertEquals(0, new BigDecimal("700").compareTo(saved.getSenderBalanceAfter()));
        assertEquals(0, new BigDecimal("800").compareTo(saved.getReceiverBalanceAfter()));
        assertTrue(saved.getTransactionCode().startsWith("UET"));
        assertNotNull(saved.getCompletedAt());
        verify(walletService).lockForUpdate(senderWallet.getId(), receiverWallet.getId());
    }

    @Test
    void transferRejectsSendingToOwnWallet() {
        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());

        assertError(ErrorCode.INVALID_INPUT,
                () -> transactionService.transfer(transferRequest(senderWallet.getId(), BigDecimal.TEN)));

        verify(walletService, never()).lockForUpdate(any());
        verify(walletService, never()).debit(any(), any());
        verify(walletService, never()).credit(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferPropagatesInsufficientBalanceAndDoesNotCreditOrRecord() {
        BigDecimal amount = new BigDecimal("5000");
        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());
        when(walletService.debit(senderWallet.getId(), amount))
                .thenThrow(new AppException(ErrorCode.INSUFFICIENT_BALANCE));

        assertError(ErrorCode.INSUFFICIENT_BALANCE,
                () -> transactionService.transfer(transferRequest(receiverWallet.getId(), amount)));

        verify(walletService, never()).credit(any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferRejectsUnknownReceiverWallet() {
        UUID unknown = UUID.randomUUID();
        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());
        doThrow(new AppException(ErrorCode.WALLET_NOT_FOUND)).when(walletService).lockForUpdate(senderWallet.getId(), unknown);

        assertError(ErrorCode.WALLET_NOT_FOUND,
                () -> transactionService.transfer(transferRequest(unknown, BigDecimal.TEN)));

        verify(walletService, never()).debit(any(), any());
    }

    // ---------- deposit ----------

    @Test
    void depositCreditsOwnWalletAndRecordsDepositWithoutSender() {
        BigDecimal amount = new BigDecimal("250");
        Wallet after = Wallet.builder().id(senderWallet.getId()).balance(new BigDecimal("1250")).build();
        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());
        when(walletService.credit(senderWallet.getId(), amount)).thenReturn(after);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class))).thenReturn(TransactionResponse.builder().build());

        transactionService.deposit(new DepositRequest(amount, "top up"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(TransactionType.DEPOSIT, saved.getType());
        assertEquals(TransactionStatus.SUCCESS, saved.getStatus());
        assertNull(saved.getSenderWalletId());
        assertNull(saved.getSenderBalanceAfter());
        assertEquals(senderWallet.getId(), saved.getReceiverWalletId());
        assertEquals(0, new BigDecimal("1250").compareTo(saved.getReceiverBalanceAfter()));
        assertEquals("top up", saved.getDescription());
        assertTrue(saved.getTransactionCode().startsWith("VNU"));
        verify(walletService, never()).debit(any(), any());
    }

    @Test
    void depositDoesNotRecordTransactionWhenWalletIsNotActive() {
        when(walletService.getMyWalletId()).thenReturn(senderWallet.getId());
        when(walletService.credit(any(), any())).thenThrow(new AppException(ErrorCode.WALLET_NOT_ACTIVE));

        assertError(ErrorCode.WALLET_NOT_ACTIVE,
                () -> transactionService.deposit(new DepositRequest(BigDecimal.TEN, null)));

        verify(transactionRepository, never()).save(any());
    }

    // ---------- history ----------

    @Test
    void getHistoryConvertsOneBasedPageToZeroBasedAndMapsContent() {
        Transaction tx = Transaction.builder().id(UUID.randomUUID()).build();
        Page<Transaction> page = new PageImpl<>(List.of(tx), PageRequest.of(1, 5), 6);
        TransactionResponse mapped = TransactionResponse.builder().id(tx.getId()).build();
        when(walletService.getMyWallet()).thenReturn(senderWallet);
        when(transactionRepository.findAllByWalletId(any(), any(Pageable.class))).thenReturn(page);
        when(transactionMapper.toResponse(tx)).thenReturn(mapped);

        PageResponse<TransactionResponse> result = transactionService.getHistory(2, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAllByWalletId(any(), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(2, result.getPageNo());
        assertEquals(5, result.getPageSize());
        assertEquals(6, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(List.of(mapped), result.getContent());
    }

    // ---------- getTransaction ----------

    @Test
    void getTransactionAllowsSender() {
        Transaction tx = transactionBetween(senderWallet.getId(), receiverWallet.getId());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(walletService.getMyWallet()).thenReturn(senderWallet);
        TransactionResponse mapped = TransactionResponse.builder().build();
        when(transactionMapper.toResponse(tx)).thenReturn(mapped);

        assertSame(mapped, transactionService.getTransaction(tx.getId()));
    }

    @Test
    void getTransactionAllowsReceiver() {
        Transaction tx = transactionBetween(senderWallet.getId(), receiverWallet.getId());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(walletService.getMyWallet()).thenReturn(receiverWallet);
        TransactionResponse mapped = TransactionResponse.builder().build();
        when(transactionMapper.toResponse(tx)).thenReturn(mapped);

        assertSame(mapped, transactionService.getTransaction(tx.getId()));
    }

    @Test
    void getTransactionRejectsUserWhoIsNeitherSenderNorReceiver() {
        Wallet stranger = Wallet.builder().id(UUID.randomUUID()).build();
        Transaction tx = transactionBetween(senderWallet.getId(), receiverWallet.getId());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(walletService.getMyWallet()).thenReturn(stranger);

        assertError(ErrorCode.UNAUTHORIZED, () -> transactionService.getTransaction(tx.getId()));

        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    void getTransactionRejectsUnknownId() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertError(ErrorCode.TRANSACTION_NOT_FOUND, () -> transactionService.getTransaction(id));
    }

    // ---------- helpers ----------

    private TransferRequest transferRequest(UUID receiverWalletId, BigDecimal amount) {
        return new TransferRequest(receiverWalletId, amount, "test");
    }

    private Transaction transactionBetween(UUID sender, UUID receiver) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .senderWalletId(sender)
                .receiverWalletId(receiver)
                .build();
    }

    private void assertError(ErrorCode expected, Executable action) {
        AppException exception = assertThrows(AppException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }
}
