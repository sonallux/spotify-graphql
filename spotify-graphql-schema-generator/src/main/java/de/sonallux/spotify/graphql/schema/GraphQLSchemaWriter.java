package de.sonallux.spotify.graphql.schema;

import graphql.schema.DefaultGraphqlTypeComparatorRegistry;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLUnionType;
import graphql.schema.idl.SchemaPrinter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.*;

@RequiredArgsConstructor
public class GraphQLSchemaWriter {
    private final Path outputFolder;

    public void writeTypes(Collection<MappedType> mappedTypes, Collection<GraphQLUnionType> unionTypes) {
        for (var entry : mappedTypes.stream()
            .collect(groupingBy(MappedType::category, mapping(MappedType::graphQLSchemaElement, toList()))).entrySet()) {
            var category = entry.getKey();
            var schemaElements = entry.getValue();

            if (category == Mapping.Category.COMMON) {
                schemaElements.addAll(unionTypes);
            }

            writeSchema(category, schemaElements);
        }
    }

    private void writeSchema(TypeMapping.Category category, List<GraphQLSchemaElement> schema) {
        schema.sort(DefaultGraphqlTypeComparatorRegistry.DEFAULT_COMPARATOR);

        var path = outputFolder.resolve(category.name().toLowerCase() + ".graphqls");
        var schemaPrinter = new SchemaPrinter(SchemaPrinter.Options.defaultOptions().useAstDefinitions(true));
        try {
            Files.writeString(path, schemaPrinter.print(schema));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
