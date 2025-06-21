package com.na.common.cache;

public interface INaRedisKeyExpirationService {
    void run(String message, byte[] pattern) throws Exception;
}
