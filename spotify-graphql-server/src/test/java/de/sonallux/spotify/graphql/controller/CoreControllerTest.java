package de.sonallux.spotify.graphql.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreControllerTest extends BaseControllerTest {

    @Test
    void searchTermIsEncoded() throws InterruptedException {
        mockWebServer.enqueue(createJsonResponse("""
            {"artists": {"items": [
                {"id": "foo_bar", "name": "Kontra K"}
            ]}}
            """)
        );

        graphQlTester.document("""
            query {search(q: "Kontra K" type: "artist") {artists {items {name id}}}}
            """)
            .execute()
            .errors().satisfy(errors -> assertThat(errors).isEmpty())
            .path("search.artists.items[0].name").entity(String.class).isEqualTo("Kontra K")
            .path("search.artists.items[0].id").entity(String.class).isEqualTo("foo_bar");

        var albumsRequest = mockWebServer.takeRequest();
        assertThat(albumsRequest.getPath()).isEqualTo("/search?limit=20&offset=0&q=Kontra%20K&type=artist");
    }
}
