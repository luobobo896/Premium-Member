package com.premium.member.common.api;

import java.util.List;

/**
 * vben 分页返回结构：items + total。
 *
 * @param items 当前页数据
 * @param total 总记录数
 * @param <T>   数据类型
 */
public record PageResult<T>(List<T> items, long total) {

    public static <T> PageResult<T> of(List<T> items, long total) {
        return new PageResult<>(items, total);
    }
}
