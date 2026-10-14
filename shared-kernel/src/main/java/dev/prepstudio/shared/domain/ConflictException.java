package dev.prepstudio.shared.domain;

/** The change clashes with existing state, e.g. a duplicate name. Mapped to gRPC ALREADY_EXISTS. */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
