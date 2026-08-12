package com.designmd.designapi.user;

import com.designmd.designapi.common.response.ApiResponse;
import com.designmd.designapi.user.dto.request.UserCreateRequest;
import com.designmd.designapi.user.dto.request.UserUpdateRequest;
import com.designmd.designapi.user.dto.response.UserResponse;
import com.designmd.designapi.user.User;
import com.designmd.designapi.user.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserController {
    @Autowired
    UserService userService;

    @PostMapping
    ApiResponse<User> createUser(@RequestBody @Valid UserCreateRequest request) {
        ApiResponse<User> apiResponse= new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        apiResponse.setCode(1000);
        return apiResponse;
    }
    @PreAuthorize("hasRole('Admin')")
    @GetMapping
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers()).build();
    }
    @PostAuthorize("returnObject.username == authentication.name")
    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }
    @GetMapping("/myInfo")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/{userId}")
    UserResponse updateUser(@PathVariable String userId , @RequestBody UserUpdateRequest request) {
        return userService.updateUser(userId,request);
    }
    @DeleteMapping("/{userId}")
    String deleteUser (@PathVariable String userId ) {
        userService.deleteUser(userId);
        return "User has been deleted";
    }
}


