package de.sonallux.spotify.graphql.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtistControllerTest extends BaseControllerTest {
    @Test
    void existingAlbumsPropertyIsNotUsedIfPagingArgumentsAreEqualButOtherArgumentsDoExist() throws Exception {
        mockWebServer.enqueue(createJsonResponse("""
            {"artists": [
                {"id": "foo", "type": "artist", "albums": {
                    "total": 5, "limit": 50, "offset": 0
                }}
            ]}
            """)
        );

        mockWebServer.enqueue(createJsonResponse("""
            {"total": 55, "limit": 50, "offset": 0, "items": [
                {"id": "album-id"}
            ]}
            """)
        );

        graphQlTester.document("""
                query{artist(id:"foo"){id, albums(limit: 50, offset: 0, include_groups: "single"){total, limit, offset}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("artist.id").entity(String.class).isEqualTo("foo")
            .path("artist.albums.total").entity(Integer.class).isEqualTo(55)
            .path("artist.albums.limit").entity(Integer.class).isEqualTo(50)
            .path("artist.albums.offset").entity(Integer.class).isEqualTo(0)
        ;

        var artistsRequest = mockWebServer.takeRequest();
        assertThat(artistsRequest.getPath()).isEqualTo("/artists?ids=foo");

        var artistsAlbumsRequest = mockWebServer.takeRequest();
        assertThat(artistsAlbumsRequest.getPath()).isEqualTo("/artists/foo/albums?include_groups=single&limit=50&offset=0");
    }

    @Test
    void existingAlbumsPropertyIsNotUsedIfPagingArgumentsAreMissingButOtherArgumentsDoExist() throws Exception {
        mockWebServer.enqueue(createJsonResponse("""
            {"artists": [
                {"id": "foo", "type": "artist", "albums": {
                    "total": 5, "limit": 50, "offset": 0
                }}
            ]}
            """)
        );

        mockWebServer.enqueue(createJsonResponse("""
            {"total": 55, "limit": 50, "offset": 0, "items": [
                {"id": "album-id"}
            ]}
            """)
        );

        graphQlTester.document("""
                query{artist(id:"foo"){id, albums(include_groups: "single"){total, limit, offset}}}
                """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("artist.id").entity(String.class).isEqualTo("foo")
            .path("artist.albums.total").entity(Integer.class).isEqualTo(55)
            .path("artist.albums.limit").entity(Integer.class).isEqualTo(50)
            .path("artist.albums.offset").entity(Integer.class).isEqualTo(0)
        ;

        var artistsRequest = mockWebServer.takeRequest();
        assertThat(artistsRequest.getPath()).isEqualTo("/artists?ids=foo");

        var artistsAlbumsRequest = mockWebServer.takeRequest();
        assertThat(artistsAlbumsRequest.getPath()).isEqualTo("/artists/foo/albums?include_groups=single&limit=20&offset=0");
    }
}
