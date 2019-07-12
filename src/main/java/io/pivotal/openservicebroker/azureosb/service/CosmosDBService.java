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
import com.microsoft.rest.ServiceCallback;
import io.pivotal.openservicebroker.azureosb.data.repository.ServiceInstanceRepository;
import io.pivotal.openservicebroker.azureosb.model.ServiceInstance;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
public class CosmosDBService implements ServiceInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(CosmosDBService.class);
    private static final String RESOURCE_GROUP = "resourceGroupName";

    private final CloudFoundryOperations cloudFoundryOperations;

    private final ServiceInstanceRepository instanceRepository;

    public CosmosDBService(CloudFoundryOperations cloudFoundryOperations, ServiceInstanceRepository instanceRepository) {
        this.cloudFoundryOperations = cloudFoundryOperations;
        this.instanceRepository = instanceRepository;
    }

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
        logger.info("Creating Service Instance [{}] for Service [{}] and Plan [{}]", request.getServiceInstanceId(), request.getServiceDefinitionId(), request.getPlanId());

        String instanceId = request.getServiceInstanceId();

        CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

        if (instanceRepository.existsById(instanceId)) {
            return Mono.just(responseBuilder.instanceExisted(true).operation("Service Instance Already Exists in the database").build());
        } else {
            Azure azure = null;
            try {
                azure = Azure.authenticate(new File(this.getClass().getClassLoader().getResource("auth.json").getFile())).withDefaultSubscription();
            } catch (IOException e) {
                e.printStackTrace();
            }
            azure.cosmosDBAccounts().define(instanceId)
                    .withRegion(Region.EUROPE_WEST)
                    .withNewResourceGroup((String) request.getParameters().get(RESOURCE_GROUP))
                    .withKind(DatabaseAccountKind.GLOBAL_DOCUMENT_DB)
                    .withSessionConsistency()
                    .withWriteReplication(Region.EUROPE_NORTH)
                    .createAsync(
                            new ServiceCallback<CosmosDBAccount>() {
                                @Override
                                public void failure(Throwable throwable) {
                                    logger.error("Houston, we have a problem!", throwable);
                                }

                                @Override
                                public void success(CosmosDBAccount cosmosDBAccount) {
                                    logger.info("Successfully started database creation for {}. Actual availability can take more than 15 minutes.", instanceId);
                                    saveInstance(request, instanceId);
                                }
                            }
                    );

            return Mono.just(responseBuilder.instanceExisted(true).operation("Service Instance Creating").build());
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
        logger.info("Deleting Service Instance [{}] for Service [{}] and Plan [{}]", request.getServiceInstanceId(), request.getServiceDefinitionId(), request.getPlanId());

        String instanceId = request.getServiceInstanceId();

        if (instanceRepository.existsById(instanceId)) {
            verifySpaceAndOrg(request, instanceId);

            Azure azure = null;
            try {
                azure = Azure.authenticate(new File(this.getClass().getClassLoader().getResource("auth.json").getFile())).withDefaultSubscription();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Optional<CosmosDBAccount> optionalCosmosDBAccount = azure.cosmosDBAccounts().list().stream().filter(c -> c.name().equals(request.getServiceInstanceId())).findFirst();

            CosmosDBAccount cosmosDBAccount = optionalCosmosDBAccount.orElseThrow(RuntimeException::new);

            azure.cosmosDBAccounts().deleteByIdAsync(cosmosDBAccount.id(),
                    new ServiceCallback<Void>() {
                        @Override
                        public void failure(Throwable throwable) {
                            logger.error("Houston, we have a problem!", throwable);
                        }

                        @Override
                        public void success(Void aVoid) {
                            instanceRepository.deleteById(instanceId);
                        }
                    });

            return Mono.just(DeleteServiceInstanceResponse.builder().build());
        } else {
            throw new ServiceInstanceDoesNotExistException(instanceId);
        }
    }

    private void verifySpaceAndOrg(DeleteServiceInstanceRequest request, String instanceId) {
        Mono<String> orgName = resolveOrgName(((CloudFoundryContext) request.getOriginatingIdentity()).getOrganizationGuid());
        Mono<String> spaceName = resolveSpaceName(((CloudFoundryContext) request.getOriginatingIdentity()).getSpaceGuid());

        String instanceOrgName = instanceRepository.findById(instanceId).get().getOrgName();
        boolean sameOrg = instanceOrgName.equals(orgName);
        if (!sameOrg) {
            throw new RuntimeException("Requesting to delete serviceInstance for wrong org. Your org is " + orgName + ", but the instance has org " + instanceOrgName + ".");
        }
        String instanceSpaceName = instanceRepository.findById(instanceId).get().getSpaceName();
        boolean sameSpace = instanceSpaceName.equals(spaceName);
        if (!sameSpace) {
            throw new RuntimeException("Requesting to delete serviceInstance for wrong space. Your space is " + spaceName + ", but the instance has space " + instanceSpaceName + ".");
        }
    }

    private void saveInstance(CreateServiceInstanceRequest request, String instanceId) {
        Mono<String> orgName = resolveOrgName(((CloudFoundryContext) request.getContext()).getOrganizationGuid());
        Mono<String> spaceName = resolveSpaceName(((CloudFoundryContext) request.getContext()).getSpaceGuid());
        Mono<Tuple2<String, String>> orgSpaceTuple = orgName.zipWith(spaceName);
        orgSpaceTuple.subscribe(tuple -> {
            logger.info("Saving service with id {} into the Service Broker database", instanceId);
            ServiceInstance serviceInstance = new ServiceInstance(instanceId, request.getServiceDefinitionId(),
                    request.getPlanId(), tuple.getT1(), tuple.getT2(), request.getParameters());
            instanceRepository.save(serviceInstance);
        });
    }

    private Mono<String> resolveOrgName(String organizationGuid) {
        return cloudFoundryOperations.organizations()
                .list()
                .filter(os -> os.getId().equals(organizationGuid))
                .map(OrganizationSummary::getName)
                .next();
    }

    private Mono<String> resolveSpaceName(String spaceGuid) {
        return cloudFoundryOperations.spaces()
                .list()
                .filter(os -> os.getId().equals(spaceGuid))
                .map(SpaceSummary::getName)
                .next();
    }

}
