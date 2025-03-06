package dev.by1337.bc.addon;

import org.by1337.blib.configuration.YamlContext;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AddonDescription {
    private final String name;
    private final String mainClass;
    private final String version;
    private final String description;
    private final Set<String> authors;


    public AddonDescription(String name, String mainClass, String version, String description, Set<String> authors) {
        this.name = name;
        this.mainClass = mainClass;
        this.version = version;
        this.description = description;
        this.authors = authors;
    }

    public AddonDescription(YamlContext context) {
        this.name = Objects.requireNonNull(context.get("name").getAsString(), "missing 'name'!");

        this.mainClass = Objects.requireNonNull(context.get("main").getAsString(), "missing 'main'!");
        this.version = context.get("version").getAsString("1.0");
        this.description = context.get("description").getAsString("");
        authors = new HashSet<>();
        if (context.get("authors").getAsObject() != null) {
            authors.addAll(context.get("authors").getAsList(String.class));
        }
        String author = context.get("author").getAsString();
        if (author != null) {
            authors.add(author);
        }
    }

    public String name() {
        return name;
    }

    public String mainClass() {
        return mainClass;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public Set<String> authors() {
        return authors;
    }
}
