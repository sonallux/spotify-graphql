package de.sonallux.spotify.graphql.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GraphQlExceptionResolver implements DataFetcherExceptionResolver {
    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof IllegalArgumentException) {
            return Mono.just(List.of(GraphqlErrorBuilder
                .newError(environment)
                .message(exception.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build()
            ));
        }
        else if (exception instanceof MissingAuthorizationException) {
            return Mono.just(List.of(GraphqlErrorBuilder
                .newError(environment)
                .message(exception.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build()
            ));
        }
        return Mono.empty();
    }
}
