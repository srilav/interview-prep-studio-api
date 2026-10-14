package dev.prepstudio.shared.domain;

import java.util.UUID;

public final class Ids {
    private Ids() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
