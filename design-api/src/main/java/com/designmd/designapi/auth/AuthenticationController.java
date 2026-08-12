package com.designmd.designapi.auth;

import com.designmd.designapi.auth.dto.request.LogoutRequest;
import com.designmd.designapi.common.response.ApiResponse;
import com.designmd.designapi.auth.dto.request.AuthenticationResquest;
import com.designmd.designapi.auth.dto.request.IntrospectRequest;
import com.designmd.designapi.auth.dto.response.AuthenticationResponse;
import com.designmd.designapi.auth.dto.response.IntrospectResponse;
import com.designmd.designapi.auth.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/token")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationResquest resquest) {
        var res = authenticationService.authenticate(resquest);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(res)
                .build();
    }
    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest resquest) throws ParseException, JOSEException {
        var res = authenticationService.introspect(resquest);
        return ApiResponse.<IntrospectResponse>builder()
                .result(res)
                .build();
    }
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest resquest) throws ParseException, JOSEException {
        authenticationService.logout(resquest);
        return ApiResponse.<Void>builder()
                .build();
    }
}

