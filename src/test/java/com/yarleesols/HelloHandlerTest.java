package com.yarleesols;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HelloHandler Lambda function
 */
class HelloHandlerTest {

    private HelloHandler handler;
    private Context context;

    @BeforeEach
    void setUp() {
        handler = new HelloHandler();
        context = new TestContext();
    }

    @Test
    void testValidRequest() {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("body", "{\"name\":\"Prasad\"}");

        // When
        Map<String, Object> response = handler.handleRequest(input, context);

        // Then
        assertEquals(200, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("Hello Prasad"));
    }

    @Test
    void testMissingBody() {
        // Given
        Map<String, Object> input = new HashMap<>();

        // When
        Map<String, Object> response = handler.handleRequest(input, context);

        // Then
        assertEquals(400, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("Request body is required"));
    }

    @Test
    void testMissingName() {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("body", "{}");

        // When
        Map<String, Object> response = handler.handleRequest(input, context);

        // Then
        assertEquals(400, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("Name parameter is required"));
    }

    @Test
    void testInvalidJson() {
        // Given
        Map<String, Object> input = new HashMap<>();
        input.put("body", "invalid json");

        // When
        Map<String, Object> response = handler.handleRequest(input, context);

        // Then
        assertEquals(400, response.get("statusCode"));
        assertTrue(response.get("body").toString().contains("Invalid JSON format"));
    }

    /**
     * Mock Context for testing
     */
    private static class TestContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "test-request-id";
        }

        @Override
        public String getLogGroupName() {
            return "/aws/lambda/test-function";
        }

        @Override
        public String getLogStreamName() {
            return "test-stream";
        }

        @Override
        public String getFunctionName() {
            return "test-function";
        }

        @Override
        public String getFunctionVersion() {
            return "$LATEST";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:us-east-1:123456789012:function:test-function";
        }

        @Override
        public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 30000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 512;
        }

        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                    System.out.println(message);
                }

                @Override
                public void log(byte[] message) {
                    System.out.println(new String(message));
                }
            };
        }
    }
}

