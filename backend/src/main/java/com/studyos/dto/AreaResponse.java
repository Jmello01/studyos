package com.studyos.dto;

public record AreaResponse(
        Long id,
        String name,
        String description,
        String color,
        String icon,
        int position,
        boolean archived) {
}