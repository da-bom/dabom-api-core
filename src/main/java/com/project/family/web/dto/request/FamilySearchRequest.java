package com.project.family.web.dto.request;

import java.util.List;

/** 가족 검색 요청 DTO */
public record FamilySearchRequest(
        Integer page, Integer size, Filters filters, List<SortCondition> sort) {
    public record Filters(StringCondition name, StringCondition phone, RangeCondition usageRate) {}

    public record StringCondition(
            String operator, // "contains"
            String value) {}

    public record RangeCondition(
            String operator, // "between"
            Double min,
            Double max) {}

    public record SortCondition(String field, String direction) {}

    public int getPage() {
        return (page != null) ? page : 0;
    }

    public int getSize() {
        return (size != null) ? size : 20;
    }
}
