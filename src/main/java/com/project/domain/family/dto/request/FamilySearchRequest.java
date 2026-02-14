package com.project.domain.family.dto.request;

import java.util.List;

public record FamilySearchRequest(
        Integer page, Integer size, Filters filters, List<SortCondition> sort) {
    public record Filters(StringCondition name, StringCondition phone, RangeCondition usageRate) {}

    public record StringCondition(String operator, String value) {}

    public record RangeCondition(String operator, Double min, Double max) {}

    public record SortCondition(String field, String direction) {}

    public int getPage() {
        return (page != null) ? page : 0;
    }

    public int getSize() {
        return (size != null) ? Math.min(size, 100) : 20;
    }
}
