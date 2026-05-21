package com.akgeneralstore;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.akgeneralstore")
@EntityScan(basePackages = "com.akgeneralstore.entity")
@EnableJpaRepositories(basePackages = "com.akgeneralstore.repository")
public class AkGeneralStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AkGeneralStoreApplication.class, args);
    }
}
