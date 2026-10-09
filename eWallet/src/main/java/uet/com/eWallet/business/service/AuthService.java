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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    JwtTokenProvider jwtTokenProvider;

    UserMapper userMapper;


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

        return userMapper.toUserResponse(user);
    }


    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.CLOSED || user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_CLOSED);
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .build();
    }
}
