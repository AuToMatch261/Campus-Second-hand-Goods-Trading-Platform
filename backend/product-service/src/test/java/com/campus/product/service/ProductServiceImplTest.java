package com.campus.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.exception.BusinessException;
import com.campus.common.mq.event.ProductEvent;
import com.campus.common.response.PageResult;
import com.campus.common.response.ResultCode;
import com.campus.product.dto.ProductListQuery;
import com.campus.product.dto.ProductVO;
import com.campus.product.dto.PublishProductRequest;
import com.campus.product.dto.UpdateProductRequest;
import com.campus.product.entity.Product;
import com.campus.product.enums.ProductCategory;
import com.campus.product.enums.ProductStatus;
import com.campus.product.mapper.ProductMapper;
import com.campus.product.search.ProductSearchService;
import com.campus.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    ProductMapper mapper;

    @Mock
    ApplicationEventPublisher events;

    @Mock
    ProductSearchService searchService;

    ProductServiceImpl service;

    @BeforeEach
    void setup() {
        service = new ProductServiceImpl(mapper, events, searchService);
    }

    private Product onShelf(long id, long sellerId) {
        Product p = new Product();
        p.setId(id);
        p.setSellerId(sellerId);
        p.setTitle("书");
        p.setPrice(new BigDecimal("10.00"));
        p.setCategory(ProductCategory.BOOKS);
        p.setStatus(ProductStatus.ON_SHELF);
        p.setViewCount(0);
        return p;
    }

    @Test
    void publish_sets_seller_and_on_shelf_and_publishes_event() {
        when(mapper.insert(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return 1;
        });

        PublishProductRequest req = new PublishProductRequest();
        req.setTitle("数据库系统概念");
        req.setPrice(new BigDecimal("29.90"));
        req.setCategory(ProductCategory.BOOKS);
        req.setImages(List.of("u1"));

        ProductVO vo = service.publish(7L, req);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(mapper).insert(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getSellerId()).isEqualTo(7L);
        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ON_SHELF);
        assertThat(saved.getViewCount()).isEqualTo(0);
        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getCategoryLabel()).isEqualTo("书籍");

        ArgumentCaptor<ProductEvent> ec = ArgumentCaptor.forClass(ProductEvent.class);
        verify(events).publishEvent(ec.capture());
        assertThat(ec.getValue().getType()).isEqualTo(ProductEvent.Type.UPSERTED);
        assertThat(ec.getValue().getProductId()).isEqualTo(100L);
    }

    @Test
    void update_only_non_null_fields() {
        Product existing = onShelf(1L, 7L);
        when(mapper.selectById(1L)).thenReturn(existing, existing);

        UpdateProductRequest req = new UpdateProductRequest();
        req.setTitle("新标题");
        req.setPrice(new BigDecimal("99.00"));

        service.update(7L, 1L, req);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(mapper).updateById(captor.capture());
        Product patch = captor.getValue();
        assertThat(patch.getId()).isEqualTo(1L);
        assertThat(patch.getTitle()).isEqualTo("新标题");
        assertThat(patch.getPrice()).isEqualByComparingTo("99.00");
        assertThat(patch.getDescription()).isNull();
        assertThat(patch.getCategory()).isNull();
    }

    @Test
    void update_rejects_non_owner() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        assertThatThrownBy(() -> service.update(99L, 1L, new UpdateProductRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_OWNER_MISMATCH.getCode());
        verify(mapper, never()).updateById(any(Product.class));
    }

    @Test
    void update_rejects_sold_product() {
        Product sold = onShelf(1L, 7L);
        sold.setStatus(ProductStatus.SOLD);
        when(mapper.selectById(1L)).thenReturn(sold);

        assertThatThrownBy(() -> service.update(7L, 1L, new UpdateProductRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_STATUS_ILLEGAL.getCode());
    }

    @Test
    void offShelf_only_on_shelf_allowed() {
        Product offShelf = onShelf(1L, 7L);
        offShelf.setStatus(ProductStatus.OFF_SHELF);
        when(mapper.selectById(1L)).thenReturn(offShelf);

        assertThatThrownBy(() -> service.offShelf(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_STATUS_ILLEGAL.getCode());
    }

    @Test
    void offShelf_owner_success() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        service.offShelf(7L, 1L);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(mapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.OFF_SHELF);
    }

    @Test
    void softDelete_only_owner() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        assertThatThrownBy(() -> service.softDelete(99L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).deleteById(any(Long.class));
    }

    @Test
    void softDelete_owner_success_publishes_deleted_event() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        service.softDelete(7L, 1L);
        verify(mapper).deleteById(1L);

        ArgumentCaptor<ProductEvent> ec = ArgumentCaptor.forClass(ProductEvent.class);
        verify(events).publishEvent(ec.capture());
        assertThat(ec.getValue().getType()).isEqualTo(ProductEvent.Type.DELETED);
        assertThat(ec.getValue().getProductId()).isEqualTo(1L);
    }

    @Test
    void detail_on_shelf_increments_view() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        when(mapper.incrementViewCount(1L)).thenReturn(1);

        ProductVO vo = service.detail(1L, true);

        verify(mapper, times(1)).incrementViewCount(1L);
        assertThat(vo.getViewCount()).isEqualTo(1);
    }

    @Test
    void detail_off_shelf_no_view_bump() {
        Product off = onShelf(1L, 7L);
        off.setStatus(ProductStatus.OFF_SHELF);
        when(mapper.selectById(1L)).thenReturn(off);

        service.detail(1L, true);
        verify(mapper, never()).incrementViewCount(1L);
    }

    @Test
    void detail_not_found() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void list_delegates_to_search_service() {
        ProductListQuery q = new ProductListQuery();
        ProductVO vo = ProductVO.builder().id(1L).title("书").status(ProductStatus.ON_SHELF).build();
        when(searchService.search(q)).thenReturn(PageResult.of(1L, 20L, 1L, List.of(vo)));

        PageResult<ProductVO> r = service.list(q);
        assertThat(r.getTotal()).isEqualTo(1);
        assertThat(r.getRecords()).hasSize(1);
        verify(searchService).search(q);
        verify(mapper, never()).selectPage(any(), any());
    }

    @Test
    void listMine_uses_seller_filter() {
        Page<Product> page = new Page<>(1, 20);
        page.setRecords(List.of(onShelf(1L, 7L)));
        page.setTotal(1);
        when(mapper.selectPage(any(), any())).thenReturn(page);

        PageResult<ProductVO> r = service.listMine(7L, 1, 20);
        assertThat(r.getRecords()).hasSize(1);
        assertThat(r.getRecords().get(0).getSellerId()).isEqualTo(7L);
    }

    @Test
    void markSold_success_on_shelf() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));
        when(mapper.updateStatusConditional(1L,
                ProductStatus.ON_SHELF.getCode(), ProductStatus.SOLD.getCode())).thenReturn(1);
        service.markSold(1L);
        verify(mapper).updateStatusConditional(1L,
                ProductStatus.ON_SHELF.getCode(), ProductStatus.SOLD.getCode());
    }

    @Test
    void markSold_already_sold() {
        Product sold = onShelf(1L, 7L);
        sold.setStatus(ProductStatus.SOLD);
        when(mapper.selectById(1L)).thenReturn(sold);
        when(mapper.updateStatusConditional(1L,
                ProductStatus.ON_SHELF.getCode(), ProductStatus.SOLD.getCode())).thenReturn(0);

        assertThatThrownBy(() -> service.markSold(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_SOLD.getCode());
    }

    @Test
    void markSold_off_shelf() {
        Product off = onShelf(1L, 7L);
        off.setStatus(ProductStatus.OFF_SHELF);
        when(mapper.selectById(1L)).thenReturn(off);
        when(mapper.updateStatusConditional(1L,
                ProductStatus.ON_SHELF.getCode(), ProductStatus.SOLD.getCode())).thenReturn(0);

        assertThatThrownBy(() -> service.markSold(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PRODUCT_OFF_SHELF.getCode());
    }

    @Test
    void tryRelist_success_when_sold() {
        Product sold = onShelf(1L, 7L);
        sold.setStatus(ProductStatus.SOLD);
        when(mapper.selectById(1L)).thenReturn(sold);
        when(mapper.updateStatusConditional(1L,
                ProductStatus.SOLD.getCode(), ProductStatus.ON_SHELF.getCode())).thenReturn(1);

        assertThat(service.tryRelist(1L)).isTrue();
        verify(mapper).updateStatusConditional(1L,
                ProductStatus.SOLD.getCode(), ProductStatus.ON_SHELF.getCode());
    }

    @Test
    void tryRelist_idempotent_when_already_on_shelf() {
        when(mapper.selectById(1L)).thenReturn(onShelf(1L, 7L));

        assertThat(service.tryRelist(1L)).isTrue();
        verify(mapper, never()).updateStatusConditional(anyLong(), anyInt(), anyInt());
    }

    @Test
    void tryRelist_returns_false_when_product_missing() {
        when(mapper.selectById(1L)).thenReturn(null);
        assertThat(service.tryRelist(1L)).isFalse();
    }

    @Test
    void tryRelist_returns_false_when_status_unknown() {
        Product off = onShelf(1L, 7L);
        off.setStatus(ProductStatus.OFF_SHELF);
        when(mapper.selectById(1L)).thenReturn(off);

        assertThat(service.tryRelist(1L)).isFalse();
        verify(mapper, never()).updateStatusConditional(anyLong(), anyInt(), anyInt());
    }
}
