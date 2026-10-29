package uet.com.eWallet.business.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.data.entity.User;
import uet.com.eWallet.data.entity.UserStatus;
import uet.com.eWallet.data.repository.UserRepository;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;
import uet.com.eWallet.mapper.UserMapper;
import uet.com.eWallet.security.SecurityUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;

    UserMapper userMapper;

    WalletService walletService;


    public UserResponse getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserResponse response = userMapper.toUserResponse(user);
        response.setBalance(walletService.getMyWallet().getBalance());
        return response;
    }


    @Transactional
    public void closeAccount() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.CLOSED || user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_CLOSED);
        }

        user.setStatus(UserStatus.CLOSED);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);
    }
}
