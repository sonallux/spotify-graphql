package de.sonallux.spotify.graphql.schema;

public record FieldMapping(String openApiName, String fieldName, String endpointPath, String fieldExtraction, Category category) implements Mapping {
    public FieldMapping(String openApiName, String fieldName, String endpointPath, Category category) {
        this(openApiName, fieldName, endpointPath, null, category);
    }

    public boolean isQueryObject() {
        return "QueryObject".equals(openApiName);
    }

    public boolean isQueryMappingForCategory() {
        return isQueryObject() && category != Category.CORE;
    }
}
