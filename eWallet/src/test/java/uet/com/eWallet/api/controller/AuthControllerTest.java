package uet.com.eWallet.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("POST /auth/register - Should return 201 when request is valid")
    void register_ValidRequest_ReturnsCreated() throws Exception {
        String json = """
                {
                    "username": "tuandq",
                    "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when username or password is blank")
    void register_InvalidPayload_ReturnsBadRequest() throws Exception {
        String json = """
                {
                    "username": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 415 when content type is not JSON")
    void register_UnsupportedMediaType_ReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
