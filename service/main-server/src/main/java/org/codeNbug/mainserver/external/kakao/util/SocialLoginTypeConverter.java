package org.codeNbug.mainserver.external.kakao.util;

import org.codeNbug.mainserver.external.kakao.constant.SocialLoginType;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class SocialLoginTypeConverter implements Converter<String, SocialLoginType> { //대문자 값을 소문자로 mapping
    @Override
    public SocialLoginType convert(String s) {
        return SocialLoginType.valueOf(s.toUpperCase());
    }
}