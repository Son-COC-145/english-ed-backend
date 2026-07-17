package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private int currentPage;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private List<T> content;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .content(page.getContent())
                .build();
    }
}
