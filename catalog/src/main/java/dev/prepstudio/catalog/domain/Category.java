package dev.prepstudio.catalog.domain;

import dev.prepstudio.shared.domain.Ids;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Part of the Platform aggregate; change it only through {@link Platform}. */
@Entity
@Table(name = "category")
public class Category implements Named {

    @Id
    private String id;
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    @OrderColumn(name = "position")
    private List<Subcategory> subcategories = new ArrayList<>();

    protected Category() {
    }

    Category(String name) {
        this.id = Ids.newId();
        this.name = name;
    }

    Subcategory addSubcategory(String name) {
        String valid = Named.validName(name, "subcategory");
        Named.requireUnique(subcategories, valid, "subcategory");
        var subcategory = new Subcategory(valid);
        subcategories.add(subcategory);
        return subcategory;
    }

    public String getId() { return id; }

    @Override
    public String getName() { return name; }

    public List<Subcategory> getSubcategories() { return Collections.unmodifiableList(subcategories); }
}
