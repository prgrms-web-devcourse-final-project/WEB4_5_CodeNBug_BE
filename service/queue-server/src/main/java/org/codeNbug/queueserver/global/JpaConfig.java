package org.codeNbug.queueserver.global;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"org.codenbug.user"})
@EnableJpaRepositories(basePackages = {"org.codenbug.user"})
public class JpaConfig {
}
