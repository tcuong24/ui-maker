package com.designmd.designapi.user;


import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    String id;
    @Indexed(unique = true)
    String username;
    String password;
    String firstName;
    String lastName;
    Set<String> role;
    LocalDate dob;
}


