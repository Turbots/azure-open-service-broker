package io.pivotal.openservicebroker.azureosb.catalog;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfiguration {

    @Bean
    public Catalog catalog() {
        return Catalog.builder()
                .serviceDefinitions(cosmosDbService())
                .build();
    }

    private ServiceDefinition cosmosDbService() {
        Plan database = Plan.builder()
                .id("db-small")
                .name("db-small")
                .description("Provisions a new CosmosDB database on Azure")
                .free(true)
                .build();

        return ServiceDefinition.builder()
                .id("cosmosdb")
                .name("CosmosDB")
                .description("CosmosDB Service Broker")
                .bindable(true)
                .planUpdateable(false)
                .tags("cosmosdb", "database")
                .plans(database)
                .build();
    }
}
