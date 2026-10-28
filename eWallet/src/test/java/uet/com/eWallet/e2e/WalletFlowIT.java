package uet.com.eWallet.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import uet.com.eWallet.data.entity.Wallet;
import uet.com.eWallet.data.repository.TransactionRepository;
import uet.com.eWallet.data.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * End-to-end flow through the real Spring context: security filter chain, JWT, services,
 * JPA and a real PostgreSQL database. Nothing is mocked.
 *
 * Needs the database from docker-compose (localhost:5433), same as CI.
 */
@SpringBootTest
class WalletFlowIT {

    static final String PASSWORD = "Password123!";

    @Autowired
    WebApplicationContext context;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    TransactionRepository transactionRepository;

    final ObjectMapper json = new ObjectMapper();

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ---------- authentication ----------

    @Test
    @DisplayName("Đăng ký → đăng nhập → xem profile")
    void registerLoginAndReadProfile() throws Exception {
        Account account = newAccount();

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + account.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.username").value(account.username))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Đăng ký trùng username bị từ chối")
    void duplicateUsernameIsRejected() throws Exception {
        Account account = newAccount();

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registerBody(account.username, randomPhone()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("Sai mật khẩu trả 401")
    void wrongPasswordIsRejected() throws Exception {
        Account account = newAccount();

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", account.username, "password", "wrong-pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    @DisplayName("Gọi API cần đăng nhập mà không có token hoặc token sai trả 401")
    void protectedEndpointsRequireValidToken() throws Exception {
        mvc.perform(get("/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transactions").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Đóng tài khoản: không đăng nhập lại được và token cũ mất hiệu lực")
    void closedAccountCannotLoginOrUseOldToken() throws Exception {
        Account account = newAccount();

        mvc.perform(delete("/users/me").header("Authorization", "Bearer " + account.token))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", account.username, "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1006));

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + account.token))
                .andExpect(status().isUnauthorized());
    }

    // ---------- money flows ----------

    @Test
    @DisplayName("Nạp tiền rồi chuyển tiền: số dư và lịch sử hai bên đúng")
    void depositThenTransferUpdatesBalancesAndHistory() throws Exception {
        Account alice = newAccount();
        Account bob = newAccount();

        deposit(alice, "1000").andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.result.status").value("SUCCESS"));
        MvcResult transfer = transfer(alice, bob, "300").andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("TRANSFER"))
                .andReturn();

        assertBalance(alice, "700");
        assertBalance(bob, "300");

        // Alice: deposit + transfer; Bob: transfer only
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + alice.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2));
        mvc.perform(get("/transactions").header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1));

        String transactionId = json.readTree(transfer.getResponse().getContentAsString())
                .path("result").path("id").asText();
        mvc.perform(get("/transactions/" + transactionId).header("Authorization", "Bearer " + bob.token))
                .andExpect(status().isOk());

        Account stranger = newAccount();
        mvc.perform(get("/transactions/" + transactionId).header("Authorization", "Bearer " + stranger.token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1016));
    }

    @Test
    @DisplayName("Chuyển vượt số dư: bị từ chối, số dư không đổi, không ghi giao dịch")
    void transferBeyondBalanceChangesNothing() throws Exception {
        Account alice = newAccount();
        Account bob = newAccount();
        deposit(alice, "100").andExpect(status().isOk());
        long before = transactionRepository.count();

        transfer(alice, bob, "500")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(1012));

        assertBalance(alice, "100");
        assertBalance(bob, "0");
        assertEquals(before, transactionRepository.count());
    }

    @Test
    @DisplayName("Không được chuyển tiền cho chính ví của mình")
    void transferToSelfIsRejected() throws Exception {
        Account alice = newAccount();
        deposit(alice, "100").andExpect(status().isOk());

        transfer(alice, alice, "50").andExpect(status().isBadRequest());

        assertBalance(alice, "100");
    }

    @Test
    @DisplayName("Số tiền không hợp lệ bị chặn ở validation")
    void invalidAmountIsRejected() throws Exception {
        Account alice = newAccount();

        deposit(alice, "0").andExpect(status().isBadRequest());
        deposit(alice, "-5").andExpect(status().isBadRequest());

        assertBalance(alice, "0");
    }

    // ---------- concurrency ----------

    @Test
    @DisplayName("Nhiều lệnh chuyển tiền đồng thời: tiền không được sinh ra hay mất đi")
    void concurrentTransfersConserveMoney() throws Exception {
        Account alice = newAccount();
        Account bob = newAccount();
        int attempts = 20;
        BigDecimal amount = new BigDecimal("100");
        deposit(alice, "1000").andExpect(status().isOk());

        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(attempts);
        var results = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return transfer(alice, bob, amount.toPlainString()).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        int succeeded = 0;
        for (var result : results) {
            if (result.get() == 200) succeeded++;
        }
        pool.shutdown();

        BigDecimal aliceBalance = walletOf(alice).getBalance();
        BigDecimal bobBalance = walletOf(bob).getBalance();

        assertEquals(0, aliceBalance.add(bobBalance).compareTo(new BigDecimal("1000")),
                "total money must stay 1000 but was " + aliceBalance.add(bobBalance));
        assertEquals(0, bobBalance.compareTo(amount.multiply(BigDecimal.valueOf(succeeded))),
                "receiver must hold exactly succeeded * amount; succeeded=" + succeeded);
        assertEquals(0, aliceBalance.compareTo(new BigDecimal("1000").subtract(amount.multiply(BigDecimal.valueOf(succeeded)))),
                "sender balance must match succeeded transfers; succeeded=" + succeeded);
    }

    // ---------- helpers ----------

    record Account(String username, UUID userId, String token) {}

    Account newAccount() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        MvcResult registered = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registerBody(username, randomPhone()))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID userId = UUID.fromString(read(registered).path("result").path("id").asText());

        MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        String token = read(login).path("result").path("accessToken").asText();
        return new Account(username, userId, token);
    }

    Map<String, String> registerBody(String username, String phone) {
        return Map.of(
                "username", username,
                "password", PASSWORD,
                "fullName", "Test " + username,
                "phone", phone,
                "email", username + "@test.com");
    }

    String randomPhone() {
        return String.format("09%08d", ThreadLocalRandom.current().nextLong(100_000_000L));
    }

    org.springframework.test.web.servlet.ResultActions deposit(Account account, String amount) throws Exception {
        return mvc.perform(authed(post("/transactions/deposit"), account)
                .content(json.writeValueAsString(Map.of("amount", new BigDecimal(amount), "description", "e2e"))));
    }

    org.springframework.test.web.servlet.ResultActions transfer(Account from, Account to, String amount) throws Exception {
        return mvc.perform(authed(post("/transactions/transfer"), from)
                .content(json.writeValueAsString(Map.of(
                        "receiverWalletId", walletOf(to).getId(),
                        "amount", new BigDecimal(amount),
                        "description", "e2e"))));
    }

    MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, Account account) {
        return builder.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + account.token);
    }

    Wallet walletOf(Account account) {
        return walletRepository.findByUser_Id(account.userId).orElseThrow();
    }

    void assertBalance(Account account, String expected) {
        BigDecimal actual = walletOf(account).getBalance();
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected balance " + expected + " but was " + actual);
    }

    JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
