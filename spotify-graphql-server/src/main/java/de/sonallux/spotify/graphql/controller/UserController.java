package de.sonallux.spotify.graphql.controller;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
public class UserController extends BaseController {
    @QueryMapping
    Mono<Map<String, Object>> me(DataLoader<String, Map<String, Object>> rawLoader) {
        return Mono.fromFuture(rawLoader.load("/me"));
    }

    @QueryMapping
    Mono<Map<String, Object>> user(@Argument("user_id") String userId, DataLoader<String, Map<String, Object>> rawLoader) {
        return Mono.fromFuture(rawLoader.load("/users/" + userId));
    }
}
