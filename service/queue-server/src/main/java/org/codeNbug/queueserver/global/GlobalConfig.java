package org.codeNbug.queueserver.global;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = {"org.codeNbug.queueserver", "org.codenbug.user.*", "org.codenbug.common.util",
	"org.codenbug.logging"})
public class GlobalConfig {
}
