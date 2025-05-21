package org.codeNbug.mainserver.global.util;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.*;
public class UtcToKstLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText(); // "2025-05-21T05:21:11.579Z"
        Instant instant = Instant.parse(value); // UTC 기준으로 Instant 생성
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul")); // KST로 변환
    }
}
