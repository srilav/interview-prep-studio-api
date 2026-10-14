package dev.prepstudio.shared.domain;

/** A business rule was violated. Mapped to gRPC INVALID_ARGUMENT. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
