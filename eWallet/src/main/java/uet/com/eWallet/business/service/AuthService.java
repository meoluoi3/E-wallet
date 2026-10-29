package uet.com.eWallet.business.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uet.com.eWallet.api.dto.request.LoginRequest;
import uet.com.eWallet.api.dto.request.RegisterRequest;
import uet.com.eWallet.api.dto.response.LoginResponse;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.data.entity.User;
import uet.com.eWallet.data.entity.UserStatus;
import uet.com.eWallet.data.repository.UserRepository;
import uet.com.eWallet.exception.AppException;
import uet.com.eWallet.exception.ErrorCode;
import uet.com.eWallet.mapper.UserMapper;
import uet.com.eWallet.security.JwtTokenProvider;
import uet.com.eWallet.security.LoginAttemptService;
import uet.com.eWallet.security.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    JwtTokenProvider jwtTokenProvider;

    UserMapper userMapper;

    WalletService walletService;

    LoginAttemptService loginAttemptService;


    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (userRepository.existsByPhone(request.phone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        if (StringUtils.hasText(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = User.builder()
                .username(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone().trim())
                .email(StringUtils.hasText(request.email()) ? request.email().trim() : null)
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        walletService.createWallet(user);

        return userMapper.toUserResponse(user);
    }


    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        loginAttemptService.assertNotLocked(username);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(username);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        loginAttemptService.reset(username);

        if (user.getStatus() == UserStatus.CLOSED || user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_CLOSED);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .build();
    }


    /** Revokes every token issued so far for the current user, on all devices. */
    @Transactional
    public void logout() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }
}
