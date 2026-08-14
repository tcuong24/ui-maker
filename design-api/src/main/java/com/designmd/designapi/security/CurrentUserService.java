package com.designmd.designapi.security;

import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    public String getUserId() {
//        String userId = SecurityContextHolder
//                .getContext()
//                .getAuthentication()
//                .getName();
//
//        if (!userRepository.existsById(userId)) {
//            throw new AppException(
//                    ErrorCode.USER_NOT_EXISTED
//            );
//        }
//
//        return userId;
        return "public";
    }

    public Optional<String> getOptionalUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }
}
