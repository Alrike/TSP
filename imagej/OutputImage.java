package de.geomar.imagej;

public enum OutputImage {
    NONE("Unknown"),
    TEP("TEP"),
    CSP("CSP"),
    DEBRIS("Debris"),
    ALGAE("Algae");

    private final String title;

    OutputImage(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
