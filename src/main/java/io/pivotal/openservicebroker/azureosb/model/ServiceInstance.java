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

package io.pivotal.openservicebroker.azureosb.model;


import javax.persistence.*;
import java.util.Map;

@Entity
@Table(name = "service_instances")
public class ServiceInstance {

    @Id
    @Column
    private final String instanceId;

    @Column
    private final String serviceDefinitionId;

    @Column
    private final String planId;

    @Column
    private final String spaceName;

    @Column
    private final String orgName;

    @ElementCollection
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    @CollectionTable(name = "service_instance_parameters", joinColumns = @JoinColumn(name = "instance_id"))
    @Convert(converter = ObjectToStringConverter.class, attributeName = "value")
    private final Map<String, Object> parameters;

    @SuppressWarnings("unused")
    private ServiceInstance() {
        instanceId = null;
        serviceDefinitionId = null;
        planId = null;
        orgName = null;
        spaceName = null;
        parameters = null;
    }

    public ServiceInstance(String instanceId, String serviceDefinitionId, String planId, String orgName, String spaceName,
                           Map<String, Object> parameters) {
        this.instanceId = instanceId;
        this.serviceDefinitionId = serviceDefinitionId;
        this.planId = planId;
        this.orgName = orgName;
        this.spaceName = spaceName;
        this.parameters = parameters;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServiceDefinitionId() {
        return serviceDefinitionId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
