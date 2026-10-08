package uet.com.eWallet.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import uet.com.eWallet.api.dto.request.RegisterRequest;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.data.entity.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-08T15:20:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UserResponse userResponse = new UserResponse();

        return userResponse;
    }

    @Override
    public User toUser(RegisterRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        return user.build();
    }
}
