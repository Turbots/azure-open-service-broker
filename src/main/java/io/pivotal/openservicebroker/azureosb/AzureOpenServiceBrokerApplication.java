package io.pivotal.openservicebroker.azureosb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:secrets.yml")
public class AzureOpenServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AzureOpenServiceBrokerApplication.class, args);
	}

}
