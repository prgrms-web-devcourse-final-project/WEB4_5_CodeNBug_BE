package org.codeNbug.mainserver.global.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

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

    /**
     * 페이지 메타데이터 (페이징 처리된 응답에만 사용)
     * null일 경우 JSON 응답에서 제외됨
     */
    private PageMetadata page;


    public RsData(String code, String msg) {
        this(code, msg, null, null);
    }

    /**
     * 데이터 포함 생성자 (페이징 없음)
     */
    public RsData(String code, String msg, T data) {
        this(code, msg, data, null);
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


    /**
     * 페이징 정보가 포함된 응답 생성
     * Spring Data의 Page 객체로부터 데이터와 페이징 정보를 추출하여 응답 생성
     *
     * @param code 응답 코드 (예: "200-SUCCESS")
     * @param msg 응답 메시지
     * @param page 페이징 처리된 데이터
     * @return 페이징 정보가 포함된 RsData 객체
     */
    public static <R> RsData<R> withPage(String code, String msg, Page<R> page) {
        return new RsData<>(
                code,
                msg,
                (R) page.getContent(),  // 페이지 내용을 데이터로 설정
                PageMetadata.from(page) // 페이징 메타데이터 생성
        );
    }

    /**
     * 성공 응답 생성 - 페이징 포함 (코드: "200-SUCCESS")
     *
     * @param msg 성공 메시지
     * @param page 페이징 처리된 데이터
     * @return 성공 응답 객체
     */
    public static <R> RsData<R> successWithPage(String msg, Page<R> page) {
        return withPage("200-SUCCESS", msg, page);
    }

    /**
     * 일반적인 성공 응답 생성 (코드: "200-SUCCESS")
     *
     * @param msg 성공 메시지
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <R> RsData<R> success(String msg, R data) {
        return new RsData<>("200-SUCCESS", msg, data);
    }

    /**
     * 데이터 없는 성공 응답 생성 (코드: "200-SUCCESS")
     *
     * @param msg 성공 메시지
     * @return 성공 응답 객체
     */
    public static <R> RsData<R> success(String msg) {
        return new RsData<>("200-SUCCESS", msg);
    }

    /**
     * 오류 응답 생성
     *
     * @param code 오류 코드 (예: "400-BAD_REQUEST")
     * @param msg 오류 메시지
     * @return 오류 응답 객체
     */
    public static <R> RsData<R> error(String code, String msg) {
        return new RsData<>(code, msg);
    }

}