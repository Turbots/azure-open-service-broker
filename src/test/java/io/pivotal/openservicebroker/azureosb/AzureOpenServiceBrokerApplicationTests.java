package io.pivotal.openservicebroker.azureosb;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.test.context.junit4.SpringRunner;

import io.pivotal.openservicebroker.azureosb.service.CosmosDBService;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AzureOpenServiceBrokerApplicationTests {

	@Autowired
	private CosmosDBService cosmosDBService;

	@Test
	public void contextLoads() {
	}

	@Test
	public void createServiceInstance()	{
		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder().build();
		Mono<CreateServiceInstanceResponse> responseMono = cosmosDBService.createServiceInstance(request);
		CreateServiceInstanceResponse response = responseMono.block();
		Assertions.assertThat(response.isInstanceExisted()).isTrue();
	}
}
