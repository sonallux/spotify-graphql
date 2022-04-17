# spotify-graphql-schema

[![Maven Central](https://img.shields.io/maven-central/v/de.sonallux.spotify/spotify-graphql-schema)](https://search.maven.org/search?q=g:%22de.sonallux.spotify%22%20AND%20a:%22spotify-graphql-schema%22)
[![GitHub](https://img.shields.io/github/license/sonallux/spotify-graphql)](https://github.com/sonallux/spotify-graphql/blob/master/LICENSE)

GraphQL schema generator for Spotify Web API using [GraphQL Java](https://www.graphql-java.com). The schema is generated from the Spotify Web API reference documentation using my [spotify-web-api-core](https://github.com/sonallux/spotify-web-api) library.

### :construction: Status of the Project
The [spotify-web-api-core](https://github.com/sonallux/spotify-web-api) library is no longer updated because Spotify has changed its reference documentation.
Therefore, this GraphQL schema generator won't receive any updates and will be rewritten in the future to use the Spotify OpenApi definition as source.

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
