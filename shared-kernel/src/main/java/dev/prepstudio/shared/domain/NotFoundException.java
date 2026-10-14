package dev.prepstudio.shared.domain;

/** Mapped to gRPC NOT_FOUND. */
public class NotFoundException extends DomainException {
    public NotFoundException(String what, String id) {
        super(what + " " + id + " does not exist");
    }
}
