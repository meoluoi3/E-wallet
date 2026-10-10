package uet.com.eWallet.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uet.com.eWallet.api.dto.request.DepositRequest;
import uet.com.eWallet.api.dto.request.TransferRequest;
import uet.com.eWallet.api.dto.response.ApiResponse;
import uet.com.eWallet.api.dto.response.TransactionResponse;
import uet.com.eWallet.business.service.TransactionService;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Transaction", description = "API liên quan đến giao dịch ")
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
}
