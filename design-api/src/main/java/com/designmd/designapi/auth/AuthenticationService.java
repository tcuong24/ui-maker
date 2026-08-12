package com.designmd.designapi.auth;

import com.designmd.designapi.auth.dto.request.AuthenticationResquest;
import com.designmd.designapi.auth.dto.request.IntrospectRequest;
import com.designmd.designapi.auth.dto.request.LogoutRequest;
import com.designmd.designapi.auth.dto.response.AuthenticationResponse;
import com.designmd.designapi.auth.dto.response.IntrospectResponse;
import com.designmd.designapi.user.User;
import com.designmd.designapi.token.ValidatedToken;
import com.designmd.designapi.common.exception.AppException;
import com.designmd.designapi.common.exception.ErrorCode;
import com.designmd.designapi.user.UserRepository;
import com.designmd.designapi.token.ValidatedTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    ValidatedTokenRepository validatedTokenRepository;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY ;
    public IntrospectResponse introspect (IntrospectRequest request) throws JOSEException, ParseException {
        var token =request.getToken();
        var jwtToken = verifyToken(token);

        return IntrospectResponse.builder()
                .valid(true)
                .build();
    }
    public AuthenticationResponse authenticate(AuthenticationResquest resquest){
        var user = userRepository.findByUsername(resquest.getUsername())
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        boolean authen = passwordEncoder.matches(resquest.getPassword(),user.getPassword());
        if (!authen){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }
    private String generateToken(User user){
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getId())
                .issuer("design-api")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .claim("scope",buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader,payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token",e);
            throw new RuntimeException(e);
        }
    }
    public  void logout(LogoutRequest request) throws ParseException, JOSEException {
        var signToken = verifyToken(request.getToken());

        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
        ValidatedToken validatedToken = ValidatedToken.builder()
                .id(jit)
                .expiryDate(expiryTime)
                .build();
        validatedTokenRepository.save(validatedToken);
    }
    private SignedJWT verifyToken(String token)
            throws JOSEException, ParseException {

        JWSVerifier verifier =
                new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime =
                signedJWT.getJWTClaimsSet().getExpirationTime();

        String jwtId =
                signedJWT.getJWTClaimsSet().getJWTID();

        boolean verified = signedJWT.verify(verifier);
        boolean expired =
                expiryTime == null ||
                        expiryTime.before(new Date());

        boolean revoked =
                jwtId != null &&
                        validatedTokenRepository.existsById(jwtId);

        if (!verified || expired || revoked) {
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED
            );
        }

        return signedJWT;
    }
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRole())) {
            user.getRole().forEach(stringJoiner::add);
        }

        return stringJoiner.toString();
    }
}

