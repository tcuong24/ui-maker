package com.designmd.designapi.security;

import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public String getUserId() {
        String userId = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        if (!userRepository.existsById(userId)) {
            throw new AppException(
                    ErrorCode.USER_NOT_EXISTED
            );
        }

        return userId;
    }
}
