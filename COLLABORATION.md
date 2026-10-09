# 🤝 THỎA THUẬN HỢP TÁC & HỢP ĐỒNG GIAO TIẾP NỘI BỘ (TEAM COLLABORATION & INTERNAL CONTRACTS)
## DỰ ÁN: E-WALLET BACKEND (PHA 1)

> **Mục đích:** Xóa bỏ sự phụ thuộc chéo, triệt tiêu nguy cơ xung đột mã nguồn (Merge Conflict) và xác định rõ ràng: **Ai gọi ai, qua Interface nào, ai cung cấp dữ liệu gì** giữa 4 thành viên trong nhóm.

---

## 👥 1. MA TRẬN PHÂN CÔNG & QUYỀN SỞ HỮU CODE (CODE OWNERSHIP)

Mỗi thành viên là **Chủ sở hữu độc quyền (Code Owner)** của một vùng code. **Không ai được phép sửa trực tiếp Entity / Repository của người khác.**

| Thành viên | Vai trò | Vùng Code sở hữu (Packages) | Trách nhiệm chính |
|---|---|---|---|
| **Đinh Quang Tuân** | Backend / Security | `uet.com.eWallet.security.*`<br>`uet.com.eWallet.business.service.AuthService`<br>`uet.com.eWallet.business.service.UserService`<br>`uet.com.eWallet.data.entity.User`<br>`uet.com.eWallet.data.repository.UserRepository`<br>`uet.com.eWallet.api.controller.AuthController` | Quản lý danh tính, Đăng ký/Đăng nhập, JWT Filter, Cung cấp thông tin User đang đăng nhập cho cả nhóm. |
| **Đặng Duy Anh** | Backend / Wallet | `uet.com.eWallet.business.service.WalletService`<br>`uet.com.eWallet.data.entity.Wallet`<br>`uet.com.eWallet.data.repository.WalletRepository`<br>`uet.com.eWallet.api.controller.WalletController` | Quản lý số dư, Nạp tiền, Cung cấp API khóa và biến động số dư cho Tuân và Dương. |
| **Lê Tùng Dương** | Backend / Transaction | `uet.com.eWallet.business.service.TransactionService`<br>`uet.com.eWallet.data.entity.Transaction`<br>`uet.com.eWallet.data.repository.TransactionRepository`<br>`uet.com.eWallet.api.controller.TransactionController` | Quản lý luân chuyển dòng tiền P2P, Điều phối Transaction nguyên tử, Quản lý lịch sử giao dịch. |
| **Hoàng Đức Nhuận** | DevOps / QA | `uet.com.eWallet.mapper.*`<br>`uet.com.eWallet.config.OpenApiConfig`<br>`docker-compose.yml`, `Dockerfile`<br>`loadtest/*` | Chuẩn hóa DTO/Mapper (MapStruct), Cấu hình Swagger, Docker hóa hệ thống, Viết kịch bản kiểm thử tải Locust. |

---

## 🔌 2. CÁC HỢP ĐỒNG GIAO TIẾP NỘI BỘ (INTERNAL JAVA CONTRACTS / PORTS)

Để tránh giẫm chân nhau, các thành viên giao tiếp với nhau **100% thông qua Interface (Port)**.

```
Tuân (Auth/User) ──────────▶ [WalletInternalPort] ◀────────── Dương (Transaction)
                                    │
                                    ▼ (implements)
                             Duy Anh (Wallet)
```

---

### HỢP ĐỒNG 1: Tuân cung cấp cho CẢ NHÓM — `SecurityUtils`
* **Người cung cấp:** Đinh Quang Tuân.
* **Người sử dụng:** Duy Anh (Wallet), Tùng Dương (Transaction).
* **Mục đích:** Cung cấp định danh người dùng đang đăng nhập mà không bắt Duy Anh và Dương phải tự parse JWT.

```java
package uet.com.eWallet.security;

public class SecurityUtils {
    /**
     * Lấy ID của người dùng hiện tại đang gửi request.
     * @return Long userId (trích xuất an toàn từ SecurityContextHolder)
     * @throws UnauthenticatedException nếu request chưa đăng nhập
     */
    public static Long getCurrentUserId() { ... }

    /**
     * Lấy Username của người dùng hiện tại.
     */
    public static String getCurrentUsername() { ... }
}
```

* **Cách dùng thực tế của Duy Anh & Dương:**
  ```java
  // Trong WalletController hoặc TransactionController:
  Long myUserId = SecurityUtils.getCurrentUserId();
  ```

---

### HỢP ĐỒNG 2: Duy Anh cung cấp cho Tuân — Mở & Đóng Ví
* **Người cung cấp:** Đặng Duy Anh (`WalletInternalPort`).
* **Người sử dụng:** Đinh Quang Tuân (`AuthService`, `UserService`).
* **Vấn đề giải quyết:**
  1. Khi Tuân đăng ký User thành công $\rightarrow$ Tuân gọi hàm này để Duy Anh tạo ví `0 VND`. (Tuân **không tự save Wallet**).
  2. Khi Tuân xóa User $\rightarrow$ Tuân gọi hàm này để kiểm tra ví có đúng bằng `0 VND` không.

```java
package uet.com.eWallet.business.port;

import java.math.BigDecimal;

public interface WalletUserPort {
    /**
     * Tạo một ví rỗng mới gắn với User vừa đăng ký.
     * @param userId ID của người dùng mới tạo
     * @return Long walletId vừa tạo
     */
    Long createInitialWallet(Long userId);

    /**
     * Kiểm tra số dư và trạng thái ví trước khi cho phép đóng tài khoản.
     * @param userId ID người dùng muốn đóng tài khoản
     * @return true nếu số dư == 0 và ví ACTIVE; false nếu còn tiền (> 0)
     */
    boolean canCloseWallet(Long userId);

    /**
     * Đóng ví (Soft Delete -> CLOSED) khi đóng tài khoản.
     */
    void closeWallet(Long userId);
}
```

---

### HỢP ĐỒNG 3: Tuân cung cấp cho Dương — Tìm người nhận khi Chuyển tiền
* **Người cung cấp:** Đinh Quang Tuân (`UserInternalPort`).
* **Người sử dụng:** Lê Tùng Dương (`TransactionService`).
* **Vấn đề giải quyết:** Người dùng nhập SĐT hoặc Username người nhận (`recipientIdentifier`). Dương không được tự query bảng `users`, Dương gọi qua Port của Tuân.

```java
package uet.com.eWallet.business.port;

import java.util.Optional;

public interface UserInternalPort {
    /**
     * Tìm kiếm thông tin người dùng nhận tiền qua Số điện thoại hoặc Username.
     * @param identifier Số điện thoại hoặc username
     * @return Optional<UserSummary> chứa userId, fullName, phoneNumber, status
     */
    Optional<UserSummaryDto> findByIdentifier(String identifier);
}
```

---

### HỢP ĐỒNG 4: Duy Anh cung cấp cho Dương — Khóa & Biến động số dư (Chuyển tiền)
* **Người cung cấp:** Đặng Duy Anh (`WalletTransferPort`).
* **Người sử dụng:** Lê Tùng Dương (`TransactionService`).
* **Vấn đề giải quyết:** Chuyển tiền cần trừ tiền ví A, cộng tiền ví B và áp dụng **Pessimistic Locking** (`SELECT FOR UPDATE`) để chống Race Condition.

```java
package uet.com.eWallet.business.port;

import java.math.BigDecimal;

public interface WalletTransferPort {
    /**
     * Khóa 2 ví theo thứ tự ID tăng dần (chống Deadlock) và thực hiện khấu trừ/cộng tiền.
     * @param senderWalletId ID ví người gửi
     * @param receiverWalletId ID ví người nhận
     * @param amount Số tiền chuyển
     * @throws InsufficientBalanceException nếu số dư người gửi không đủ
     */
    void executeBalanceTransfer(Long senderWalletId, Long receiverWalletId, BigDecimal amount);
    
    /**
     * Lấy ID ví theo userId
     */
    Long getWalletIdByUserId(Long userId);
}
```

---

## 📦 3. QUY ƯỚC DTO & MAPPER VỚI HOÀNG ĐỨC NHUẬN (DEVOPS / QA)

Để tránh tình trạng 3 bạn Backend ngồi chờ Nhuận viết DTO rồi mới code:

1. **Về DTO Request/Response:**
   * Cả nhóm đã thống nhất **100% chuẩn hợp đồng tại [`openapi.yaml`](file:///home/tuanlala/.gemini/antigravity/scratch/E-wallet/openapi.yaml)**.
   * Mỗi bạn Backend được quyền chủ động tạo các class/record DTO trong module của mình theo đúng schema trong `openapi.yaml`.
2. **Trách nhiệm của Nhuận:**
   * Cung cấp class phản hồi chung: `ApiResponse<T>`.
   * Cấu hình MapStruct: Viết các Mapper chuyển đổi giữa Entity $\leftrightarrow$ DTO (`UserMapper`, `WalletMapper`, `TransactionMapper`).
   * Đồng bộ OpenAPI: Đảm bảo Swagger UI hiển thị đúng tài liệu.
   * Viết kịch bản Locust và Dockerfile hoàn chỉnh.

---

## 🌿 4. QUY TRÌNH PHỐI HỢP GIT (BRANCHING & COMMIT STRATEGY)

Để không bao giờ bị đè code lên nhau trên GitHub:

```
main (Bản nộp chính thức, luôn chạy được)
  └── develop (Nhánh tích hợp chung của cả nhóm)
        ├── feat/auth-security     (Đinh Quang Tuân)
        ├── feat/wallet-balance    (Đặng Duy Anh)
        ├── feat/transactions      (Lê Tùng Dương)
        └── feat/devops-qa         (Hoàng Đức Nhuận)
```

### Quy tắc làm việc hàng ngày:
1. **Tuyệt đối không push trực tiếp lên `main` và `develop`**.
2. Mỗi người tạo branch riêng từ `develop`:
   * Tuân: `git checkout -b feat/auth-security`
   * Duy Anh: `git checkout -b feat/wallet-balance`
   * Dương: `git checkout -b feat/transactions`
   * Nhuận: `git checkout -b feat/devops-qa`
3. Khi hoàn thành một tính năng:
   * Tạo **Pull Request (PR)** vào nhánh `develop`.
   * Phải có ít nhất 1 thành viên khác review và xác nhận không vỡ Interface Contract trước khi Merge.
4. **Quy ước đặt tên Commit (Conventional Commits):**
   * `feat: add jwt authentication filter`
   * `feat: implement wallet deposit logic`
   * `fix: correct balance check condition`
   * `docs: update collaboration agreement`

---

## ✅ 5. ĐIỀU KIỆN HOÀN THÀNH (DEFINITION OF DONE - DoD)

Một chức năng chỉ được coi là hoàn thành khi:
1. Tuân thủ 100% đặc tả API tại [`API_SPECIFICATION.md`](file:///home/tuanlala/.gemini/antigravity/scratch/E-wallet/API_SPECIFICATION.md) và [`openapi.yaml`](file:///home/tuanlala/.gemini/antigravity/scratch/E-wallet/openapi.yaml).
2. Không vi phạm 6 quy tắc bất biến tài chính trong [`SPECIFICATION.md`](file:///home/tuanlala/.gemini/antigravity/scratch/E-wallet/SPECIFICATION.md).
3. Đã test thành công qua Swagger UI (`/swagger-ui.html`).
4. Build Maven không lỗi: `./mvnw clean test-compile` thành công.
