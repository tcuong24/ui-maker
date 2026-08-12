package com.designmd.designapi.security;

import com.designmd.designapi.user.User;
import com.designmd.designapi.user.Role;
import com.designmd.designapi.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Slf4j
@Configuration
public class ApplicationConfigClass {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    @ConditionalOnProperty(
            name = "app.bootstrap-admin.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    ApplicationRunner applicationRunner(UserRepository userRepository){
        return args ->{
            if (userRepository.findByUsername("admin").isEmpty()){
                var roles = new HashSet<String>();
                roles.add(Role.ADMIN.name());
                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .role(roles)
                        .build();
                userRepository.save(user);
                log.warn("admin user has been created with default password admin. please change it");
            }

        };
    }
}

