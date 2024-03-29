# spotify-graphql-server

[![Maven Central](https://img.shields.io/maven-central/v/de.sonallux.spotify/spotify-graphql-server)](https://search.maven.org/search?q=g:%22de.sonallux.spotify%22%20AND%20a:%22spotify-graphql-server%22)
[![GitHub](https://img.shields.io/github/license/sonallux/spotify-graphql)](https://github.com/sonallux/spotify-graphql/blob/master/LICENSE)

A GraphQl server for Spotify's Web Api.
 
### :construction: Status of the Project
The old implementation using [GraphQL Java Kickstart](https://www.graphql-java-kickstart.com/spring-boot) is currently rewritten based on the new official [Spring GraphQL Project](https://spring.io/projects/spring-graphql).

### Configuration
To run the Spotify GraphQL server a valid client id and client secret must be obtained by registering your application on the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
These two values can be added in to the `application.yml` configuration file or passed with the following environment variables:
````
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_SPOTIFY_CLIENT_ID=<your client id>
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_SPOTIFY_CLIENT_SECRET=<your client secret>
````
