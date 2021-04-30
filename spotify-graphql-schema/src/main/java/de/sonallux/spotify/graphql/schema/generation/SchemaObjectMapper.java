package de.sonallux.spotify.graphql.schema.generation;

import com.google.common.base.Strings;
import de.sonallux.spotify.core.SpotifyWebApiUtils;
import de.sonallux.spotify.core.model.SpotifyWebApi;
import de.sonallux.spotify.core.model.SpotifyWebApiEndpoint;
import de.sonallux.spotify.core.model.SpotifyWebApiObject;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@RequiredArgsConstructor
class SchemaObjectMapper {
    private static final List<String> PRIMITIVE_TYPES = List.of("Boolean", "Float", "Integer", "String", "Timestamp");

    private final SpotifyWebApi spotifyWebApi;
    private final Map<String, SchemaObject> schemaObjectMap = new TreeMap<>(); // SpotifyObject name -> SchemaObject
    private final Queue<String> queue = new LinkedList<>(); // SpotifyObject name

    Map<String, SchemaObject> generate() {
        var queryObject = EndpointMapping.QUERY_SCHEMA_OBJECT;
        schemaObjectMap.put("Query", queryObject);

        queryObject.getFields().forEach((name, field) -> addContainedTypes(field.getType()));
        iterate();

        for (var endpointMapping : EndpointMapping.MAPPINGS) {
            addToQueue(endpointMapping.getObjectName());
            iterate();

            var schemaObject = schemaObjectMap.get(endpointMapping.getObjectName());
            var endpoint = endpointMapping.getEndpoint(spotifyWebApi);
            var endpointResponseType = getResponseTypeFromEndpoint(endpoint, endpointMapping.getFieldExtraction());
            var field = schemaObject.computeFieldIfAbsent(endpointMapping.getFieldName(), name -> generateFieldFromEndpointMapping(name, endpointResponseType));
            if (field.getEndpoint() != null) {
                throw new IllegalStateException(schemaObject.getName() + "." + field.getName() + " has already an endpoint: " + field.getEndpoint().getId());
            }
            if (!endpointResponseType.equals(field.getType())) {
                throw new IllegalStateException("Endpoint " + endpoint.getId() + " has wrong response type for field " + field.getName() + " on object " + schemaObject.getName());
            }
            field.setEndpoint(endpoint);
            field.setFieldExtraction(endpointMapping.getFieldExtraction());
            field.setIdProvidedByParent(endpointMapping.isIdProvidedByParent());
        }

        spotifyWebApi.getCategoryList().stream()
            .flatMap(c -> c.getEndpointList().stream())
            .filter(e -> !"GET".equals(e.getHttpMethod()))
            .flatMap(e -> e.getResponseTypes().stream())
            .filter(r -> !"Void".equals(r.getType()))
            .forEach(r -> addToQueue(r.getType()));
        iterate();

        return schemaObjectMap;
    }

    private String getResponseTypeFromEndpoint(SpotifyWebApiEndpoint endpoint, String fieldExtraction) {
        if (endpoint.getResponseTypes().size() != 1) {
            throw new IllegalStateException("Endpoint " + endpoint.getId() + " has not one response type");
        }
        var type = endpoint.getResponseTypes().get(0).getType();
        if (fieldExtraction == null) {
            return type;
        }
        var spotifyObject = spotifyWebApi.getObject(type).orElseThrow();
        return spotifyObject.getProperties().stream()
            .filter(property -> property.getName().equals(fieldExtraction))
            .map(SpotifyWebApiObject.Property::getType)
            .findFirst().orElseThrow();
    }

    private SchemaField generateFieldFromEndpointMapping(String name, String type) {
        addContainedTypes(type);
        iterate();
        return new SchemaField(name, type);
    }

    private void iterate() {
        while (!queue.isEmpty()) {
            var currentObjectName = queue.poll();
            if (schemaObjectMap.containsKey(currentObjectName)) {
                continue;
            }

            var schemaObject = generateObject(currentObjectName);
            if (!schemaObject.getName().equals(currentObjectName)) {
                throw new RuntimeException("Generated object expected name " + currentObjectName + " but got " + schemaObject.getName());
            }
            this.schemaObjectMap.put(currentObjectName, schemaObject);
        }
    }

    private SchemaObject generateObject(String objectName) {
        var spotifyObject = spotifyWebApi.getObject(objectName).orElseGet(() -> new SpotifyWebApiObject(objectName));
        var fields = spotifyObject.getProperties().stream()
            .map(prop -> {
                addContainedTypes(prop.getType());
                return new SchemaField(prop.getName(), prop.getType(), prop.getDescription());
            })
            .collect(Collectors.toMap(SchemaField::getName, f -> f, (f1, f2) -> {throw new RuntimeException("Duplicated field name");}, TreeMap::new));

        String description = null;
        if (!Strings.isNullOrEmpty(spotifyObject.getLink())) {
            description = String.format("[%s](%s)", spotifyObject.getName(), spotifyObject.getLink());
        }
        return new SchemaObject(objectName, description, fields);
    }

    private void addContainedTypes(String type) {
        if ("Object".equals(type)) {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        if (PRIMITIVE_TYPES.contains(type)) {
            return;
        }

        Matcher matcher;
        if ((matcher = SpotifyWebApiUtils.ARRAY_TYPE_PATTERN.matcher(type)).matches()) {
            addContainedTypes(matcher.group(1));
        } else if (type.contains(" | ")) {
            Arrays.asList(type.split(" \\| ")).forEach(this::addContainedTypes);
        } else {
            addToQueue(type);
        }
    }

    private void addToQueue(String object) {
        if (!schemaObjectMap.containsKey(object)) {
            queue.add(object);
        }
    }
}
