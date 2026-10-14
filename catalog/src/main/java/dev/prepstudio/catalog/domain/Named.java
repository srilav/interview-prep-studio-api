package dev.prepstudio.catalog.domain;

import dev.prepstudio.shared.domain.ConflictException;
import dev.prepstudio.shared.domain.DomainException;

import java.util.Collection;

/** A taxonomy node with a display name that must be unique among its siblings. */
interface Named {

    int MAX_NAME_LENGTH = 120;

    String getName();

    static String validName(String name, String label) {
        if (name == null || name.isBlank()) throw new DomainException("Enter a " + label + " name");
        String trimmed = name.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException("A " + label + " name can have at most " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    static void requireUnique(Collection<? extends Named> siblings, String name, String label) {
        if (siblings.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            throw new ConflictException("A " + label + " named \"" + name + "\" already exists here");
        }
    }
}
