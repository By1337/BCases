package dev.by1337.bc.prize;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PrizeMap {
    public static final Codec<PrizeMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PrizeSelector.CODEC).fieldOf("prizes").forGetter(PrizeMap::prizes)
    ).apply(instance, PrizeMap::new));

    private final Map<String, PrizeSelector> prizes;

    public PrizeMap(Map<String, PrizeSelector> prizes) {
        this.prizes = prizes;

    }

    public boolean hasPrizeSet(String name) {
        return prizes.containsKey(name);
    }
    public Set<String> keySet(){
        return prizes.keySet();
    }

    public Map<String, PrizeSelector> prizes() {
        return prizes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrizeMap prizeMap = (PrizeMap) o;
        return Objects.equals(prizes, prizeMap.prizes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prizes);
    }

    @NotNull
    public PrizeSelector getPrizes(String id) {
        return Objects.requireNonNull(prizes.get(id), "Unknown prize " + id);
    }
}
