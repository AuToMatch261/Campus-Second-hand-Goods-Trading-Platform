package com.campus.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注 Controller 方法参数，从 X-User-Id 头自动注入当前登录用户 ID。
 * 仅在网关已校验 Token 并下发 X-User-Id 头时有意义。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /** 未携带 X-User-Id 头时是否抛出 401，默认必填 */
    boolean required() default true;
}
