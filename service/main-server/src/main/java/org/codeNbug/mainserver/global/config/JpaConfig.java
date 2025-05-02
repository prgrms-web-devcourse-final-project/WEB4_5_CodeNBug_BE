package org.codeNbug.mainserver.global.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {"org.codeNbug.mainserver.domain", "org.codenbug.user.user.repository"})
@EntityScan(basePackages = {"org.codeNbug.mainserver.domain", "org.codenbug.user.user.entity"})
public class JpaConfig {
}
