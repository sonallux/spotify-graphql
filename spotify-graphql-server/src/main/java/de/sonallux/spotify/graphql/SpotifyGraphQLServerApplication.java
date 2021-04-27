package de.sonallux.spotify.graphql;

import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.graphql.schema.SpotifyDataLoaderRegistryFactory;
import de.sonallux.spotify.graphql.schema.generation.SpotifyGraphQLSchemaGenerator;
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

	@Bean
    SpotifyWebApi createSpotifyWebApi() throws IOException {
        return SpotifyWebApiUtils.load();
    }

    @Bean
    HttpClient createRestClient() {
	    return new HttpClient();
    }

    @Bean
    SpotifyDataLoaderRegistryFactory createSpotifyDataLoaderBuilder(SpotifyWebApi spotifyWebApi, HttpClient httpClient) {
	    return new SpotifyDataLoaderRegistryFactory(spotifyWebApi, httpClient);
    }

    @Bean
    GraphQLSchema createSchema(SpotifyWebApi spotifyWebApi) {
	    return new SpotifyGraphQLSchemaGenerator().generate(spotifyWebApi);
    }
}
