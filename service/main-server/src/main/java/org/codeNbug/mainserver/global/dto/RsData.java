package org.codeNbug.mainserver.global.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsData<T> {
    /**
     * 응답 상태 코드 (예: "200-SUCCESS", "400-BAD_REQUEST")
     * HTTP 상태 코드와 상세 코드를 포함
     */
    private String code;

    /**
     * 응답 메시지
     * 사용자에게 표시할 메시지 또는 오류 설명
     */
    private String msg;

    /**
     * 응답 데이터
     * API 응답에 포함될 실제 데이터 객체
     */
    private T data;

    public RsData(String code, String msg) {
        this(code, msg, null);
    }

    /**
     * HTTP 상태 코드 추출
     * code 문자열에서 첫 번째 부분을 추출하여 HTTP 상태 코드로 변환
     *
     * @return HTTP 상태 코드 (예: 200, 400, 500)
     */
    @JsonIgnore
    public int getStatusCode() {
        String statusCodeStr = code.split("-")[0];
        return Integer.parseInt(statusCodeStr);
    }

}