package de.sonallux.spotify.graphql.schema.generation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Getter
@Setter
@AllArgsConstructor
class SchemaObject {
    private final String name;
    private String description;
    private Map<String, SchemaField> fields;

    SchemaObject(String name) {
        this(name, null, new TreeMap<>());
    }

    public void addField(SchemaField field) {
        fields.put(field.getName(), field);
    }

    public SchemaField getField(String fieldName) {
        return fields.get(fieldName);
    }

    public SchemaField computeFieldIfAbsent(String fieldName, Function<String, SchemaField> mappingFunction) {
        return fields.computeIfAbsent(fieldName, mappingFunction);
    }
}
