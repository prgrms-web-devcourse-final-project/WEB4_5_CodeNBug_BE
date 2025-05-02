package org.codeNbug.mainserver.global.dto;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;

/**
 * 페이징 처리된 응답에 포함될 메타데이터
 */
@Getter
@Builder
public class PageMetadata {
	private int page;            // 현재 페이지 번호 (0-based)
	private int size;            // 페이지 크기
	private long totalElements;  // 전체 요소 수
	private int totalPages;      // 전체 페이지 수
	private boolean first;       // 첫 페이지 여부
	private boolean last;        // 마지막 페이지 여부

	/**
	 * Spring Data의 Page 객체로부터 PageMetadata 생성
	 */
	public static <T> PageMetadata from(Page<T> page) {
		return PageMetadata.builder()
			.page(page.getNumber())
			.size(page.getSize())
			.totalElements(page.getTotalElements())
			.totalPages(page.getTotalPages())
			.first(page.isFirst())
			.last(page.isLast())
			.build();
	}
}