package com.project.domain.family.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record FamilySearchRequest(
        @Schema(description = "페이지 번호(0부터 시작)", example = "0") Integer page,
        @Schema(description = "페이지 크기(최대 100)", example = "20") Integer size,
        @Schema(description = "검색 필터 조건") Filters filters,
        @Schema(description = "정렬 조건 목록") List<SortCondition> sort) {
    public record Filters(
            @Schema(description = "이름 검색 조건") StringCondition name,
            @Schema(description = "연락처 검색 조건") StringCondition phone,
            @Schema(description = "사용률 범위 검색 조건") RangeCondition usageRate) {}

    public record StringCondition(
            @Schema(description = "비교 연산자", example = "contains") String operator,
            @Schema(description = "비교 값", example = "홍길동 / 1234") String value) {}

    public record RangeCondition(
            @Schema(description = "범위 연산자", example = "between") String operator,
            @Schema(description = "최소값", example = "10.0") Double min,
            @Schema(description = "최대값", example = "90.0") Double max) {}

    public record SortCondition(
            @Schema(description = "정렬 대상 필드", example = "createdAt") String field,
            @Schema(description = "정렬 방향(asc/desc)", example = "desc") String direction) {}

    public int getPage() {
        return (page != null) ? page : 0;
    }

    public int getSize() {
        return (size != null) ? Math.min(size, 100) : 20;
    }
}
