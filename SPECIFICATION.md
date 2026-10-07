# 📘 TÀI LIỆU ĐẶC TẢ NGHIỆP VỤ & CA SỬ DỤNG (BUSINESS SPECIFICATION)
## DỰ ÁN: E-WALLET BACKEND (PHA 1)

> **Tài liệu bổ sung cho README.md**  
> **Mục đích:** Đặc tả chi tiết toàn bộ yêu cầu nghiệp vụ, quy tắc bất biến, luồng xử lý và hợp đồng API của hệ thống Ví điện tử Pha 1. Đảm bảo nhóm triển khai chính xác các hành vi chức năng trước khi bước vào Pha 2 (Kiểm thử tải & Tối ưu thuộc tính chất lượng).

---

## MỤC LỤC
1. [Bối cảnh & Ranh giới bài toán](#1-bối-cảnh--ranh-giới-bài-toán)
2. [Các quy tắc nghiệp vụ bất biến (System Invariants)](#2-các-quy-tắc-nghiệp-vụ-bất-biến-system-invariants)
3. [Đặc tả chi tiết các Ca sử dụng (Use Case Specifications)](#3-đặc-tả-chi-tiết-các-ca-sử-dụng-use-case-specifications)
   - [UC-01: Đăng ký tài khoản & Mở ví](#uc-01-đăng-ký-tài-khoản--mở-ví)
   - [UC-02: Đăng nhập & Xác thực JWT](#uc-02-đăng-nhập--xác-thực-jwt)
   - [UC-03: Xem thông tin ví & Số dư](#uc-03-xem-thông-tin-ví--số-dư)
   - [UC-04: Nạp tiền vào ví](#uc-04-nạp-tiền-vào-ví)
   - [UC-05: Chuyển tiền ngang hàng (P2P Transfer)](#uc-05-chuyển-tiền-ngang-hàng-p2p-transfer)
   - [UC-06: Truy vấn lịch sử giao dịch (Phân trang & Lọc)](#uc-06-truy-vấn-lịch-sử-giao-dịch-phân-trang--lọc)
   - [UC-07: Xem chi tiết một giao dịch](#uc-07-xem-chi-tiết-một-giao-dịch)
   - [UC-08: Đóng tài khoản người dùng](#uc-08-đóng-tài-khoản-người-dùng)
4. [Mô hình Dữ liệu & Sổ cái (Ledger Design)](#4-mô-hình-dữ-liệu--sổ-cái-ledger-design)
5. [Hợp đồng API & Chuẩn hóa phản hồi (API Contracts)](#5-hợp-đồng-api--chuẩn-hóa-phản-hồi-api-contracts)

---

## 1. BỐI CẢNH & RANH GIỚI BÀI TOÁN

### 1.1. Bản chất hệ thống
Hệ thống là một **Core Wallet Engine (Lõi ví điện tử nội bộ)**, phục vụ các nghiệp vụ quản lý định danh người dùng, lưu trữ số dư tài khoản và thực hiện luân chuyển dòng tiền nội bộ an toàn.

### 1.2. Ranh giới phạm vi (Scope Boundary - Pha 1)
* **Trong phạm vi (In-Scope):**
  - Quản lý danh tính cơ bản: Đăng ký, đăng nhập stateless qua JWT Token.
  - Quản lý ví: Mỗi người dùng có duy nhất 1 ví tiền tệ mặc định là **VND**.
  - Nghiệp vụ dòng tiền:
    - Nạp tiền (Deposit mô phỏng nguồn tiền nội bộ).
    - Chuyển tiền (P2P Transfer trực tiếp giữa hai ví).
  - Sổ cái kế toán: Ghi nhận lịch sử giao dịch bất biến (Audit Trail), phân trang và lọc.
  - Đóng ví an toàn khi số dư bằng 0.
* **Ngoài phạm vi (Out-of-Scope - Dành cho Pha 2 hoặc các module mở rộng):**
  - Tích hợp cổng thanh toán bên thứ ba (VNPay, Momo, Banking NAPAS).
  - Xác thực đa yếu tố (OTP SMS / Email, Biometric).
  - Ví đa tiền tệ (Multi-currency: USD, EUR,...).
  - Tính năng thấu chi (Overdraft) hoặc số dư âm có tín dụng.

---

## 2. CÁC QUY TẮC NGHIỆP VỤ BẤT BIẾN (SYSTEM INVARIANTS)

Để tránh mọi rủi ro về sai lệch tài chính, hệ thống bắt buộc phải thỏa mãn 6 quy tắc bất biến sau tại mọi thời điểm:

| Mã quy tắc | Tên quy tắc | Biểu thức / Nội dung | Ý nghĩa tài chính & Kỹ thuật |
|:---:|---|---|---|
| **INV-01** | **Non-Negative Balance** | $\forall w \in \text{Wallets}: w.balance \ge 0$ | Số dư ví không bao giờ được phép âm. Nghiêm cấm mọi hành vi chi tiêu vượt quá số dư khả dụng. |
| **INV-02** | **Strictly Positive Amount** | $\forall t \in \text{Transactions}: t.amount > 0$ | Mọi giao dịch nạp hoặc chuyển tiền bắt buộc có số tiền dương nghiêm ngặt. Nghiêm cấm số tiền $\le 0$. |
| **INV-03** | **Conservation of Money** | $\Delta \text{Balance}_{sender} + \Delta \text{Balance}_{receiver} = 0$ | Với giao dịch chuyển tiền: Tổng tiền của hai bên được bảo toàn tuyệt đối. Tiền không tự nhiên sinh ra hay mất đi. |
| **INV-04** | **Append-Only Ledger** | $\text{Immutable}(\text{Transactions})$ | Sổ cái giao dịch là bất biến: Chỉ thêm mới (`INSERT`), cấm sửa (`UPDATE`) và cấm xóa (`DELETE`). |
| **INV-05** | **Concurrency Safety** | $\text{Serializable}(\text{Transfers})$ | Hai giao dịch đồng thời trên cùng một ví phải được tuần tự hóa (Serialized), tuyệt đối không để xảy ra **Double Spending**. |
| **INV-06** | **Zero Balance on Closure** | $\text{Close}(w) \iff w.balance = 0$ | Ví chỉ được phép đóng khi số dư chính xác bằng 0 và áp dụng **Soft Delete** để bảo toàn lịch sử giao dịch. |

---

## 3. ĐẶC TẢ CHI TIẾT CÁC CA SỬ DỤNG (USE CASE SPECIFICATIONS)

---

### UC-01: ĐĂNG KÝ TÀI KHOẢN & MỞ VÍ
* **Endpoint:** `POST /auth/register`
* **Quyền truy cập:** Public (Khách vãng lai).
* **Mô tả:** Người dùng đăng ký một tài khoản mới trên hệ thống. Khi tài khoản được tạo thành công, hệ thống phải tự động khởi tạo 1 ví điện tử tương ứng đi kèm với số dư ban đầu là 0 VND.
* **Tiền điều kiện:** 
  - `username`, `phoneNumber`, `email` chưa tồn tại trong hệ thống.
* **Tham số đầu vào:**
  | Tên trường | Kiểu dữ liệu | Ràng buộc validation | Bắt buộc |
  |---|---|---|:---:|
  | `username` | String | Độ dài từ 4 - 30 ký tự, chỉ gồm chữ cái, số và dấu gạch dưới | Có |
  | `password` | String | Độ dài tối thiểu 8 ký tự, ít nhất 1 chữ hoa, 1 chữ thường, 1 số | Có |
  | `fullName` | String | Độ dài từ 2 - 100 ký tự | Có |
  | `phoneNumber` | String | Định dạng số điện thoại Việt Nam (10 chữ số, đầu số 03, 05, 07, 08, 09) | Có |
  | `email` | String | Định dạng email hợp lệ | Có |
* **Luồng chính (Main Flow):**
  1. Client gửi request đăng ký với các trường thông tin trên.
  2. Hệ thống kiểm tra định dạng đầu vào (Jakarta Validation).
  3. Hệ thống kiểm tra tính duy nhất của `username`, `phoneNumber`, `email`.
  4. Hệ thống băm mật khẩu bằng thuật toán **BCrypt** (salt rounds $\ge 10$).
  5. Trong một Database Transaction:
     - Tạo bản ghi `User` với trạng thái `ACTIVE`.
     - Tạo bản ghi `Wallet` liên kết với `User.id`, `balance = 0.00`, `currency = "VND"`, `status = ACTIVE`.
  6. Hệ thống trả về thông tin người dùng vừa tạo (không bao gồm password) kèm `walletId`, HTTP Status `201 Created`.
* **Luồng ngoại lệ (Exceptions):**
  - Dữ liệu không hợp lệ (sai format, thiếu trường) $\rightarrow$ Trả về mã lỗi `1001` (HTTP `400 Bad Request`).
  - Trùng `username`, `phoneNumber` hoặc `email` $\rightarrow$ Trả về mã lỗi `1005` (HTTP `409 Conflict`).

---

### UC-02: ĐĂNG NHẬP & XÁC THỰC JWT
* **Endpoint:** `POST /auth/login`
* **Quyền truy cập:** Public.
* **Mô tả:** Xác thực người dùng bằng `username` và `password`. Trả về JWT Access Token để người dùng sử dụng ở các API yêu cầu xác thực.
* **Tham số đầu vào:**
  | Tên trường | Kiểu dữ liệu | Ràng buộc | Bắt buộc |
  |---|---|---|:---:|
  | `username` | String | Không rỗng | Có |
  | `password` | String | Không rỗng | Có |
* **Luồng chính (Main Flow):**
  1. Client gửi `username` và `password`.
  2. Hệ thống tìm kiếm bản ghi `User` theo `username`.
  3. Hệ thống kiểm tra trạng thái tài khoản: phải là `ACTIVE` (nếu là `CLOSED` hoặc `LOCKED` thì từ chối).
  4. Hệ thống so khớp mật khẩu bằng `BCryptPasswordEncoder.matches()`.
  5. Nếu khớp, hệ thống tạo **JWT Token**:
     - Thuật toán: `HS256`.
     - Claims: `sub = userId`, `username = username`, `iat = currentTime`, `exp = currentTime + 3600` (hạn 60 phút).
  6. Trả về `accessToken`, `tokenType = "Bearer"`, `expiresIn = 3600`, HTTP Status `200 OK`.
* **Luồng ngoại lệ (Exceptions):**
  - Sai username hoặc password $\rightarrow$ Trả về mã lỗi `1002` (HTTP `401 Unauthorized`).
  - Tài khoản đã bị đóng/khóa $\rightarrow$ Trả về mã lỗi `1003` (HTTP `403 Forbidden`).

---

### UC-03: XEM THÔNG TIN VÍ & SỐ DƯ
* **Endpoint:** `GET /accounts/me`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Người dùng tra cứu thông tin ví, chủ sở hữu và số dư khả dụng hiện tại.
* **Tiền điều kiện:** JWT hợp lệ, tài khoản đang ở trạng thái `ACTIVE`.
* **Luồng chính (Main Flow):**
  1. Hệ thống trích xuất `userId` từ `SecurityContextHolder`.
  2. Truy vấn thực thể `Wallet` tương ứng với `userId`.
  3. Trả về DTO thông tin ví: `walletId`, `userId`, `ownerName`, `balance`, `currency`, `status`, HTTP Status `200 OK`.
* **Luồng ngoại lệ (Exceptions):**
  - Token không hợp lệ hoặc hết hạn $\rightarrow$ Trả về mã lỗi `1002` (HTTP `401 Unauthorized`).
  - Không tìm thấy ví tương ứng $\rightarrow$ Trả về mã lỗi `1004` (HTTP `404 Not Found`).

---

### UC-04: NẠP TIỀN VÀO VÍ
* **Endpoint:** `POST /accounts/me/deposits`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Người dùng nạp thêm tiền vào ví của chính mình (mô phỏng nguồn tiền nạp từ ngân hàng/cổng thanh toán).
* **Tham số đầu vào:**
  | Tên trường | Kiểu dữ liệu | Ràng buộc | Bắt buộc |
  |---|---|---|:---:|
  | `amount` | BigDecimal | Tối thiểu $10.000\text{ VND}$, tối đa $50.000.000\text{ VND}$/lần | Có |
  | `description` | String | Tối đa 255 ký tự | Không |
* **Luồng chính (Main Flow):**
  1. Hệ thống trích xuất `userId` từ JWT Token.
  2. Kiểm tra trạng thái ví của người dùng phải là `ACTIVE`.
  3. Kiểm tra tính hợp lệ của `amount` ($10.000 \le amount \le 50.000.000$).
  4. Thực hiện trong một Transaction nguyên tử (`@Transactional`):
     - Khóa bản ghi ví (`SELECT FOR UPDATE` hoặc cộng dồn trực tiếp).
     - Tăng số dư ví: $balance \leftarrow balance + amount$.
     - Tạo bản ghi `Transaction`:
       - `transactionCode`: UUID ngẫu nhiên (ví dụ: `DEP-9a2c-4f1b`).
       - `senderWalletId`: `NULL` (nguồn tiền bên ngoài).
       - `receiverWalletId`: `myWallet.id`.
       - `amount`: `amount`.
       - `type`: `DEPOSIT`.
       - `status`: `SUCCESS`.
       - `description`: Nội dung nạp tiền.
  5. Trả về chi tiết giao dịch nạp tiền vừa hoàn tất, HTTP Status `201 Created`.
* **Luồng ngoại lệ (Exceptions):**
  - `amount` nhỏ hơn 10.000 hoặc vượt quá 50.000.000 $\rightarrow$ Mã lỗi `1001` (HTTP `400 Bad Request`).
  - Ví đang bị khóa/đóng $\rightarrow$ Mã lỗi `1003` (HTTP `403 Forbidden`).

---

### UC-05: CHUYỂN TIỀN NGANG HÀNG (P2P TRANSFER)
* **Endpoint:** `POST /transfers`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Chuyển tiền từ ví người dùng hiện tại sang ví người dùng khác trong hệ thống. Đây là ca nghiệp vụ quan trọng nhất, đòi hỏi tính toàn vẹn và chống Race Condition cao nhất.
* **Tham số đầu vào:**
  | Tên trường | Kiểu dữ liệu | Ràng buộc | Bắt buộc |
  |---|---|---|:---:|
  | `recipientIdentifier` | String | Số điện thoại hoặc Username của người nhận | Có |
  | `amount` | BigDecimal | Tối thiểu $1.000\text{ VND}$, tối đa $100.000.000\text{ VND}$/lần | Có |
  | `description` | String | Tối đa 255 ký tự | Không |
* **Quy tắc nghiệp vụ kiểm tra (Validation Rules):**
  1. Người gửi không được phép tự chuyển tiền cho chính mình (Sender $\ne$ Receiver).
  2. Người nhận phải tồn tại và ví người nhận phải có trạng thái `ACTIVE`.
  3. Số dư khả dụng của người gửi phải $\ge amount$.
* **Luồng chính (Main Flow):**
  1. Hệ thống lấy `senderUserId` từ token.
  2. Tìm kiếm người nhận theo `recipientIdentifier` (tìm theo `phoneNumber` trước, nếu không thấy tìm theo `username`).
  3. Kiểm tra điều kiện: `senderUserId != recipientUser.id` (nếu trùng $\rightarrow$ Báo lỗi `2002`).
  4. Mở một Database Transaction nguyên tử (`@Transactional`):
     - **Cơ chế Khóa chống Deadlock & Race Condition:**
       - Lấy ID của 2 ví: `id1 = min(senderWalletId, receiverWalletId)` và `id2 = max(...)`.
       - Thực hiện khóa lần lượt theo thứ tự tăng dần của ID (`SELECT ... FOR UPDATE` ví có ID nhỏ hơn trước, sau đó khóa ví có ID lớn hơn). Việc này loại trừ hoàn toàn khả năng xảy ra **Deadlock** khi hai người dùng chuyển tiền chéo nhau đồng thời.
     - Sau khi có khóa: Kiểm tra lại số dư ví người gửi $balance_{sender} \ge amount$ (nếu không đủ $\rightarrow$ Báo lỗi `2001`, Rollback).
     - Khấu trừ ví người gửi: $balance_{sender} \leftarrow balance_{sender} - amount$.
     - Tăng số dư ví người nhận: $balance_{receiver} \leftarrow balance_{receiver} + amount$.
     - Tạo bản ghi `Transaction`:
       - `transactionCode`: UUID ngẫu nhiên (ví dụ: `TRF-4e7a-8b1c`).
       - `senderWalletId`: `senderWallet.id`.
       - `receiverWalletId`: `receiverWallet.id`.
       - `amount`: `amount`.
       - `type`: `TRANSFER`.
       - `status`: `SUCCESS`.
       - `description`: Nội dung chuyển tiền.
  5. Commit Transaction, trả về kết quả chuyển tiền kèm số dư còn lại của người gửi, HTTP Status `201 Created`.
* **Luồng ngoại lệ (Exceptions):**
  - Tự chuyển tiền cho chính mình $\rightarrow$ Mã lỗi `2002` (HTTP `422 Unprocessable Entity`).
  - Số dư khả dụng không đủ $\rightarrow$ Mã lỗi `2001` (HTTP `422 Unprocessable Entity`).
  - Không tìm thấy người nhận $\rightarrow$ Mã lỗi `1004` (HTTP `404 Not Found`).
  - Ví người nhận bị khóa/đóng $\rightarrow$ Mã lỗi `1001` (HTTP `400 Bad Request`).

---

### UC-06: TRUY VẤN LỊCH SỬ GIAO DỊCH (PHÂN TRANG & LỌC)
* **Endpoint:** `GET /transactions`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Lấy danh sách giao dịch mà người dùng tham gia (là bên gửi hoặc bên nhận), hỗ trợ phân trang và lọc theo loại giao dịch.
* **Tham số Query (Query Parameters):**
  | Tên tham số | Kiểu dữ liệu | Mặc định | Mô tả |
  |---|---|:---:|---|
  | `page` | Integer | `0` | Chỉ số trang (bắt đầu từ 0) |
  | `size` | Integer | `10` | Số phần tử mỗi trang (tối đa 50) |
  | `type` | String | `ALL` | Bộ lọc: `ALL`, `DEPOSIT`, `TRANSFER` |
* **Quy tắc phân loại chiều dòng tiền (`direction`):**
  - Nếu `myWalletId == receiverWalletId` và `senderWalletId == NULL` $\rightarrow$ Giao dịch Nạp (`type = DEPOSIT`, `direction = IN`).
  - Nếu `myWalletId == receiverWalletId` và `senderWalletId != NULL` $\rightarrow$ Nhận chuyển tiền (`type = TRANSFER`, `direction = IN`).
  - Nếu `myWalletId == senderWalletId` $\rightarrow$ Chuyển tiền đi (`type = TRANSFER`, `direction = OUT`).
* **Luồng chính (Main Flow):**
  1. Hệ thống xác định `myWalletId` từ JWT.
  2. Thực thi câu lệnh truy vấn phân trang:  
     `SELECT t FROM Transaction t WHERE (t.senderWalletId = :myWalletId OR t.receiverWalletId = :myWalletId) [AND t.type = :type] ORDER BY t.createdAt DESC`.
  3. Ánh xạ từng bản ghi sang DTO `TransactionResponse`, tính toán trường `direction` (`IN`/`OUT`) và thông tin `counterpart` (Đối tác giao dịch: tên + SĐT người nhận/gửi).
  4. Trả về cấu trúc phân trang chuẩn Spring Page (`content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`), HTTP Status `200 OK`.

---

### UC-07: XEM CHI TIẾT MỘT GIAO DỊCH
* **Endpoint:** `GET /transactions/{id}`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Xem chi tiết đầy đủ của một giao dịch cụ thể theo ID.
* **Quy tắc bảo mật chống IDOR (Insecure Direct Object References):**
  - Người dùng **chỉ được phép xem** giao dịch nếu `myWalletId == senderWalletId` HOẶC `myWalletId == receiverWalletId`.
  - Tuyệt đối không cho phép User A xem chi tiết giao dịch của User B và User C.
* **Luồng chính (Main Flow):**
  1. Hệ thống lấy `myWalletId` từ JWT.
  2. Truy vấn giao dịch theo `{id}`.
  3. Nếu không tìm thấy $\rightarrow$ Báo lỗi `1004` (HTTP `404 Not Found`).
  4. Kiểm tra quyền sở hữu: Nếu `myWalletId != senderWalletId` VÀ `myWalletId != receiverWalletId` $\rightarrow$ Từ chối truy cập bằng mã lỗi `1003` (HTTP `403 Forbidden`).
  5. Nếu hợp lệ $\rightarrow$ Trả về chi tiết giao dịch, HTTP Status `200 OK`.

---

### UC-08: ĐÓNG TÀI KHOẢN NGƯỜI DÙNG
* **Endpoint:** `DELETE /accounts/me`
* **Quyền truy cập:** Yêu cầu đăng nhập (JWT).
* **Mô tả:** Đóng vĩnh viễn tài khoản và ví điện tử của người dùng.
* **Tiền điều kiện:** 
  - Số dư ví **bắt buộc phải chính xác bằng 0.00 VND** ($balance == 0$).
  - Không có giao dịch đang ở trạng thái xử lý (`PENDING` hoặc `PROCESSING`).
* **Luồng chính (Main Flow):**
  1. Hệ thống lấy `userId` và `walletId` từ JWT.
  2. Kiểm tra số dư ví:
     - Nếu $balance > 0 \rightarrow$ Từ chối thao tác, thông báo yêu cầu chuyển hoặc rút hết tiền trước khi đóng tài khoản (Mã lỗi `1001`, HTTP `400 Bad Request`).
  3. Trong một Database Transaction:
     - Cập nhật trạng thái `Wallet`: $status \leftarrow \text{CLOSED}$.
     - Cập nhật trạng thái `User`: $status \leftarrow \text{CLOSED}$.
  4. Áp dụng cơ chế **Soft Delete**: Không xóa bản ghi khỏi DB để bảo toàn khóa ngoại của các giao dịch trong quá khứ mà người dùng này từng tham gia.
  5. Trả về HTTP Status `204 No Content`.

---

## 4. MÔ HÌNH DỮ LIỆU & SỔ CÁI (LEDGER DESIGN)

### 4.1. Tại sao tách biệt bảng `users` và `wallets`?
Mô hình 1-1 giữa User và Wallet được thiết kế tách rời vì 3 lý do kiến trúc cốt lõi:
1. **Phân tách trách nhiệm (Separation of Concerns):** Bảng `users` quản lý thông tin định danh (Identity/Auth), còn bảng `wallets` quản lý tài sản tài chính (Ledger Balance).
2. **Hiệu năng khóa (Row-Level Locking):** Khi thực hiện chuyển tiền, hệ thống chỉ cần khóa dòng dữ liệu trên bảng `wallets` (`SELECT FOR UPDATE`), giải phóng bảng `users` cho các tác vụ đọc thông tin người dùng khác mà không bị nghẽn (Block).
3. **Mở rộng tương lai (Pha 2):** Dễ dàng mở rộng cho phép một User sở hữu nhiều ví (ví dụ: ví điểm thưởng, ví voucher, ví đa tiền tệ) mà không làm vỡ schema ban đầu.

### 4.2. Đặc tả Chi tiết Cơ sở dữ liệu (PostgreSQL Schema)

```sql
-- 1. BẢNG NGƯỜI DÙNG (USERS)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED')),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. BẢNG VÍ ĐIỆN TỬ (WALLETS)
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0.00),
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED')),
    version BIGINT NOT NULL DEFAULT 0, -- Dùng cho Optimistic Locking
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. BẢNG SỔ CÁI GIAO DỊCH (TRANSACTIONS)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_code VARCHAR(64) NOT NULL UNIQUE,
    sender_wallet_id BIGINT REFERENCES wallets(id), -- NULL khi là DEPOSIT
    receiver_wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0.00),
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- CHỈ MỤC TỐI ƯU TRUY VẤN LỊCH SỬ GIAO DỊCH
CREATE INDEX idx_transactions_sender ON transactions(sender_wallet_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
```

---

## 5. HỢP ĐỒNG API & CHUẨN HÓA PHẢN HỒI (API CONTRACTS)

### 5.1. Định dạng phản hồi chuẩn (`ApiResponse<T>`)
Mọi API trả về đều tuân thủ cấu trúc đồng nhất:
```json
{
  "code": 1000,
  "message": "Thao tác thành công",
  "result": { }
}
```

### 5.2. Danh mục Mã lỗi Ứng dụng (Domain Error Codes)

| HTTP Status | App Code | Tên lỗi | Mô tả chi tiết |
|:---:|:---:|---|---|
| `200` / `201` | `1000` | `SUCCESS` | Yêu cầu thực thi thành công |
| `400` | `1001` | `VALIDATION_ERROR` | Dữ liệu đầu vào sai định dạng hoặc vi phạm ràng buộc min/max |
| `401` | `1002` | `UNAUTHENTICATED` | Token JWT không hợp lệ, bị sửa đổi hoặc đã hết hạn |
| `403` | `1003` | `ACCESS_DENIED` | Không có quyền truy cập tài nguyên của người khác (vi phạm IDOR) |
| `404` | `1004` | `NOT_FOUND` | Không tìm thấy User, Wallet hoặc Transaction được yêu cầu |
| `409` | `1005` | `DUPLICATE_RESOURCE` | `username`, `phoneNumber` hoặc `email` đã tồn tại |
| `422` | `2001` | `INSUFFICIENT_BALANCE` | Số dư khả dụng trong ví không đủ để thực hiện giao dịch chuyển tiền |
| `422` | `2002` | `SELF_TRANSFER_FORBIDDEN` | Người dùng cố gắng tự chuyển tiền cho chính tài khoản của mình |
| `500` | `9999` | `INTERNAL_SERVER_ERROR`| Lỗi hệ thống nội bộ chưa được định danh |

---

### 5.3. Chi tiết Request / Response Payload cho 8 Endpoint

#### 1. `POST /auth/register`
* **Request:**
  ```json
  {
    "username": "tuan_dinh",
    "password": "Password123@",
    "fullName": "Đinh Quang Tuân",
    "phoneNumber": "0987654321",
    "email": "tuan@example.com"
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "code": 1000,
    "message": "Đăng ký tài khoản thành công",
    "result": {
      "id": 1,
      "username": "tuan_dinh",
      "fullName": "Đinh Quang Tuân",
      "phoneNumber": "0987654321",
      "email": "tuan@example.com",
      "walletId": 1,
      "balance": 0.00
    }
  }
  ```

#### 2. `POST /auth/login`
* **Request:**
  ```json
  {
    "username": "tuan_dinh",
    "password": "Password123@"
  }
  ```
* **Response (200 OK):**
  ```json
  {
    "code": 1000,
    "message": "Đăng nhập thành công",
    "result": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
  }
  ```

#### 3. `GET /accounts/me`
* **Header:** `Authorization: Bearer <token>`
* **Response (200 OK):**
  ```json
  {
    "code": 1000,
    "message": "Lấy thông tin ví thành công",
    "result": {
      "walletId": 1,
      "userId": 1,
      "ownerName": "Đinh Quang Tuân",
      "balance": 500000.00,
      "currency": "VND",
      "status": "ACTIVE"
    }
  }
  ```

#### 4. `POST /accounts/me/deposits`
* **Header:** `Authorization: Bearer <token>`
* **Request:**
  ```json
  {
    "amount": 200000.00,
    "description": "Nạp tiền ngân hàng mô phỏng"
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "code": 1000,
    "message": "Nạp tiền thành công",
    "result": {
      "transactionId": 101,
      "transactionCode": "DEP-7f3b8a1c",
      "type": "DEPOSIT",
      "amount": 200000.00,
      "currentBalance": 700000.00,
      "createdAt": "2026-10-07T22:15:00"
    }
  }
  ```

#### 5. `POST /transfers`
* **Header:** `Authorization: Bearer <token>`
* **Request:**
  ```json
  {
    "recipientIdentifier": "0912345678",
    "amount": 150000.00,
    "description": "Chuyển tiền ăn tối"
  }
  ```
* **Response (201 Created):**
  ```json
  {
    "code": 1000,
    "message": "Chuyển tiền thành công",
    "result": {
      "transactionId": 102,
      "transactionCode": "TRF-9c2e4a8b",
      "type": "TRANSFER",
      "amount": 150000.00,
      "recipientName": "Lê Tùng Dương",
      "remainingBalance": 550000.00,
      "createdAt": "2026-10-07T22:16:00"
    }
  }
  ```

#### 6. `GET /transactions`
* **Header:** `Authorization: Bearer <token>`
* **Query Params:** `?page=0&size=10&type=ALL`
* **Response (200 OK):**
  ```json
  {
    "code": 1000,
    "message": "Lấy lịch sử giao dịch thành công",
    "result": {
      "content": [
        {
          "id": 102,
          "transactionCode": "TRF-9c2e4a8b",
          "type": "TRANSFER",
          "direction": "OUT",
          "amount": 150000.00,
          "counterpart": "Lê Tùng Dương (0912345678)",
          "description": "Chuyển tiền ăn tối",
          "status": "SUCCESS",
          "createdAt": "2026-10-07T22:16:00"
        },
        {
          "id": 101,
          "transactionCode": "DEP-7f3b8a1c",
          "type": "DEPOSIT",
          "direction": "IN",
          "amount": 200000.00,
          "counterpart": "Nạp tiền hệ thống",
          "description": "Nạp tiền ngân hàng mô phỏng",
          "status": "SUCCESS",
          "createdAt": "2026-10-07T22:15:00"
        }
      ],
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  }
  ```

#### 7. `GET /transactions/{id}`
* **Header:** `Authorization: Bearer <token>`
* **Response (200 OK):**
  ```json
  {
    "code": 1000,
    "message": "Lấy chi tiết giao dịch thành công",
    "result": {
      "id": 102,
      "transactionCode": "TRF-9c2e4a8b",
      "type": "TRANSFER",
      "amount": 150000.00,
      "senderName": "Đinh Quang Tuân",
      "receiverName": "Lê Tùng Dương",
      "status": "SUCCESS",
      "description": "Chuyển tiền ăn tối",
      "createdAt": "2026-10-07T22:16:00"
    }
  }
  ```

#### 8. `DELETE /accounts/me`
* **Header:** `Authorization: Bearer <token>`
* **Response (204 No Content):** Body rỗng.
