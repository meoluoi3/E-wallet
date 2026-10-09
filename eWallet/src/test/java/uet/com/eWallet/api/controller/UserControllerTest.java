package uet.com.eWallet.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.business.service.UserService;
import uet.com.eWallet.data.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    @DisplayName("GET /users/me - Thành công trả về 200 và thông tin cá nhân")
    void getMyProfile_Success_ReturnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .username("tuandq")
                .fullName("Đinh Quang Tuân")
                .phone("0987654321")
                .email("tuan@example.com")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(userService.getMyProfile()).thenReturn(userResponse);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("tuandq"))
                .andExpect(jsonPath("$.data.fullName").value("Đinh Quang Tuân"));
    }
}
