package com.campus.common.mq;

public final class RabbitMqConstants {

    public static final String ORDER_EVENTS_EXCHANGE = "campus.order.events";

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_CONFIRMED = "order.confirmed";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";

    public static final String RK_ORDER_ALL = "order.#";

    public static final String QUEUE_PRODUCT_ORDER_CANCELLED = "product.order.cancelled";
    public static final String QUEUE_MESSAGE_ORDER_EVENTS = "message.order.events";

    public static final String PRODUCT_EVENTS_EXCHANGE = "campus.product.events";

    public static final String RK_PRODUCT_UPSERTED = "product.upserted";
    public static final String RK_PRODUCT_DELETED = "product.deleted";
    public static final String RK_PRODUCT_ALL = "product.#";

    public static final String QUEUE_SEARCH_PRODUCT_EVENTS = "search.product.events";

    public static final String DLX_EXCHANGE = "campus.dlx";
    public static final String DLQ_PRODUCT_ORDER_CANCELLED = "product.order.cancelled.dlq";
    public static final String DLQ_MESSAGE_ORDER_EVENTS = "message.order.events.dlq";
    public static final String DLQ_SEARCH_PRODUCT_EVENTS = "search.product.events.dlq";

    private RabbitMqConstants() {}
}
