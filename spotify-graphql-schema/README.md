# spotify-graphql-schema

[![Maven Central](https://img.shields.io/maven-central/v/de.sonallux.spotify/spotify-graphql-schema)](https://search.maven.org/search?q=g:%22de.sonallux.spotify%22%20AND%20a:%22spotify-graphql-schema%22)
[![GitHub](https://img.shields.io/github/license/sonallux/spotify-graphql)](https://github.com/sonallux/spotify-graphql/blob/master/LICENSE)

GraphQL schema for Spotify Web API using [GraphQL Java](https://www.graphql-java.com). The schema is generated from the Spotify Web API reference documentation using [spotify-web-api-core](https://github.com/sonallux/spotify-web-api).

### Example
````java
var authorizationHeader = "Bearer ???";

var queryString = """
    query {
      track(id: "3VT8hOC5vuDXBsHrR53WFh") {
        name
        artists {name}
        album {name}
        popularity
      }
    }""";

var spotifyGraphQL = new SpotifyGraphQL();
var result = spotifyGraphQL.execute(queryString, authorizationHeader);
````
