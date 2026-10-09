                    /** Validate authenticated plaintext and institution-required fields. */
                    @Override
                    public void validate(BaseChannelRequest request, String plainBody) {
                        if (request == null) {
                            throw new IllegalArgumentException("[=capability.method] request is required");
                        }
                        // TODO: Validate the confirmed notification fields after security processing.
                        throw new UnsupportedOperationException("Implement [=capability.method] validation");
                    }

                    /** Return the standard notification; the platform owns iPay forwarding and ACK. */
                    @Override
                    public [=capability.responseName] map(BaseChannelRequest request, String plainBody) {
                        // TODO: Map authenticated plaintext using the confirmed notification contract.
                        throw new UnsupportedOperationException("Implement [=capability.method] notification mapping");
                    }
