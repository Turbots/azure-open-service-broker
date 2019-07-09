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

import io.pivotal.openservicebroker.azureosb.data.repository.ServiceBindingRepository;
import io.pivotal.openservicebroker.azureosb.model.ServiceBinding;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Optional;

@Service
public class CosmosDBBindingService implements ServiceInstanceBindingService {

    private final ServiceBindingRepository bindingRepository;

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
                    .credentials(binding.get().getCredentials());
        } else {
            responseBuilder
                    .bindingExisted(false)
                    .credentials(new HashMap<String, Object>());
        }

        return Mono.just(responseBuilder.build());
    }

    @Override
    public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
        String bindingId = request.getBindingId();

        Optional<ServiceBinding> serviceBinding = bindingRepository.findById(bindingId);

        if (serviceBinding.isPresent()) {
            return Mono.just(GetServiceInstanceAppBindingResponse.builder()
                    .parameters(serviceBinding.get().getParameters())
                    .credentials(serviceBinding.get().getCredentials())
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