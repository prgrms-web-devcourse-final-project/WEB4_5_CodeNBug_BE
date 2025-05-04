package org.codeNbug.mainserver.global.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.codeNbug.mainserver", "org.codenbug.security", "org.codenbug.common"})
public class AppConfig {
}
