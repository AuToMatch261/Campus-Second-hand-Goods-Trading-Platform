package com.campus.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE t_product SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") long id);

    @Update("UPDATE t_product SET status = #{newStatus}, updated_at = NOW() "
            + "WHERE id = #{id} AND status = #{oldStatus} AND deleted = 0")
    int updateStatusConditional(@Param("id") long id,
                                @Param("oldStatus") int oldStatus,
                                @Param("newStatus") int newStatus);
}
