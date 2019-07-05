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
        Plan account = Plan.builder()
                .id("account")
                .name("account")
                .description("Creates a CosmosDB account")
                .free(true)
                .bindable(false)
                .build();
        Plan database = Plan.builder()
                .id("database")
                .name("database")
                .description("Creates an empty CosmosDB database")
                .free(true)
                .build();

        return ServiceDefinition.builder()
                .id("cosmosdb")
                .name("CosmosDB")
                .description("CosmosDB Account")
                .bindable(true)
                .planUpdateable(false)
                .tags("cosmosdb", "database")
                .plans(account, database)
                .build();
    }
}
