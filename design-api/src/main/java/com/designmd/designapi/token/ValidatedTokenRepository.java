package com.designmd.designapi.token;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ValidatedTokenRepository extends MongoRepository<ValidatedToken, String> {
}

