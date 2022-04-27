package de.sonallux.spotify.graphql.controller;

import de.sonallux.spotify.graphql.MockWebServerJUnitExtension;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.graphql.web.WebGraphQlHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spotify.base-url=http://localhost:" + MockWebServerJUnitExtension.PORT)
@ExtendWith(MockWebServerJUnitExtension.class)
class AlbumControllerTest {
    @Autowired
    WebGraphQlHandler webGraphQlHandler;

    GraphQlTester graphQlTester;

    MockWebServer mockWebServer;

    @BeforeEach
    void setUp() {
        graphQlTester = WebGraphQlTester
            .builder(webGraphQlHandler)
            .header(HttpHeaders.AUTHORIZATION, "spotify_test_auth_token")
            .build();
    }

    @Test
    void multipleAlbumsAreLoaded() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"albums": [
                    {"id": "foo", "type": "album", "name": "Foo Name"},
                    {"id": "bar", "type": "album", "name": "Bar Name"}
                ]}
                """)
        );

        graphQlTester.document("""
            query{albums(uris: ["spotify:album:foo","spotify:album:bar","spotify:album:foo"]){name}}
            """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("albums[0].name").entity(String.class).isEqualTo("Foo Name")
            .path("albums[1].name").entity(String.class).isEqualTo("Bar Name")
            .path("albums[2].name").entity(String.class).isEqualTo("Foo Name");

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/albums?ids=foo,bar");
    }

    @Test
    void missingArtistsPropertiesAreLoaded() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"albums": [
                    {"id": "foo", "type": "album", "name": "Bar", "artists":[
                        {"id": "test", "type": "artist", "name": "Test Artist"}
                    ]}
                ]}
                """)
        );

        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"artists": [
                    {"id": "test", "type": "artist", "name": "Test Artist", "popularity": 42}
                ]}
                """)
        );

        graphQlTester.document("""
                query{album(id:"foo"){id, name, artists{name, popularity}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("album.id").entity(String.class).isEqualTo("foo")
            .path("album.name").entity(String.class).isEqualTo("Bar")
            .path("album.artists[0].name").entity(String.class).isEqualTo("Test Artist")
            .path("album.artists[0].popularity").entity(Integer.class).isEqualTo(42);

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/albums?ids=foo");

        var artistsRequest = mockWebServer.takeRequest();
        assertThat(artistsRequest.getPath()).isEqualTo("/artists?ids=test");
    }

    @Test
    void existingTracksPropertyIsUsedIfPagingArgumentsAreMissing() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"albums": [
                    {"id": "foo", "type": "album", "tracks": {
                        "total": 5, "limit": 20, "offset": 0
                    }}
                ]}
                """)
        );

        graphQlTester.document("""
                query{album(id:"foo"){id, tracks{total, limit, offset}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("album.id").entity(String.class).isEqualTo("foo")
            .path("album.tracks.total").entity(Integer.class).isEqualTo(5)
            .path("album.tracks.limit").entity(Integer.class).isEqualTo(20)
            .path("album.tracks.offset").entity(Integer.class).isEqualTo(0)
        ;

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/albums?ids=foo");
    }

    @Test
    void existingTracksPropertyIsUsedIfPagingArgumentsAreEqual() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"albums": [
                    {"id": "foo", "type": "album", "tracks": {
                        "total": 5, "limit": 20, "offset": 20
                    }}
                ]}
                """)
        );

        graphQlTester.document("""
                query{album(id:"foo"){id, tracks(limit: 20, offset: 20){total, limit, offset}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("album.id").entity(String.class).isEqualTo("foo")
            .path("album.tracks.total").entity(Integer.class).isEqualTo(5)
            .path("album.tracks.limit").entity(Integer.class).isEqualTo(20)
            .path("album.tracks.offset").entity(Integer.class).isEqualTo(20)
        ;

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/albums?ids=foo");
    }

    @Test
    void tracksPropertyIsLoadedIfPagingArgumentsDoNotMatch() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"albums": [
                    {"id": "foo", "type": "album", "tracks": {
                        "total": 25, "limit": 20, "offset": 0, "items": [{"id": "foo"}]
                    }}
                ]}
                """)
        );

        mockWebServer.enqueue(new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {"total": 55, "limit": 50, "offset": 50, "items": [
                    {"id": "track-id"}
                ]}
                """)
        );

        graphQlTester.document("""
                query{album(id:"foo"){id, tracks(limit: 50, offset: 50){total, limit, offset, items{id}}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("album.id").entity(String.class).isEqualTo("foo")
            .path("album.tracks.total").entity(Integer.class).isEqualTo(55)
            .path("album.tracks.limit").entity(Integer.class).isEqualTo(50)
            .path("album.tracks.offset").entity(Integer.class).isEqualTo(50)
            .path("album.tracks.items[0].id").entity(String.class).isEqualTo("track-id")
        ;

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/albums?ids=foo");

        var albumsTracksRequest = mockWebServer.takeRequest();
        assertThat(albumsTracksRequest.getPath()).isEqualTo("/albums/foo/tracks?limit=50&offset=50");
    }
}
