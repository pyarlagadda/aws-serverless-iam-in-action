package com.yarleesols;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.HashMap;

/**
 * AWS Lambda function handler for POST /hello endpoint
 * Demonstrates IAM-first serverless design with proper logging
 */
public class HelloHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Request ID: " + context.getAwsRequestId());
        logger.log("Function Name: " + context.getFunctionName());

        Map<String, Object> response = new HashMap<>();

        try {
            // Parse the request body from API Gateway proxy integration
            String body = (String) input.get("body");
            logger.log("Request body: " + body);

            if (body == null || body.isEmpty()) {
                return createErrorResponse(400, "Request body is required", logger);
            }

            // Parse JSON body
            Map<String, Object> requestBody = objectMapper.readValue(body, Map.class);
            String name = (String) requestBody.get("name");

            if (name == null || name.isEmpty()) {
                return createErrorResponse(400, "Name parameter is required", logger);
            }

            // Create success response
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Hello " + name);

            response.put("statusCode", 200);
            response.put("body", objectMapper.writeValueAsString(responseBody));
            response.put("headers", createHeaders());

            logger.log("Successfully processed request for name: " + name);

        } catch (JsonProcessingException e) {
            logger.log("Error parsing JSON: " + e.getMessage());
            return createErrorResponse(400, "Invalid JSON format", logger);
        } catch (Exception e) {
            logger.log("Unexpected error: " + e.getMessage());
            return createErrorResponse(500, "Internal server error", logger);
        }

        return response;
    }

    private Map<String, Object> createErrorResponse(int statusCode, String message, LambdaLogger logger) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);

        try {
            response.put("statusCode", statusCode);
            response.put("body", objectMapper.writeValueAsString(errorBody));
            response.put("headers", createHeaders());
        } catch (JsonProcessingException e) {
            logger.log("Error creating error response: " + e.getMessage());
        }

        return response;
    }

    private Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        return headers;
    }
}

