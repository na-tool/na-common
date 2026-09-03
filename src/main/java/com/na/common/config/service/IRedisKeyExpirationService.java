package com.na.common.config.service;

public interface IRedisKeyExpirationService {
    void run(String message, byte[] pattern) throws Exception;
}
