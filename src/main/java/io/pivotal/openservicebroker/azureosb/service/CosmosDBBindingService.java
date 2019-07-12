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
import com.microsoft.azure.management.cosmosdb.DatabaseAccountConnectionString;
import com.microsoft.azure.management.cosmosdb.DatabaseAccountListConnectionStringsResult;
import com.microsoft.azure.management.cosmosdb.DatabaseAccountListKeysResult;
import io.pivotal.openservicebroker.azureosb.data.repository.ServiceBindingRepository;
import io.pivotal.openservicebroker.azureosb.model.ServiceBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Objects.requireNonNull;

@Service
public class CosmosDBBindingService implements ServiceInstanceBindingService {

    private static final Logger logger = LoggerFactory.getLogger(CosmosDBBindingService.class);

    private final ServiceBindingRepository bindingRepository;
    private static final String RESOURCE_GROUP = "resourceGroupName";

    public CosmosDBBindingService(ServiceBindingRepository bindingRepository) {
        this.bindingRepository = bindingRepository;
    }

    @Override
    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
        CreateServiceInstanceAppBindingResponseBuilder responseBuilder =
                CreateServiceInstanceAppBindingResponse.builder();

        Optional<ServiceBinding> binding = bindingRepository.findById(request.getBindingId());

        if (binding.isPresent()) {
            responseBuilder
                    .bindingExisted(true)
                    .credentials(retrieveCredentials((String) request.getParameters().get(RESOURCE_GROUP), request.getServiceInstanceId()));
        } else {
            responseBuilder
                    .bindingExisted(false)
                    .credentials(retrieveCredentials((String) request.getParameters().get(RESOURCE_GROUP), request.getServiceInstanceId()));
            bindingRepository.save(new ServiceBinding(request.getBindingId(),request.getParameters(), new HashMap<>()));
        }

        return Mono.just(responseBuilder.build());
    }

    private Map<String, Object> retrieveCredentials(String resourceGroup, String instanceId) {
        Azure azure = null;
        try {
            azure = Azure.authenticate(new File(requireNonNull(this.getClass().getClassLoader().getResource("auth.json")).getFile())).withDefaultSubscription();
        } catch (IOException e) {
            logger.error("Could not retrieve credentials from file auth.json", e);
        }

        DatabaseAccountListConnectionStringsResult connectionStrings = azure.cosmosDBAccounts().listConnectionStrings(resourceGroup, instanceId);
        DatabaseAccountListKeysResult keys = azure.cosmosDBAccounts().listKeys(resourceGroup, instanceId);

        Map<String, Object> credentials = toCredentialsMap(connectionStrings, keys);

        return credentials;
    }

    private Map<String, Object> toCredentialsMap(DatabaseAccountListConnectionStringsResult connectionStrings, DatabaseAccountListKeysResult keys) {
        Map<String, Object> credentials = new HashMap<>();
        List<Map<String, String>> connectionStringList = new ArrayList<>();

        for (DatabaseAccountConnectionString connectionString : connectionStrings.connectionStrings()) {
            Map<String, String> connectionStringMap = new HashMap<>();
            connectionStringMap.put("connectionString", connectionString.connectionString());
            connectionStringMap.put("description", connectionString.description());
            connectionStringList.add(connectionStringMap);
        }

        credentials.put("cosmosdb_connection_strings", connectionStringList);

        Map<String, String> keysMap = new HashMap<>();
        keysMap.put("primaryMasterKey", keys.primaryMasterKey());
        keysMap.put("primaryReadonlyMasterKey", keys.primaryReadonlyMasterKey());
        keysMap.put("secondaryMasterKey", keys.secondaryMasterKey());
        keysMap.put("secondaryReadonlyMasterKey", keys.secondaryReadonlyMasterKey());

        credentials.put("cosmosdb_keys", keysMap);
        return credentials;
    }

    @Override
    public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
        String bindingId = request.getBindingId();

        Optional<ServiceBinding> serviceBinding = bindingRepository.findById(bindingId);

        if (serviceBinding.isPresent()) {
            return Mono.just(GetServiceInstanceAppBindingResponse.builder()
                    .parameters(serviceBinding.get().getParameters())
                    .credentials(retrieveCredentials((String) serviceBinding.get().getParameters().get(RESOURCE_GROUP), request.getServiceInstanceId()))
                    .build());
        } else {
            throw new ServiceInstanceBindingDoesNotExistException(bindingId);
        }
    }

    @Override
    public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
        String bindingId = request.getBindingId();

        if (bindingRepository.existsById(bindingId)) {
            bindingRepository.deleteById(bindingId);

            return Mono.just(DeleteServiceInstanceBindingResponse.builder().build());
        } else {
            throw new ServiceInstanceBindingDoesNotExistException(bindingId);
        }
    }
}