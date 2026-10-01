/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].customize.api;

import com.alibaba.fastjson.JSONObject;
import [=capability.requestType];
import [=capability.responseType];
import [=packageName].extension.ChannelApiExtension;
import [=packageName].model.ChannelHttpResult;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Implement the institution protocol for [=capability.method]; never select a domain here. */
@Component
public class [=capability.title]Mapping implements ChannelApiExtension<[=capability.requestName], [=capability.responseName]> {
    /** Validate required fields before generating a channel request. */
    @Override
    public void validate([=capability.requestName] request) {
        if (request == null) {
            throw new IllegalArgumentException("[=capability.method] request is required");
        }
        // TODO: Validate the fields required by the confirmed channel protocol.
        throw new UnsupportedOperationException("Implement [=capability.method] validation");
    }

    /** Map standard fields, amount units and optional values into the exact channel JSON. */
    @Override
    public JSONObject mapRequestBody([=capability.requestName] request) {
        // TODO: Map fields; do not return a fabricated success or forward the standard DTO unchanged.
        throw new UnsupportedOperationException("Implement [=capability.method] request mapping");
    }

    /** Supply only placeholder values for the path template selected by the platform. */
    @Override
    public Map<String, String> mapUrlParameters([=capability.requestName] request) {
        // TODO: Return path placeholder values if the platform route uses placeholders.
        return Collections.emptyMap();
    }

    /** Map the verified response, preserving unknown/processing results and institution error codes. */
    @Override
    public [=capability.responseName] mapResponse([=capability.requestName] request, ChannelHttpResult response) {
        // TODO: Map fields and apply the agreed platform result-code mapping contract.
        throw new UnsupportedOperationException("Implement [=capability.method] response mapping");
    }
}
