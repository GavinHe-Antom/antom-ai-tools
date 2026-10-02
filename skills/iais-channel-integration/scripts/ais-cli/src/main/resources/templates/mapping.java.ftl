                    /** Validate required fields before generating a channel request. */
                    @Override
                    public void validate([=capability.requestName] request) {
                        if (request == null) {
                            throw new IllegalArgumentException("[=capability.method] request is required");
                        }
                        // TODO: Validate the fields required by the confirmed channel protocol.
                        throw new UnsupportedOperationException("Implement [=capability.method] validation");
                    }

                    /** Map fields, amount units and optional values into the exact channel JSON. */
                    @Override
                    public JSONObject mapRequestBody([=capability.requestName] request) {
                        // TODO: Map fields; do not fabricate success or forward the standard DTO unchanged.
                        throw new UnsupportedOperationException("Implement [=capability.method] request mapping");
                    }

                    /** Supply only placeholder values for the path template selected by the platform. */
                    @Override
                    public Map<String, String> mapUrlParameters([=capability.requestName] request) {
                        // TODO: Return path placeholder values if the platform route uses placeholders.
                        return Collections.emptyMap();
                    }

                    /** Map verified responses, preserving unknown/processing results and institution codes. */
                    @Override
                    public [=capability.responseName] mapResponse([=capability.requestName] request, ChannelHttpResult response) {
                        // TODO: Map fields and apply the agreed platform result-code mapping contract.
                        throw new UnsupportedOperationException("Implement [=capability.method] response mapping");
                    }
