/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].support;

/**
 * Expected adapter failure, converted to a standard failure response by the host framework.
 * When wrapping a lower-level exception, provide its cause to preserve the stack trace.
 * Messages should identify the failing step without including keys or complete sensitive payloads.
 */
public class ChannelIntegrationException extends RuntimeException {

    /**
     * Creates an adapter failure with a safe diagnostic message.
     *
     * @param message description of the failing step without sensitive data
     */
    public ChannelIntegrationException(String message) {
        super(message);
    }

    /**
     * Creates an adapter failure while preserving its underlying cause.
     *
     * @param message description of the failing step without sensitive data
     * @param cause underlying failure
     */
    public ChannelIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
