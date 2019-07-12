package io.pivotal.openservicebroker.azureosb;

import io.pivotal.openservicebroker.azureosb.service.CosmosDBService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Duration;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.main.web-application-type=reactive", webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient
public class AzureOpenServiceBrokerApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	final static String INSTANCE_ID = "andreas-test-db2";
	final static String BINDING_ID = "my-binding-id";
	final static String RESOURCE_GROUP = "test-resource-group";
	final static String RESOURCE_GROUP_KEY = "resourceGroupName";
	final static String ORG_GUID = "ba950d69-cbbc-4ede-9341-0965aed49db8";
	final static String SPACE_GUID = "74036a83-f676-4e8b-b0ec-dfb812fff551";

	@Test
	public void contextLoads() {
	}

	@Test
	public void catalog() {
		webTestClient.get().uri("/v2/catalog").exchange().expectStatus().isOk();
	}

	@Test
	public void theOneAndOnlyTest() throws InterruptedException {
		webTestClient = webTestClient.mutate().responseTimeout(Duration.ofMinutes(15)).build();

		// Create Instance

		CreateServiceInstanceRequest request = CreateServiceInstanceRequest.builder()
				.serviceDefinitionId("cosmosdb")
				.parameters(RESOURCE_GROUP_KEY, RESOURCE_GROUP)
				.planId("db-small")
				.context(CloudFoundryContext.builder()
						.organizationGuid(ORG_GUID)
						.spaceGuid(SPACE_GUID)
						.build())
				.build();
		webTestClient.put().uri("/v2/service_instances/{instanceId}", INSTANCE_ID)
				.body(BodyInserters.fromObject(request)).exchange().expectStatus().isEqualTo(HttpStatus.OK);

		// Get Instance

		with().pollDelay(5, SECONDS).and().pollInterval(5, SECONDS).await().atMost(15, MINUTES).untilAsserted(() ->
				webTestClient.get().uri("/v2/service_instances/{instanceId}", INSTANCE_ID)
				.exchange().expectStatus().isEqualTo(HttpStatus.OK));

		// Create Service Binding

		CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = CreateServiceInstanceBindingRequest.builder()
				.serviceDefinitionId("cosmosdb")
				.parameters(RESOURCE_GROUP_KEY, RESOURCE_GROUP)
				.planId("db-small")
				.serviceInstanceId(INSTANCE_ID).build();

		webTestClient.put().uri("/v2/service_instances/{instanceId}/service_bindings/{bindingId}", INSTANCE_ID, BINDING_ID)
				.body(BodyInserters.fromObject(createServiceInstanceBindingRequest)).exchange().expectStatus().isEqualTo(HttpStatus.CREATED);

		// Delete Service Binding

		Thread.sleep(15_000);

		// TODO: set header X-Broker-API-Originating-Identity to set the org and space
		webTestClient.delete().uri("/v2/service_instances/{instanceId}/service_bindings/{bindingId}?service_id={serviceId}&plan_id={planId}", INSTANCE_ID, BINDING_ID, "cosmosdb", "db-small")
				.exchange().expectStatus().isEqualTo(HttpStatus.OK);

		// Delete Instance

		// TODO: set header X-Broker-API-Originating-Identity to set the org and space
		webTestClient.delete().uri("/v2/service_instances/{instanceId}?service_id={serviceId}&plan_id={planId}", INSTANCE_ID, "cosmosdb", "db-small")
				.exchange().expectStatus().isEqualTo(HttpStatus.OK);
	}
}
