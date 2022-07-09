package de.sonallux.spotify.graphql.schema;

import graphql.schema.*;
import graphql.schema.idl.SchemaPrinter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
public class GraphQLSchemaWriter {
    public static final Comparator<GraphQLNamedType> TYPE_COMPARATOR = Comparator
        .comparing((GraphQLNamedType type) -> "Query".equals(type.getName())).reversed()
        .thenComparing(type -> (GraphQLSchemaElement)type, DefaultGraphqlTypeComparatorRegistry.DEFAULT_COMPARATOR);

    private final Path outputFolder;

    public void writeTypes(Map<Mapping.Category, List<GraphQLNamedType>> typeMap) {
        typeMap.forEach(this::writeSchema);
    }

    private void writeSchema(TypeMapping.Category category, List<GraphQLNamedType> types) {
        var path = outputFolder.resolve(category.name().toLowerCase() + ".graphqls");
        var schemaPrinter = new SchemaPrinter();

        var string = types.stream()
            .sorted(TYPE_COMPARATOR)
            .map(type -> {
                var s = schemaPrinter.print(type);
                if (isObjectTypeExtension(category, type)) {
                    // Note: this only works if the type has no description!
                    s = "extend " + s;
                }
                return s;
            })
            .collect(joining());

        try {
            Files.writeString(path, string);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isObjectTypeExtension(Mapping.Category category, GraphQLType type) {
        return category != Mapping.Category.CORE && type instanceof GraphQLObjectType objectType && objectType.getName().equals("Query");
    }

}
