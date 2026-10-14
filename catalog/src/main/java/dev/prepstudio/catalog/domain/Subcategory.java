package dev.prepstudio.catalog.domain;

import dev.prepstudio.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Leaf of the taxonomy; keys are filed under a subcategory. Part of the Platform aggregate. */
@Entity
@Table(name = "subcategory")
public class Subcategory implements Named {

    @Id
    private String id;
    private String name;

    protected Subcategory() {
    }

    Subcategory(String name) {
        this.id = Ids.newId();
        this.name = name;
    }

    public String getId() { return id; }

    @Override
    public String getName() { return name; }
}
