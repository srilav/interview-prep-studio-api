package dev.prepstudio.ai;

/** The language model could not produce a response. Mapped to gRPC UNAVAILABLE. */
public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
