package uet.com.eWallet.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uet.com.eWallet.api.dto.request.LoginRequest;
import uet.com.eWallet.api.dto.request.RegisterRequest;
import uet.com.eWallet.api.dto.response.LoginResponse;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.business.service.AuthService;
import uet.com.eWallet.data.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("POST /auth/register - Thành công trả về 201 và thông tin User")
    void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "tuandq",
                "Password123!",
                "Đinh Quang Tuân",
                "0987654321",
                "tuan@example.com"
        );

        UserResponse userResponse = UserResponse.builder()
                .id(UUID.randomUUID())
                .username("tuandq")
                .fullName("Đinh Quang Tuân")
                .phone("0987654321")
                .email("tuan@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.result.username").value("tuandq"));
    }

    @Test
    @DisplayName("POST /auth/register - Thất bại khi username hoặc password rỗng")
    void register_InvalidPayload_ReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "",
                "",
                "invalid-phone",
                "invalid-email"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - Thành công trả về 200 và JWT token")
    void login_ValidCredentials_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("tuandq", "Password123!");

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("mocked.jwt.token")
                .tokenType("Bearer")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.accessToken").value("mocked.jwt.token"));
    }
}
