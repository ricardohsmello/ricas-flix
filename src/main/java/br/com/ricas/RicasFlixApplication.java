package br.com.ricas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RicasFlixApplication {

	public static void main(String[] args) {
		SpringApplication.run(RicasFlixApplication.class, args);
	}

}
