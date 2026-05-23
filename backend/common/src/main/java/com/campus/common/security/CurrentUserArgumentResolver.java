package com.campus.common.security;

import com.campus.common.constant.CommonConstants;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;



public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) return false;
        Class<?> type = parameter.getParameterType();
        return type == Long.class || type == long.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String header = request == null ? null : request.getHeader(CommonConstants.HEADER_USER_ID);

        CurrentUser ann = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = ann == null || ann.required();

        if (header == null || header.isBlank()) {
            if (required) throw new BusinessException(ResultCode.UNAUTHORIZED);
            return parameter.getParameterType() == long.class ? 0L : null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "X-User-Id 非法");
        }
    }
}
