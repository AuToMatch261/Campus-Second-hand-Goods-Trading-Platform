package com.campus.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页（1 起）")
    private long page;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "总条数")
    private long total;

    @Schema(description = "记录列表")
    private List<T> records;

    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        return new PageResult<>(page, size, total, records);
    }
}
