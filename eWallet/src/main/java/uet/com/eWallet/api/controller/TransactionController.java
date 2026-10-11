package uet.com.eWallet.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uet.com.eWallet.api.dto.request.DepositRequest;
import uet.com.eWallet.api.dto.request.TransferRequest;
import uet.com.eWallet.api.dto.response.ApiResponse;
import uet.com.eWallet.api.dto.response.PageResponse;
import uet.com.eWallet.api.dto.response.TransactionResponse;
import uet.com.eWallet.business.service.TransactionService;

import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Transaction", description = "APIs relate to Transaction")
public class TransactionController {

    TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(request);

        return ResponseEntity.ok(ApiResponse.success("Chuyển tiền thành công", response));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(request);

        return ResponseEntity.ok(ApiResponse.success("Nạp tiền thành công", response));
    }

    @Operation(summary = "Lấy lịch sử giao dịch của ví hiện tại", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactionHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<TransactionResponse> history = transactionService.getHistory(page, size);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch thành công", history));
    }

    @Operation(summary = "Xem chi tiết 1 giao dịch", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID id
    ) {
        TransactionResponse response = transactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết giao dịch thành công", response));
    }

}
