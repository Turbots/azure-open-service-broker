/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.openservicebroker.azureosb.service;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.cosmosdb.CosmosDBAccount;
import com.microsoft.azure.management.cosmosdb.DatabaseAccountKind;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import io.pivotal.openservicebroker.azureosb.data.repository.ServiceInstanceRepository;
import io.pivotal.openservicebroker.azureosb.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
public class CosmosDBService implements ServiceInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(CosmosDBService.class);

    private final ServiceInstanceRepository instanceRepository;

    public CosmosDBService(ServiceInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    // EXAMPLE ON HOW TO FETCH A SECURITY TOKEN AND CALL THE AZURE API
    //
    //@PostConstruct
    public void postConstruct() {


//        String token = webClient.post()
//                .uri("https://login.microsoftonline.com/" + tenantId + "/oauth2/token")
//                .body(BodyInserters
//                        .fromFormData("grant_type", "client_credentials")
//                        .with("scope", "user_impersonation")
//                        .with("resource", "https://management.azure.com/")
//                ).headers(headers -> {
//                    headers.setBasicAuth(clientId, clientSecret);
//                })
//                .retrieve()
//                .bodyToMono(SecurityToken.class)
//                .map(response -> {
//                    logger.info("Received a security token [{}]", response.getAccessToken());
//                    return response.getAccessToken();
//                }).block();
//
//        webClient.get()
//                .uri("https://management.azure.com/subscriptions/" + subscriptionId + "/resourceGroups/test-resource-group/providers/Microsoft.DocumentDB/databaseAccounts/test-cosmos-db-hackaton?api-version=2015-04-08")
//                .headers(headers -> headers.setBearerAuth(token))
//                .retrieve()
//                .bodyToMono(String.class)
//                .map(response -> {
//                    logger.info(response);
//                    return "Response: " + response;
//                }).block();
    }

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
        logger.info("Creating Service Instance [{}] for Service [{}] and Plan [{}]", request.getServiceInstanceId(), request.getServiceDefinitionId(), request.getPlanId());

        String instanceId = request.getServiceInstanceId();

        CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

        if (instanceRepository.existsById(instanceId)) {
            return Mono.just(responseBuilder.instanceExisted(true).operation("Service Instance Already Exists in the database").build());
        } else {
            // TO BE IMPLEMENTED

            Azure azure = null;
            try {
                azure = Azure.authenticate(new File(this.getClass().getClassLoader().getResource("auth.json").getFile())).withDefaultSubscription();
            } catch (IOException e) {
                e.printStackTrace();
            }
            CosmosDBAccount cosmosDBAccount = azure.cosmosDBAccounts().define("andreas-test-db")
                    .withRegion(Region.EUROPE_NORTH)
                    .withNewResourceGroup("test-resource-group")
                    .withKind(DatabaseAccountKind.GLOBAL_DOCUMENT_DB)
                    .withSessionConsistency()
                    .withWriteReplication(Region.EUROPE_NORTH)
                    .withReadReplication(Region.EUROPE_NORTH)
                    .create();

            saveInstance(request, instanceId);
            return Mono.just(responseBuilder.instanceExisted(true).operation("Service Instance Created").build());
        }
    }

    @Override
    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
        String instanceId = request.getServiceInstanceId();

        Optional<ServiceInstance> serviceInstance = instanceRepository.findById(instanceId);

        if (serviceInstance.isPresent()) {
            return Mono.just(GetServiceInstanceResponse.builder()
                    .serviceDefinitionId(serviceInstance.get().getServiceDefinitionId())
                    .planId(serviceInstance.get().getPlanId())
                    .parameters(serviceInstance.get().getParameters())
                    .build());
        } else {
            throw new ServiceInstanceDoesNotExistException(instanceId);
        }
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
        String instanceId = request.getServiceInstanceId();

        if (instanceRepository.existsById(instanceId)) {
            // TO BE IMPLEMENTED

            instanceRepository.deleteById(instanceId);

            return Mono.just(DeleteServiceInstanceResponse.builder().build());
        } else {
            throw new ServiceInstanceDoesNotExistException(instanceId);
        }
    }

    private void saveInstance(CreateServiceInstanceRequest request, String instanceId) {
        ServiceInstance serviceInstance = new ServiceInstance(instanceId, request.getServiceDefinitionId(),
                request.getPlanId(), request.getParameters());
        instanceRepository.save(serviceInstance);
    }
}
