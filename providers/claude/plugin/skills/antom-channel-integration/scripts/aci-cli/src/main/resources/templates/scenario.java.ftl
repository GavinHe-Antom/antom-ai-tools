/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alipay.iacqintegrationhub.channel.sdk.api.error.ResultCodeService;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.model.base.Result;
import [=packageName].support.ChannelIntegrationException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Test-only fixture loading and platform boundary assertions; never used by the adapter at runtime. */
final class DeliveryScenario {
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList("success", "business-failure",
            "processing", "validation-failure", "security-failure", "http-failure", "mapping-failure", "identity", "protocol"));
    private final String method;
    private final JSONObject data;
    private final Path directory;

    private DeliveryScenario(String method, JSONObject data, Path directory) {
        this.method = method;
        this.data = data;
        this.directory = directory;
    }

    static Stream<DeliveryScenario> load(Class<?> owner, String method) throws Exception {
        URL resource = owner.getResource("/scenarios/" + method);
        assertNotNull(resource, "Missing protocol cases for " + method);
        assertEquals("file", resource.getProtocol(), "Run delivery tests from the Maven test resources directory");
        List<Path> paths;
        try (Stream<Path> entries = Files.list(Paths.get(resource.toURI()))) {
            paths = entries.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
        assertFalse(paths.isEmpty(), "At least one independent confirmed case is required for " + method);
        Set<String> ids = new HashSet<>();
        java.util.ArrayList<DeliveryScenario> cases = new java.util.ArrayList<>();
        for (Path path : paths) {
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JSONObject data = JSON.parseObject(source);
            assertNotNull(data, "Case must be a JSON object: " + path);
            DeliveryScenario scenario = new DeliveryScenario(method, data, path.getParent());
            scenario.validate(path);
            assertTrue(ids.add(scenario.text(data, "id")), "Duplicate case id: " + path);
            cases.add(scenario);
        }
        assertTrue(cases.stream().anyMatch(scenario -> scenario.data.containsKey("expectedResult")
                && Arrays.asList("success", "processing", "protocol").contains(scenario.data.getString("category"))),
                "At least one returning success/processing/protocol case is required for " + method);
        // Parse all fixtures before a business exception is captured: malformed data can never pass a failure case.
        return cases.stream();
    }

    private void validate(Path path) {
        String id = text(data, "id");
        assertTrue(id.matches("[a-z][a-z0-9-]{0,63}"), "Case id must be a lowercase identifier: " + path);
        assertEquals(id + ".json", path.getFileName().toString(), "Case id must match its filename");
        assertTrue(CATEGORIES.contains(text(data, "category")), "Unknown case category: " + path);
        assertTrue(bool(data, "confirmed"), "Confirm protocol expectations before delivery: " + path);
        assertTrue(data.containsKey("input"), label("input is required, including explicit null"));
        assertTrue(data.get("input") == null || data.get("input") instanceof JSONObject, label("input must be an object or null"));
        JSONObject context = context();
        bool(context, "present");
        text(context, "channelCode");
        body(context, "merchantId");
        body(context, "runtimeEnv");
        security();
        int calls = httpCalls();
        assertTrue(calls <= 1, label("Each baseline SPI makes at most one HTTP call; write a dedicated JUnit test for multi-call protocols"));
        if (calls > 0) {
            validateBoundary(http());
            JSONObject request = expectedRequest();
            text(request, "method");
            body(request, "contentType");
            for (String field : Arrays.asList("pathParameters", "headers", "queryParameters", "formParameters", "attributes")) {
                map(request, field);
            }
            body(request, "body");
            if (!http().containsKey("exception")) {
                httpResponse();
            }
        }
        JSONArray mapping = mappings();
        Set<List<String>> mappedArguments = new HashSet<>();
        for (Object entry : mapping) {
            assertTrue(entry instanceof JSONObject, label("resultCode entries must be objects"));
            JSONObject boundary = (JSONObject) entry;
            assertTrue(calls(boundary) > 0, label("Omit unused resultCode entries instead of adding zero-call stubs"));
            validateBoundary(boundary);
            JSONObject request = object(boundary, "request", JSONObject.class);
            text(request, "channelCode");
            body(request, "channelResultCode");
            body(request, "channelResultMsg");
            body(request, "api");
            if (request.containsKey("secondResultCode")) {
                body(request, "secondResultCode");
            }
            if (request.containsKey("thirdResultCode")) {
                assertTrue(request.containsKey("secondResultCode"), label("thirdResultCode requires secondResultCode"));
                body(request, "thirdResultCode");
            }
            List<String> arguments = new java.util.ArrayList<>();
            arguments.add(request.getString("channelCode"));
            arguments.add(request.getString("channelResultCode"));
            if (request.containsKey("secondResultCode")) {
                arguments.add(request.getString("secondResultCode"));
            }
            if (request.containsKey("thirdResultCode")) {
                arguments.add(request.getString("thirdResultCode"));
            }
            arguments.add(request.getString("channelResultMsg"));
            arguments.add(request.getString("api"));
            assertTrue(mappedArguments.add(arguments), label("Duplicate resultCode arguments; use one entry with an aggregated calls count"));
            if (!boundary.containsKey("exception")) {
                object(boundary, "result", Result.class);
            }
        }
        assertNotEquals(data.containsKey("expectedResult"), data.containsKey("expectedException"),
                label("Specify exactly one of expectedResult and expectedException"));
        if (data.containsKey("expectedException")) {
            assertFalse(Arrays.asList("success", "processing", "protocol").contains(data.getString("category")),
                    label("Returning-result categories cannot declare expectedException"));
            exception(data, "expectedException");
        } else {
            object(data, "expectedResult", JSONObject.class);
        }
    }

    void requireSecuritySteps(String... names) {
        Set<String> allowed = new HashSet<>(Arrays.asList(names));
        assertEquals(allowed, security().keySet(), label("Specify every selected security step, including calls:0; remove unselected steps"));
        for (String name : names) {
            JSONObject boundary = boundary(security(), name);
            if (calls(boundary) > 0) {
                validateBoundary(boundary);
                object(boundary, "request", JSONObject.class);
            }
        }
    }

    JSONObject boundary(JSONObject parent, String name) { return object(parent, name, JSONObject.class); }
    JSONObject context() { return object(data, "context", JSONObject.class); }
    JSONObject security() { return object(data, "security", JSONObject.class); }
    JSONObject http() { return object(data, "http", JSONObject.class); }
    JSONObject expectedRequest() { return object(data, "expectedRequest", JSONObject.class); }
    int httpCalls() { return calls(http()); }

    private JSONArray mappings() {
        assertTrue(data.get("resultCode") instanceof JSONArray, label("resultCode must be an array, including [] for no mapping"));
        return data.getJSONArray("resultCode");
    }

    <T> T input(Class<T> type) {
        if (data.get("input") == null) { return null; }
        return object(data, "input", type);
    }

    int calls(JSONObject boundary) {
        assertTrue(boundary.get("calls") instanceof Integer, label("calls must be an explicit nonnegative integer"));
        int count = boundary.getIntValue("calls");
        assertTrue(count >= 0, label("calls cannot be negative"));
        return count;
    }

    <T> T object(JSONObject parent, String field, Class<T> type) {
        assertTrue(parent.get(field) instanceof JSONObject, label(field + " must be an object"));
        T parsed = parent.getJSONObject(field).toJavaObject(type);
        assertNotNull(parsed, label(field));
        return parsed;
    }

    String text(JSONObject parent, String field) {
        assertTrue(parent.get(field) instanceof String, label(field + " must be a string"));
        return parent.getString(field);
    }

    boolean bool(JSONObject parent, String field) {
        assertTrue(parent.get(field) instanceof Boolean, label(field + " must be an explicit boolean"));
        return parent.getBooleanValue(field);
    }

    String body(JSONObject parent, String field) {
        if ("body".equals(field) && parent.containsKey("bodyFile")) {
            assertFalse(parent.containsKey("body"), label("Specify body or bodyFile, never both"));
            String name = text(parent, "bodyFile");
            assertTrue(name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*\\.txt"), label("bodyFile must name a sibling .txt fixture"));
            Path file = directory.resolve(name);
            assertFalse(Files.isSymbolicLink(file), label("Raw body fixtures must not be symbolic links"));
            try {
                return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            } catch (java.io.IOException failure) {
                throw new AssertionError(label("Cannot read bodyFile: " + name), failure);
            }
        }
        assertTrue(parent.containsKey(field), label(field + " must be explicit, including null"));
        assertTrue(parent.get(field) == null || parent.get(field) instanceof String, label(field + " must be exact text or null"));
        return parent.getString(field);
    }

    JSONObject map(JSONObject parent, String field) {
        assertTrue(parent.containsKey(field), label(field + " must be explicit; {} and null differ"));
        assertTrue(parent.get(field) == null || parent.get(field) instanceof JSONObject, label(field + " must be an object or null"));
        return parent.getJSONObject(field);
    }

    private void validateBoundary(JSONObject boundary) {
        if (boundary.containsKey("exception")) {
            assertFalse(boundary.containsKey("result") || boundary.containsKey("response"), label("A boundary exception cannot also have a result"));
            exception(boundary, "exception");
        } else {
            assertTrue(boundary.containsKey("result") || boundary.containsKey("response"), label("A called boundary needs a result or exception"));
        }
    }

    RuntimeException exception(JSONObject parent, String field) {
        JSONObject expected = object(parent, field, JSONObject.class);
        String type = text(expected, "type");
        String message = text(expected, "message");
        assertFalse(message.isEmpty(), label("Expected exception messages must be specific"));
        switch (type) {
            case "IllegalArgumentException": return new IllegalArgumentException(message);
            case "IllegalStateException": return new IllegalStateException(message);
            case "SecurityException": return new SecurityException(message);
            case "ChannelIntegrationException": return new ChannelIntegrationException(message);
            // UnsupportedOperationException is intentionally forbidden: unfinished hooks are not business failures.
            default: throw new AssertionError(label("Unsupported exception type: " + type + "; use a dedicated JUnit test for custom exceptions"));
        }
    }

    HttpResponse httpResponse() {
        JSONObject response = object(http(), "response", JSONObject.class);
        assertTrue(response.get("statusCode") instanceof Integer, label("response.statusCode must be an integer"));
        int status = response.getIntValue("statusCode");
        assertTrue(status == -1 || (status >= 100 && status <= 599),
                label("response.statusCode must be -1 (transport failure) or between 100 and 599"));
        JSONObject headers = map(response, "headers");
        Map<String, String> parsed = null;
        if (headers != null) {
            parsed = JSON.parseObject(headers.toJSONString(), new TypeReference<Map<String, String>>() { });
        }
        return HttpResponse.builder().statusCode(status).headers(parsed).body(body(response, "body")).build();
    }

    void assertOutcome(Object result, RuntimeException failure) {
        if (failure instanceof UnsupportedOperationException) {
            throw new AssertionError(label("Implement the real protocol hook; unfinished code cannot satisfy a failure case"), failure);
        }
        if (data.containsKey("expectedException")) {
            RuntimeException expected = exception(data, "expectedException");
            assertNotNull(failure, label("Expected a business exception but invocation returned"));
            assertEquals(expected.getClass(), failure.getClass(), label("exception.type"));
            assertEquals(expected.getMessage(), failure.getMessage(), label("exception.message"));
        } else {
            if (failure != null) {
                throw new AssertionError(label("Unexpected application exception"), failure);
            }
            assertNotNull(result, label("A selected SPI must not return null"));
            assertEquals(data.getJSONObject("expectedResult"), JSON.toJSON(result), label("result"));
        }
    }

    void stubResultCodes(ResultCodeService service) {
        for (Object entry : mappings()) {
            JSONObject boundary = (JSONObject) entry;
            JSONObject request = boundary.getJSONObject("request");
            Result result = null;
            if (!boundary.containsKey("exception")) {
                result = object(boundary, "result", Result.class);
            }
            org.mockito.stubbing.OngoingStubbing<Result> stub;
            if (request.containsKey("thirdResultCode")) {
                stub = when(service.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("secondResultCode"), request.getString("thirdResultCode"),
                        request.getString("channelResultMsg"), request.getString("api")));
            } else if (request.containsKey("secondResultCode")) {
                stub = when(service.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("secondResultCode"), request.getString("channelResultMsg"), request.getString("api")));
            } else {
                stub = when(service.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("channelResultMsg"), request.getString("api")));
            }
            if (boundary.containsKey("exception")) {
                stub.thenThrow(exception(boundary, "exception"));
            } else {
                stub.thenReturn(result);
            }
        }
    }

    void verifyResultCodes(ResultCodeService service) {
        for (Object entry : mappings()) {
            JSONObject boundary = (JSONObject) entry;
            JSONObject request = boundary.getJSONObject("request");
            ResultCodeService verified = verify(service, times(calls(boundary)));
            if (request.containsKey("thirdResultCode")) {
                verified.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("secondResultCode"), request.getString("thirdResultCode"),
                        request.getString("channelResultMsg"), request.getString("api"));
            } else if (request.containsKey("secondResultCode")) {
                verified.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("secondResultCode"), request.getString("channelResultMsg"), request.getString("api"));
            } else {
                verified.mapping(request.getString("channelCode"), request.getString("channelResultCode"),
                        request.getString("channelResultMsg"), request.getString("api"));
            }
        }
        verifyNoMoreInteractions(service);
    }

    String label(String field) { return method + "/" + data.getString("id") + ": " + field; }
    @Override public String toString() { return data.getString("id"); }
}
