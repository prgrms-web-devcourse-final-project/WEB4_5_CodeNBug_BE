package org.codeNbug.mainserver.global.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = {"org.codeNbug.mainserver", "org.codenbug.user",
	"org.codenbug.common"})
public class AppConfig {

}
