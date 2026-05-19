package com.campus.common.constant;

public final class CommonConstants {

    private CommonConstants() {}

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_AUTH = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String REDIS_TOKEN_PREFIX = "auth:token:";
    public static final String REDIS_USER_PREFIX = "user:info:";
}
