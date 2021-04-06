package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.graphql.schema.SchemaCreator;
import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
import graphql.schema.GraphQLSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class SpotifyGraphQLServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotifyGraphQLServerApplication.class, args);
	}

    private SpotifyWebApi readApiDocumentation() {
        try {
            return SpotifyWebApiUtils.load();
        } catch (IOException e) {
            System.err.println("Failed to read web API documentation file: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    @Bean
    HttpClient createRestClient() {
	    return new HttpClient();
    }

    @Bean
    SpotifyDataLoaderRegistryFactory createSpotifyDataLoaderBuilder(HttpClient httpClient) {
	    return new SpotifyDataLoaderRegistryFactory(httpClient);
    }

    @Bean
    GraphQLSchema createSchema() {
	    var apiDocumentation = readApiDocumentation();
	    return new SchemaCreator().generate(apiDocumentation);
    }
}
