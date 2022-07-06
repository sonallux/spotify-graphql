package de.sonallux.spotify.graphql.schema;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record SpotifyOpenApi(OpenAPI openAPI) {

    public static SpotifyOpenApi fromFile(Path openApiFile) throws IOException {
        var openApiAsString = Files.readString(openApiFile);
        var parseOptions = new ParseOptions();
        parseOptions.setResolve(false);
        parseOptions.setResolveFully(false);
        parseOptions.setResolveCombinators(false);
        var parseResult = new OpenAPIV3Parser().readContents(openApiAsString, null, parseOptions);
        if (!parseResult.getMessages().isEmpty()) {
            var errorString = "- " + String.join("\n- ", parseResult.getMessages());
            throw new IllegalArgumentException("Invalid OpenApi file:\n" + errorString);
        }

        return new SpotifyOpenApi(parseResult.getOpenAPI());
    }

    public Schema<?> getSchema(String name) {
        return openAPI.getComponents().getSchemas().get(name);
    }

    public Schema<?> getSchemaFromRef(String reference) {
        return getSchema(getSchemaName(reference));
    }

    public Parameter getParameter(String name) {
        return openAPI.getComponents().getParameters().get(name);
    }

    public Parameter getParameter(Parameter parameter) {
        if (parameter.get$ref() == null) {
            return parameter;
        }
        return getParameterFromRef(parameter.get$ref());
    }

    public Parameter getParameterFromRef(String reference) {
        return getParameter(getParameterName(reference));
    }

    public ApiResponse getResponse(String name) {
        return openAPI.getComponents().getResponses().get(name);
    }

    public ApiResponse getResponseFromRef(String reference) {
        return getResponse(getResponseName(reference));
    }

    public Operation getOperation(FieldMapping fieldMapping) {
        return openAPI.getPaths().get(fieldMapping.endpointPath()).getGet();
    }

    public Schema<?> getResponseSchema(FieldMapping fieldMapping) {
        var operation = getOperation(fieldMapping);
        var response = getResponseFromRef(operation.getResponses().get("200").get$ref());
        var responseSchema = response.getContent().get("application/json").getSchema();

        if (fieldMapping.fieldExtraction() == null) {
            return responseSchema;
        }

        return (Schema<?>) responseSchema.getProperties().get(fieldMapping.fieldExtraction());
    }

    public static String getSchemaName(String reference) {
        if (!reference.startsWith("#/components/schemas/")) {
            throw new IllegalArgumentException("Expected schema reference but got " + reference);
        }
        return reference.substring(21).replace("Simplified", "");
    }

    public static String getParameterName(String reference) {
        if (!reference.startsWith("#/components/parameters/")) {
            throw new IllegalArgumentException("Expected parameter reference but got " + reference);
        }
        return reference.substring(24)
            .replace("Simplified", "");// TODO needed here???
    }

    public static String getResponseName(String reference) {
        if (!reference.startsWith("#/components/responses/")) {
            throw new IllegalArgumentException("Expected response reference but got " + reference);
        }
        return reference.substring(23)
            .replace("Simplified", "");// TODO needed here???
    }
}
