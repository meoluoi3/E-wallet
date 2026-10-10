package uet.com.eWallet.mapper;

import org.mapstruct.Mapper;
import uet.com.eWallet.api.dto.response.TransactionResponse;
import uet.com.eWallet.data.entity.Transaction;

@Mapper(componentModel = "Spring")
public interface TransactionMapper {
    TransactionResponse toResponse(Transaction transaction);
}
