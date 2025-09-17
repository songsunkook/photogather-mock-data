package io.spring.imagegenerator;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing
public class ImageGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageGeneratorApplication.class, args);
    }
}