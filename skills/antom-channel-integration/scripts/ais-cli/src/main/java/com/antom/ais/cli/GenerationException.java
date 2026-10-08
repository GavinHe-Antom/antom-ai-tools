/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

/** Expected CLI failure carrying a stable process exit code. */
final class GenerationException extends RuntimeException {
    final int exitCode;

    GenerationException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }
}
