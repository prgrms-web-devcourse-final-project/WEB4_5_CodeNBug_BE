package org.codeNbug.mainserver.global.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"org.codeNbug.mainserver.domain", "org.codenbug.user"})
@EnableJpaRepositories(basePackages = {"org.codeNbug.mainserver.domain.*.repository", "org.codenbug.user.repository"})
public class JpaConfig {
}
