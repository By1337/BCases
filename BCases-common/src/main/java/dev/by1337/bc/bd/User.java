package dev.by1337.bc.bd;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.by1337.blib.configuration.serialization.DefaultCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public User(String lastKnownName, UUID uuid, Database database, Map<String, List<CaseKey>> keys) {
        this.lastKnownName = lastKnownName;
        this.uuid = uuid;
        this.database = database;
        this.keys = keys;
    }

    public User(String lastKnownName, UUID uuid, Map<String, List<CaseKey>> keys) {
        this.lastKnownName = lastKnownName;
        this.uuid = uuid;
        this.keys = keys;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public int keysCountOfType(String type) {
        var list = keys.get(type);
        if (list == null) return 0;
        return list.size();
    }

    public int keysCount(){
        return keys.size();
    }

    public List<CaseKey> getKeysOfType(String type) {
        return keys.get(type);
    }

    public void addKey(CaseKey key) {
        silentAddKey(key);
        database.addKeyIntoDb(key, lastKnownName, uuid);
    }

    public void silentAddKey(CaseKey key) {
        keys.computeIfAbsent(key.id(), k -> new ArrayList<>()).add(key);
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
        return res;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public UUID uuid() {
        return uuid;
    }
}
