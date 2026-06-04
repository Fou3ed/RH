package com.maram.payroll.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * API pagination envelope: {@code { data, total, page, limit }}.
 */
public record PageResponse<T>(
        List<T> data,
        long total,
        int page,
        int limit) {

    /** Maps a Spring Data {@link Page} of entities into a DTO page. */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize());
    }
}
