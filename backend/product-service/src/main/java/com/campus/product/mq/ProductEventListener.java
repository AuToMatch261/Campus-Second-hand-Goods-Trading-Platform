package com.campus.product.mq;

import com.campus.common.mq.RabbitMqConstants;
import com.campus.common.mq.event.ProductEvent;
import com.campus.product.entity.Product;
import com.campus.product.mapper.ProductMapper;
import com.campus.product.search.ProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 自产自消：监听本服务发出的 product.# 事件，按当前 DB 状态把对应 doc 写入 ES。
 * 删除事件直接 delete by id。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductMapper productMapper;
    private final ProductIndexService indexService;

    @RabbitListener(queues = RabbitMqConstants.QUEUE_SEARCH_PRODUCT_EVENTS)
    public void onProductEvent(ProductEvent event) {
        if (event == null || event.getProductId() == null || event.getType() == null) {
            log.warn("非法商品事件，丢弃: {}", event);
            return;
        }
        switch (event.getType()) {
            case UPSERTED -> {
                Product p = productMapper.selectById(event.getProductId());
                if (p == null) {
                    // 极少见：事件投递时商品已被删除。当成 delete 处理
                    log.info("UPSERTED 事件但商品已不存在，改为删除 ES doc: productId={}",
                            event.getProductId());
                    indexService.delete(event.getProductId());
                    return;
                }
                indexService.upsert(p);
            }
            case DELETED -> indexService.delete(event.getProductId());
        }
    }
}
