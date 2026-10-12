package uet.com.eWallet.business.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionService {

    TransactionRepository transactionRepository;
    WalletService walletService;
    TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        Wallet senderWallet = walletService.getMyWallet();
        Wallet receiverWallet = walletService.getWalletById(request.getReceiverWalletId());

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Wallet updateSenderWallet = walletService.debit(senderWallet.getId(), request.getAmount());
        Wallet updateReceiverWallet = walletService.credit(receiverWallet.getId(), request.getAmount());

        Transaction transaction = Transaction.builder()
                .transactionCode("UET" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase())
                .senderWalletId(senderWallet.getId())
                .receiverWalletId(receiverWallet.getId())
                .amount(request.getAmount())
                .currency("VND")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .senderBalanceAfter(updateSenderWallet.getBalance())
                .receiverBalanceAfter(updateReceiverWallet.getBalance())
                .description(request.getDescription())
                .completedAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transaction);
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        Wallet userWallet = walletService.getMyWallet();

        Wallet updateWallet = walletService.credit(userWallet.getId(), request.getAmount());

        Transaction transaction = Transaction.builder()
                .transactionCode("VNU" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase())
                .senderWalletId(null)
                .receiverWalletId(userWallet.getId())
                .amount(request.getAmount())
                .currency("VND")
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .senderBalanceAfter(null)
                .receiverBalanceAfter(updateWallet.getBalance())
                .description(request.getDescription())
                .completedAt(Instant.now())
                .build();

        transaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(transaction);
    }

    public PageResponse<TransactionResponse> getHistory(int page, int size) {
        Wallet userWallet = walletService.getMyWallet();

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Transaction> transactionPage = transactionRepository.findAllByWalletId(userWallet.getId(), pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();

        return PageResponse.<TransactionResponse>builder()
                .content(content)
                .pageNo(page)
                .pageSize(size)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .isLast(transactionPage.isLast())
                .build();
    }

    public TransactionResponse getTransaction(UUID id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        Wallet userWallet = walletService.getMyWallet();

        boolean isSender = userWallet.getId().equals(transaction.getSenderWalletId());
        boolean isReceiver = userWallet.getId().equals(transaction.getReceiverWalletId());

        if (!isSender && !isReceiver) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return transactionMapper.toResponse(transaction);
    }
}
