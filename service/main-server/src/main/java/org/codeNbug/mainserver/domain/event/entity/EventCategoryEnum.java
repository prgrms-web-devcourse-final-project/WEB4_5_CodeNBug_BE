package org.codeNbug.mainserver.domain.event.entity;

import java.util.Arrays;

public enum EventCategoryEnum {
    CONCERT("콘서트"),
    MUSICAL("뮤지컬"),
    EXHIBITION("전시"),
    SPORTS("스포츠"),
    FAN_MEETING("팬미팅"),
    ETC("기타");

    private final String displayName;

    EventCategoryEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EventCategoryEnum fromDisplayName(String displayName) {
        return Arrays.stream(values())
                .filter(e -> e.displayName.equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이벤트 카테고리: " + displayName));
    }
}
