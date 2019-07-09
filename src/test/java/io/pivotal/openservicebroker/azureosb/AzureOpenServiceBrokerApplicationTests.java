package io.pivotal.openservicebroker.azureosb;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import io.pivotal.openservicebroker.azureosb.service.CosmosDBService;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.main.web-application-type=reactive", webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient
public class AzureOpenServiceBrokerApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private CosmosDBService cosmosDBService;


	final static String INSTANCE_ID = "my-service-instance-id";
	final static String BINDING_ID = "my-service-instance-id";
	@Test
	public void contextLoads() {
	}

	@Test
	public void catalog() {
		webTestClient.get().uri("/v2/catalog").exchange().expectStatus().isOk();
	}

	@Test
	public void createServiceInstance() {
		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder().serviceDefinitionId("cosmosdb")
				.planId("db-small").build();
		webTestClient.put().uri("/v2/service_instances/{instanceId}", INSTANCE_ID)
				.body(BodyInserters.fromObject(request)).exchange().expectStatus().isEqualTo(HttpStatus.OK);
	}

	@Test
	public void bindServiceInstance() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest.builder().serviceDefinitionId("cosmosdb")
		.planId("db-small")
		.serviceInstanceId(INSTANCE_ID).build();

		webTestClient.put().uri("/v2/service_instances/{instanceId}/service_bindings/{bindingId}",INSTANCE_ID, BINDING_ID)
				.body(BodyInserters.fromObject(request)).exchange().expectStatus().isEqualTo(HttpStatus.OK);
	}
}
