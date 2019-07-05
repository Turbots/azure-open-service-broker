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

import io.pivotal.openservicebroker.azureosb.data.repository.ServiceInstanceRepository;
import io.pivotal.openservicebroker.azureosb.model.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class BookStoreServiceInstanceService implements ServiceInstanceService {

    private static final Logger log = LoggerFactory.getLogger(BookStoreServiceInstanceService.class);

    private final ServiceInstanceRepository instanceRepository;

    public BookStoreServiceInstanceService(ServiceInstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
        log.info("Creating Service Instance [{}] for Service [{}] and Plan [{}]", request.getServiceInstanceId(), request.getServiceDefinitionId(), request.getPlanId());

        String instanceId = request.getServiceInstanceId();

        CreateServiceInstanceResponseBuilder responseBuilder = CreateServiceInstanceResponse.builder();

        if (instanceRepository.existsById(instanceId)) {
            responseBuilder.instanceExisted(true);
        } else {
            saveInstance(request, instanceId);
        }

        return Mono.just(responseBuilder.operation("Service Instance Created").build());
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
