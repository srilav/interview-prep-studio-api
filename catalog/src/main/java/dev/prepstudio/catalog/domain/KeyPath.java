package dev.prepstudio.catalog.domain;

/** Where a key sits in the taxonomy. */
public record KeyPath(String platformId, String platformName,
                      String categoryId, String categoryName,
                      String subcategoryId, String subcategoryName) {

    public String display() {
        return platformName + " › " + categoryName + " › " + subcategoryName;
    }
}
