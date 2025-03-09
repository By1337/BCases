package dev.by1337.bc.bd;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.by1337.blib.configuration.serialization.DefaultCodecs;

import java.util.*;
import java.util.stream.IntStream;

public class User {
    public static final Codec<User> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(User::lastKnownName),
            DefaultCodecs.UUID.fieldOf("uuid").forGetter(User::uuid),
            Codec.unboundedMap(Codec.STRING, CaseKey.CODEC.listOf()).fieldOf("keys").forGetter(v -> v.keys)
    ).apply(instance, User::new));

    private final String lastKnownName;
    private final UUID uuid;
    private Database database;
    private final Map<String, List<CaseKey>> keys;
    private int keysCount;

    public User(String lastKnownName, UUID uuid, Database database, Map<String, List<CaseKey>> keys) {
        this.lastKnownName = lastKnownName;
        this.uuid = uuid;
        this.database = database;
        this.keys = keys;
        keysCount = keys.values().stream().flatMapToInt(list -> IntStream.of(list.size())).sum();
    }

    public User(String lastKnownName, UUID uuid, Map<String, List<CaseKey>> keys) {
        this.lastKnownName = lastKnownName;
        this.uuid = uuid;
        this.keys = keys;
        keysCount = keys.values().stream().flatMapToInt(list -> IntStream.of(list.size())).sum();
    }

    public void removeOutdated() {
        for (String s : keys.keySet().toArray(new String[0])) {
            var list = keys.get(s);
            for (CaseKey key : list.toArray(new CaseKey[0])) {
                if (key.removalDate() < System.currentTimeMillis()) {
                    removeKey(key);
                }
            }
            if (list.isEmpty()) {
                keys.remove(s);
            }
        }
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public int keysCountOfType(String type) {
        var list = keys.get(type);
        if (list == null) return 0;
        return list.size();
    }

    public int keysCount() {
        return keysCount;
    }

    public List<CaseKey> getKeysOfType(String type) {
        return Collections.unmodifiableList(keys.get(type));
    }

    public void addKey(CaseKey key) {
        silentAddKey(key);
        database.addKeyIntoDb(key, lastKnownName, uuid);
    }

    public void silentAddKey(CaseKey key) {
        keys.computeIfAbsent(key.id(), k -> new ArrayList<>()).add(key);
        keysCount++;
    }

    public boolean removeKey(CaseKey key) {
        if (silentRemoveKey(key)) {
            database.remKeyFromDb(key, lastKnownName, uuid);
            return true;
        }
        return false;
    }

    public boolean silentRemoveKey(CaseKey key) {
        var list = keys.get(key.id());
        if (list == null) return false;
        var res = list.remove(key);
        if (list.isEmpty()) {
            keys.remove(key.id());
        }
        if (res) keysCount--;
        return res;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public UUID uuid() {
        return uuid;
    }
}
