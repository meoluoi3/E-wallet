# 📡 ĐẶC TẢ CHI TIẾT REST API (API SPECIFICATION)
## DỰ ÁN: E-WALLET BACKEND (PHA 1)

> **Tài liệu tham chiếu kỹ thuật dành cho Backend Developer, Frontend/Client và QA/DevOps (Kịch bản Locust).**  
> Mọi endpoint đều tuân thủ nguyên tắc RESTful, dữ liệu trao đổi qua định dạng **JSON (UTF-8)** và được bao bọc bởi cấu trúc phản hồi chuẩn `ApiResponse<T>`.

---

## 📑 MỤC LỤC
1. [Quy chuẩn thiết kế API chung](#1-quy-chuẩn-thiết-kế-api-chung)
2. [Cấu trúc phản hồi chung & Danh mục Mã lỗi](#2-cấu-trúc-phản-hồi-chung--danh-mục-mã-lỗi)
3. [Chi tiết 8 Endpoints dự kiến](#3-chi-tiết-8-endpoints-dự-kiến)
   - [3.1. POST /auth/register — Đăng ký tài khoản & Mở ví](#31-post-authregister--đăng-ký-tài-khoản--mở-ví)
   - [3.2. POST /auth/login — Đăng nhập & Lấy JWT Token](#32-post-authlogin--đăng-nhập--lấy-jwt-token)
   - [3.3. GET /accounts/me — Truy vấn thông tin ví & Số dư](#33-get-accountsme--truy-vấn-thông-tin-ví--số-dư)
   - [3.4. POST /accounts/me/deposits — Nạp tiền vào ví](#34-post-accountsmedeposits--nạp-tiền-vào-ví)
   - [3.5. POST /transfers — Chuyển tiền ngang hàng (P2P)](#35-post-transfers--chuyển-tiền-ngang-hàng-p2p)
   - [3.6. GET /transactions — Lịch sử giao dịch (Phân trang & Lọc)](#36-get-transactions--lịch-sử-giao-dịch-phân-trang--lọc)
   - [3.7. GET /transactions/{id} — Chi tiết một giao dịch](#37-get-transactionsid--chi-tiết-một-giao-dịch)
   - [3.8. DELETE /accounts/me — Đóng tài khoản người dùng](#38-delete-accountsme--đóng-tài-khoản-người-dùng)
4. [Tích hợp Swagger UI & OpenAPI 3](#4-tích-hợp-swagger-ui--openapi-3)

---

## 1. QUY CHUẨN THIẾT KẾ API CHUNG

* **Base URL:** `http://localhost:8080` (môi trường Local / Docker).
* **Tiêu chuẩn Header chung:**
  - Request chứa body: `Content-Type: application/json; charset=UTF-8`
  - Request có xác thực: `Authorization: Bearer <accessToken>`
* **Quy chuẩn kiểu dữ liệu:**
  - **Tiền tệ:** Định dạng `BigDecimal` làm tròn 2 chữ số thập phân (Ví dụ: `150000.00`), đơn vị cơ sở mặc định: **VND**.
  - **Thời gian:** Chuẩn ISO 8601 UTC/Local (`YYYY-MM-DDTHH:mm:ss`, ví dụ: `2026-10-07T22:30:00`).
  - **ID:** Số nguyên dương lớn (`Long` / `BigInt`).

---

## 2. CẤU TRÚC PHẢN HỒI CHUNG & DANH MỤC MÃ LỖI

### 2.1. Cấu trúc bao bọc (`ApiResponse<T>`)

Mọi phản hồi từ hệ thống (kể cả thành công và thất bại) đều trả về cấu trúc thống nhất:

```json
{
  "code": 1000,
  "message": "Thông điệp mô tả kết quả xử lý",
  "result": { ... }
}
```
* `code` (Integer): Mã nghiệp vụ nội bộ của hệ thống.
* `message` (String): Thông báo dễ hiểu dành cho người dùng / Client.
* `result` (Object / Array / null): Dữ liệu trả về khi thành công. Khi xảy ra lỗi, trường này có thể là `null` hoặc danh sách chi tiết lỗi validation.

### 2.2. Danh mục Mã lỗi toàn hệ thống (Global Error Matrix)

| HTTP Status | App Code | Tên lỗi | Mô tả chi tiết |
|:---:|:---:|---|---|
| `200 OK` / `201 Created` | `1000` | `SUCCESS` | Yêu cầu xử lý thành công |
| `400 Bad Request` | `1001` | `VALIDATION_ERROR` | Dữ liệu đầu vào sai định dạng, thiếu trường bắt buộc hoặc vi phạm min/max |
| `401 Unauthorized` | `1002` | `UNAUTHENTICATED` | Chưa truyền header xác thực hoặc Token JWT không hợp lệ / hết hạn |
| `403 Forbidden` | `1003` | `ACCESS_DENIED` | Không có quyền truy cập tài nguyên của người khác (vi phạm IDOR) |
| `404 Not Found` | `1004` | `NOT_FOUND` | Không tìm thấy User, Ví hoặc Giao dịch được chỉ định |
| `409 Conflict` | `1005` | `DUPLICATE_RESOURCE` | `username`, `phoneNumber` hoặc `email` đã tồn tại trên hệ thống |
| `422 Unprocessable` | `2001` | `INSUFFICIENT_BALANCE` | Số dư khả dụng trong ví không đủ để thực hiện chuyển tiền |
| `422 Unprocessable` | `2002` | `SELF_TRANSFER_FORBIDDEN` | Người dùng cố gắng tự chuyển tiền cho chính bản thân mình |
| `500 Server Error` | `9999` | `INTERNAL_SERVER_ERROR`| Lỗi hệ thống nội bộ chưa xác định |

---

## 3. CHI TIẾT 8 ENDPOINTS DỰ KIẾN

---

### 3.1. `POST /auth/register` — Đăng ký tài khoản & Mở ví
* **Chức năng:** Tạo tài khoản người dùng mới và tự động kích hoạt 1 ví điện tử với số dư `0.00 VND`.
* **Xác thực:** **Public** (Không cần Token).
* **Headers:** `Content-Type: application/json`

#### Tham số Request Body:
| Trường | Kiểu | Bắt buộc | Ràng buộc Validation | Ý nghĩa |
|---|---|:---:|---|---|
| `username` | String | Có | `@NotBlank`, dài từ 4 - 30 ký tự, `^[a-zA-Z0-9_]+$` | Tên đăng nhập duy nhất |
| `password` | String | Có | `@NotBlank`, tối thiểu 8 ký tự | Mật khẩu tài khoản (sẽ được băm BCrypt) |
| `fullName` | String | Có | `@NotBlank`, tối đa 100 ký tự | Họ và tên người dùng |
| `phoneNumber`| String | Có | `@NotBlank`, regex: `^(0[35789])[0-9]{8}$` | Số điện thoại duy nhất định danh chuyển tiền |
| `email` | String | Có | `@NotBlank`, `@Email`, tối đa 100 ký tự | Địa chỉ email duy nhất |

* **Request Body mẫu:**
```json
{
  "username": "tuan_dinh",
  "password": "Password123@",
  "fullName": "Đinh Quang Tuân",
  "phoneNumber": "0987654321",
  "email": "tuan@example.com"
}
```

* **Response thành công (HTTP `201 Created`):**
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

* **Response lỗi thường gặp:**
  - `400 Bad Request` (`code: 1001`): Mật khẩu dưới 8 ký tự hoặc SĐT sai format.
  - `409 Conflict` (`code: 1005`): Username hoặc SĐT đã tồn tại.

* **cURL mẫu:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tuan_dinh",
    "password": "Password123@",
    "fullName": "Đinh Quang Tuân",
    "phoneNumber": "0987654321",
    "email": "tuan@example.com"
  }'
```

---

### 3.2. `POST /auth/login` — Đăng nhập & Lấy JWT Token
* **Chức năng:** Xác thực thông tin đăng nhập và cấp phát JWT Access Token (hạn 60 phút).
* **Xác thực:** **Public**.
* **Headers:** `Content-Type: application/json`

#### Tham số Request Body:
| Trường | Kiểu | Bắt buộc | Ràng buộc | Ý nghĩa |
|---|---|:---:|---|---|
| `username` | String | Có | `@NotBlank` | Tên đăng nhập |
| `password` | String | Có | `@NotBlank` | Mật khẩu dạng plain text |

* **Request Body mẫu:**
```json
{
  "username": "tuan_dinh",
  "password": "Password123@"
}
```

* **Response thành công (HTTP `200 OK`):**
```json
{
  "code": 1000,
  "message": "Đăng nhập thành công",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJ0dWFuX2RpbmgiLCJpYXQiOjE3OTI1OTYwMDAsImV4cCI6MTc5MjU5OTYwMH0.signature",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

* **Response lỗi thường gặp:**
  - `401 Unauthorized` (`code: 1002`): Sai username hoặc password.
  - `403 Forbidden` (`code: 1003`): Tài khoản đang bị khóa hoặc đã đóng.

---

### 3.3. `GET /accounts/me` — Truy vấn thông tin ví & Số dư
* **Chức năng:** Lấy thông tin ví và số dư khả dụng của người dùng đang đăng nhập.
* **Xác thực:** **Yêu cầu JWT Token**.
* **Headers:** `Authorization: Bearer <accessToken>`

* **Response thành công (HTTP `200 OK`):**
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

* **cURL mẫu:**
```bash
curl -X GET http://localhost:8080/accounts/me \
  -H "Authorization: Bearer <token>"
```

---

### 3.4. `POST /accounts/me/deposits` — Nạp tiền vào ví
* **Chức năng:** Nạp tiền vào ví của chính mình (mô phỏng nguồn tiền bên ngoài).
* **Xác thực:** **Yêu cầu JWT Token**.
* **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`

#### Tham số Request Body:
| Trường | Kiểu | Bắt buộc | Ràng buộc Validation | Ý nghĩa |
|---|---|:---:|---|---|
| `amount` | BigDecimal | Có | `@NotNull`, `@DecimalMin("10000.00")`, `@DecimalMax("50000000.00")` | Số tiền nạp: 10.000 đến 50.000.000 VND |
| `description` | String | Không | Tối đa 255 ký tự | Ghi chú giao dịch nạp |

* **Request Body mẫu:**
```json
{
  "amount": 200000.00,
  "description": "Nạp tiền ngân hàng mô phỏng"
}
```

* **Response thành công (HTTP `201 Created`):**
```json
{
  "code": 1000,
  "message": "Nạp tiền vào ví thành công",
  "result": {
    "transactionId": 101,
    "transactionCode": "DEP-7f3b8a1c-9e2d-4c5b",
    "type": "DEPOSIT",
    "amount": 200000.00,
    "currentBalance": 700000.00,
    "status": "SUCCESS",
    "createdAt": "2026-10-07T22:30:00"
  }
}
```

* **Response lỗi thường gặp:**
  - `400 Bad Request` (`code: 1001`): Nạp dưới 10.000 VND hoặc vượt quá 50.000.000 VND.

---

### 3.5. `POST /transfers` — Chuyển tiền ngang hàng (P2P)
* **Chức năng:** Chuyển tiền sang ví người dùng khác trong hệ thống qua Số điện thoại hoặc Username.
* **Xác thực:** **Yêu cầu JWT Token**.
* **Headers:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`

#### Tham số Request Body:
| Trường | Kiểu | Bắt buộc | Ràng buộc Validation | Ý nghĩa |
|---|---|:---:|---|---|
| `recipientIdentifier` | String | Có | `@NotBlank` | Số điện thoại hoặc Username của người nhận |
| `amount` | BigDecimal | Có | `@NotNull`, `@DecimalMin("1000.00")`, `@DecimalMax("100000000.00")` | Số tiền chuyển: Tối thiểu 1.000 VND |
| `description` | String | Không | Tối đa 255 ký tự | Lời nhắn chuyển tiền |

* **Request Body mẫu:**
```json
{
  "recipientIdentifier": "0912345678",
  "amount": 150000.00,
  "description": "Chuyển tiền ăn tối"
}
```

* **Response thành công (HTTP `201 Created`):**
```json
{
  "code": 1000,
  "message": "Chuyển tiền thành công",
  "result": {
    "transactionId": 102,
    "transactionCode": "TRF-4e7a8b1c-3d2e-11ec",
    "type": "TRANSFER",
    "amount": 150000.00,
    "recipientName": "Lê Tùng Dương",
    "recipientPhone": "0912345678",
    "remainingBalance": 550000.00,
    "status": "SUCCESS",
    "createdAt": "2026-10-07T22:35:00"
  }
}
```

* **Response lỗi thường gặp:**
  - `404 Not Found` (`code: 1004`): Người nhận không tồn tại trong hệ thống.
  - `422 Unprocessable Entity` (`code: 2001`): Số dư người gửi không đủ.
  - `422 Unprocessable Entity` (`code: 2002`): Người gửi tự chuyển tiền cho chính mình.
  - `400 Bad Request` (`code: 1001`): Ví người nhận bị khóa hoặc đóng.

* **cURL mẫu:**
```bash
curl -X POST http://localhost:8080/transfers \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientIdentifier": "0912345678",
    "amount": 150000.00,
    "description": "Chuyển tiền ăn tối"
  }'
```

---

### 3.6. `GET /transactions` — Lịch sử giao dịch (Phân trang & Lọc)
* **Chức năng:** Lấy danh sách giao dịch mà người dùng tham gia (là người gửi hoặc người nhận), có hỗ trợ phân trang và lọc theo loại giao dịch.
* **Xác thực:** **Yêu cầu JWT Token**.
* **Headers:** `Authorization: Bearer <accessToken>`

#### Tham số Query Parameters:
| Tên tham số | Kiểu | Bắt buộc | Mặc định | Ý nghĩa |
|---|---|:---:|:---:|---|
| `page` | Integer | Không | `0` | Chỉ mục trang (0-indexed) |
| `size` | Integer | Không | `10` | Số phần tử mỗi trang (tối đa 50) |
| `type` | String | Không | `ALL` | Bộ lọc loại giao dịch: `ALL`, `DEPOSIT`, `TRANSFER` |

* **Response thành công (HTTP `200 OK`):**
```json
{
  "code": 1000,
  "message": "Lấy lịch sử giao dịch thành công",
  "result": {
    "content": [
      {
        "id": 102,
        "transactionCode": "TRF-4e7a8b1c-3d2e-11ec",
        "type": "TRANSFER",
        "direction": "OUT",
        "amount": 150000.00,
        "counterpart": "Lê Tùng Dương (0912345678)",
        "description": "Chuyển tiền ăn tối",
        "status": "SUCCESS",
        "createdAt": "2026-10-07T22:35:00"
      },
      {
        "id": 101,
        "transactionCode": "DEP-7f3b8a1c-9e2d-4c5b",
        "type": "DEPOSIT",
        "direction": "IN",
        "amount": 200000.00,
        "counterpart": "Hệ thống nạp",
        "description": "Nạp tiền ngân hàng mô phỏng",
        "status": "SUCCESS",
        "createdAt": "2026-10-07T22:30:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

* **Quy ước trường `direction`:**
  - `IN`: Tiền vào ví (Cộng số dư).
  - `OUT`: Tiền ra khỏi ví (Trừ số dư).

---

### 3.7. `GET /transactions/{id}` — Chi tiết một giao dịch
* **Chức năng:** Xem thông tin chi tiết của một giao dịch cụ thể.
* **Xác thực:** **Yêu cầu JWT Token**.
* **Path Variable:** `{id}` (Long) — Mã định danh của bản ghi giao dịch.
* **Bảo mật:** Hệ thống kiểm tra quyền sở hữu. Nếu người gửi và người nhận đều không phải là người dùng hiện tại $\rightarrow$ Trả về `403 Forbidden` (chống lỗi **IDOR**).

* **Response thành công (HTTP `200 OK`):**
```json
{
  "code": 1000,
  "message": "Lấy chi tiết giao dịch thành công",
  "result": {
    "id": 102,
    "transactionCode": "TRF-4e7a8b1c-3d2e-11ec",
    "type": "TRANSFER",
    "direction": "OUT",
    "amount": 150000.00,
    "senderName": "Đinh Quang Tuân",
    "receiverName": "Lê Tùng Dương",
    "status": "SUCCESS",
    "description": "Chuyển tiền ăn tối",
    "createdAt": "2026-10-07T22:35:00"
  }
}
```

* **Response lỗi thường gặp:**
  - `404 Not Found` (`code: 1004`): Không tồn tại giao dịch với ID tương ứng.
  - `403 Forbidden` (`code: 1003`): Giao dịch này thuộc về 2 người dùng khác.

---

### 3.8. `DELETE /accounts/me` — Đóng tài khoản người dùng
* **Chức năng:** Đóng vĩnh viễn ví và tài khoản của người dùng hiện tại.
* **Xác thực:** **Yêu cầu JWT Token**.
* **Tiền điều kiện bắt buộc:** Số dư ví hiện tại **phải chính xác bằng 0.00 VND**.
* **Cơ chế:** Soft Delete (chuyển trạng thái `User` và `Wallet` thành `CLOSED`), vô hiệu hóa khả năng đăng nhập/chuyển tiền trong tương lai, giữ nguyên lịch sử giao dịch.

* **Response thành công (HTTP `204 No Content`):**
  - Không có nội dung Response Body.

* **Response lỗi thường gặp:**
  - `400 Bad Request` (`code: 1001`): Số dư ví vẫn còn tiền ($balance > 0$).
  ```json
  {
    "code": 1001,
    "message": "Không thể đóng tài khoản do số dư ví vẫn còn 500000.00 VND. Vui lòng chuyển hết tiền trước khi đóng tài khoản.",
    "result": null
  }
  ```

---

## 4. TÍCH HỢP SWAGGER UI & OPENAPI 3

Khi triển khai Springdoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`), các endpoint trên sẽ được render trực quan tại các địa chỉ:

| Tài nguyên | Đường dẫn URL | Mô tả |
|---|---|---|
| **Swagger UI HTML** | `http://localhost:8080/swagger-ui.html` | Giao diện test API tương tác trực tiếp |
| **OpenAPI 3 Docs JSON** | `http://localhost:8080/v3/api-docs` | Schema JSON chuẩn hóa xuất ra cho Postman / Locust |

### Cấu hình OpenAPI Security Scheme trong mã nguồn:
Mọi endpoint (trừ `/auth/**`) cần được gắn tag bảo mật `Bearer Authentication`:
```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```
