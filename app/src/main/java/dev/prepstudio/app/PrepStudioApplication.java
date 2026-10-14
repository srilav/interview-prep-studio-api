package dev.prepstudio.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Assembles the bounded-context modules into one deployable (modular monolith). */
@SpringBootApplication(scanBasePackages = "dev.prepstudio")
@EntityScan("dev.prepstudio")
@EnableJpaRepositories("dev.prepstudio")
@EnableAsync
@EnableScheduling
public class PrepStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrepStudioApplication.class, args);
    }
}
