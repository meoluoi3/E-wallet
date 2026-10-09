package uet.com.eWallet.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uet.com.eWallet.api.dto.request.RegisterRequest;
import uet.com.eWallet.api.dto.response.UserResponse;
import uet.com.eWallet.data.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    User toUser(RegisterRequest request);
}
