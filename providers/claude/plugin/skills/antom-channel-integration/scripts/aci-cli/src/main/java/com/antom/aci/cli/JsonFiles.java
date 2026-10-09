/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small strict JSON documents; polymorphic deserialization is never enabled. */
final class JsonFiles {
    static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    private JsonFiles() { }

    static AdapterSpec read(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > 1024 * 1024) {
            throw new GenerationException(2, "Configuration must be a regular JSON file up to 1 MiB");
        }
        return MAPPER.readValue(path.toFile(), AdapterSpec.class);
    }

    static void write(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }
}
