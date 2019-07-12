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
@Table(name = "service_bindings")
public class ServiceBinding {
    @Id
    @Column
    private final String bindingId;

    @ElementCollection
    @MapKeyColumn(name = "parameter_name")
    @Column(name = "parameter_value")
    @CollectionTable(name = "service_binding_parameters", joinColumns = @JoinColumn(name = "binding_id"))
    @Convert(converter = ObjectToStringConverter.class, attributeName = "value")
    private final Map<String, Object> parameters;

    /**
     * Currently not being used. We don't want to store plain text credentials in the database.
     * If we would use CredHub, this would be okay though.
     * For now, we call Azure every time we need the credentials.
     */
    @ElementCollection
    @MapKeyColumn(name = "credential_name")
    @Column(name = "credential_value")
    @CollectionTable(name = "service_binding_credentials", joinColumns = @JoinColumn(name = "binding_id"))
    @Convert(converter = ObjectToStringConverter.class, attributeName = "value")
    private final Map<String, Object> credentials;

    @SuppressWarnings("unused")
    private ServiceBinding() {
        this.bindingId = null;
        this.parameters = null;
        this.credentials = null;
    }

    public ServiceBinding(String bindingId, Map<String, Object> parameters, Map<String, Object> credentials) {
        this.bindingId = bindingId;
        this.parameters = parameters;
        this.credentials = credentials;
    }

    public String getBindingId() {
        return bindingId;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
