package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Controller
public class CoreController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> search(@Argument Map<String, Object> arguments, DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/search", arguments, rawLoader);
    }

    @QueryMapping
    Mono<List<String>> markets(DataLoader<String, Map<String, Object>> rawLoader) {
        return loadRawObject("/markets", rawLoader)
            .map(response -> (List<String>)response.get("markets"));
    }
}
