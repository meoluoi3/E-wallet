package uet.com.eWallet.mapper;

import org.mapstruct.Mapper;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.data.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}
