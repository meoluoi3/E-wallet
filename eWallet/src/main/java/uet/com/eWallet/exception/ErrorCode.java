package uet.com.eWallet.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    USER_EXISTED(1001, "Tên đăng nhập đã tồn tại", HttpStatus.BAD_REQUEST),

    PHONE_EXISTED(1002, "Số điện thoại đã tồn tại", HttpStatus.BAD_REQUEST),

    EMAIL_EXISTED(1003, "Email đã tồn tại", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND(1004, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    INVALID_CREDENTIALS(1005, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),

    ACCOUNT_CLOSED(1006, "Tài khoản đã bị đóng", HttpStatus.FORBIDDEN),

    UNAUTHENTICATED(1007, "Chưa xác thực hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),

    INVALID_INPUT(1008, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST);

    int code;

    String message;

    HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
