package dev.by1337.bc.yaml;

import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.configuration.YamlValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class CashedYamlContext {

    private final Map<String, Object> cashed = new HashMap<>();
    private final YamlContext source;

    public CashedYamlContext(YamlContext source) {
        this.source = source;
    }


    public <T> T get(String path, Function<YamlValue, T> mapper) {
        return Objects.requireNonNull(get(path, mapper, null), path + " not found");
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String path, Function<YamlValue, T> mapper, T def) {
        if (cashed.containsKey(path)) {
            return (T) cashed.get(path);
        }
        YamlValue val = source.get(path);
        if (val.isEmpty()) {
            cashed.put(path, def);
            return def;
        }
        T result = mapper.apply(val);
        cashed.put(path, result);
        return result;
    }

    public YamlContext source() {
        return source;
    }
}
