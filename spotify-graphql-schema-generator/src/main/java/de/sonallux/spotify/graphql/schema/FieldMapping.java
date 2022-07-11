package de.sonallux.spotify.graphql.schema;

import io.swagger.v3.oas.models.parameters.Parameter;

public record FieldMapping(String openApiName, String fieldName, String endpointPath, String fieldExtraction, boolean skipPathParameters, Category category) implements Mapping {
    public FieldMapping(String openApiName, String fieldName, String endpointPath, Category category) {
        this(openApiName, fieldName, endpointPath, null, true, category);
    }

    public FieldMapping(String openApiName, String fieldName, String endpointPath, String fieldExtraction, Category category) {
        this(openApiName, fieldName, endpointPath, fieldExtraction, true, category);
    }

    public boolean filterParameter(Parameter parameter) {
        if (skipPathParameters && "path".equals(parameter.getIn())) {
            return false;
        }
        return true;
    }

    public boolean isQueryObject() {
        return "QueryObject".equals(openApiName);
    }

    public boolean isQueryMappingForCategory() {
        return isQueryObject() && category != Category.CORE;
    }
}
