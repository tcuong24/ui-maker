package com.designmd.designapi.user;

import com.designmd.designapi.user.dto.request.UserCreateRequest;
import com.designmd.designapi.user.dto.request.UserUpdateRequest;
import com.designmd.designapi.user.dto.response.UserResponse;
import com.designmd.designapi.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")

public interface UserMapper {
    User toUser(UserCreateRequest request);
    UserResponse toUserResponse(User user);
    List<UserResponse> toUserResponse(List<User> users);
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}


