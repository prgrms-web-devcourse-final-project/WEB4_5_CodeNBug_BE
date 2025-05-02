package org.codeNbug.queueserver.global;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = {"org.codeNbug.queueserver", "org.codenbug.user"})
@EnableJpaRepositories(basePackages = {"org.codenbug.user.user.repository"})
@EntityScan(basePackages = {"org.codenbug.user.user.entity"})
public class GlobalConfig {
}
