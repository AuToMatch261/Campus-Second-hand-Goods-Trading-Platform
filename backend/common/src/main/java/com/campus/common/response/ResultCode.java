package com.campus.common.response;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    SERVER_ERROR(500, "服务器内部错误"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "账号或密码错误"),
    USER_ALREADY_EXISTS(10003, "用户已存在"),

    PRODUCT_NOT_FOUND(20001, "商品不存在"),
    PRODUCT_OFF_SHELF(20002, "商品已下架"),
    PRODUCT_SOLD(20003, "商品已售出"),
    PRODUCT_OWNER_MISMATCH(20004, "无权操作他人商品"),
    PRODUCT_STATUS_ILLEGAL(20005, "商品当前状态不允许此操作"),

    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_STATUS_ILLEGAL(30002, "订单状态非法"),
    ORDER_BUY_OWN_PRODUCT(30003, "不能购买自己发布的商品"),
    ORDER_ACCESS_DENIED(30004, "无权访问该订单"),
    ORDER_PRODUCT_PRICE_CHANGED(30005, "商品价格已变动，请刷新后重试"),

    MESSAGE_TO_SELF(40001, "不能给自己发消息"),
    MESSAGE_ACCESS_DENIED(40002, "无权访问该会话"),
    MESSAGE_CONTENT_EMPTY(40003, "消息内容不能为空");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
