package com.designmd.designapi.token;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
@Builder
@Getter
@Setter
@Document(collection = "validated_tokens")
public class ValidatedToken {
    @Id
    String id;
    @Indexed(expireAfter = "0s")
    Date expiryDate;
}

