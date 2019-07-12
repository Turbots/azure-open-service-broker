package io.pivotal.openservicebroker.azureosb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.cloudfoundry.discovery.EnableCloudFoundryClient;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@EnableCloudFoundryClient
public class AzureOpenServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AzureOpenServiceBrokerApplication.class, args);
	}

}
