package fr.triplan.bffvoyageur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BffVoyageurApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffVoyageurApplication.class, args);
    }
}
