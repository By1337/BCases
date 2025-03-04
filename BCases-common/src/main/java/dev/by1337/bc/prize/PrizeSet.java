package dev.by1337.bc.prize;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.by1337.blib.random.WeightedItem;
import org.by1337.blib.random.WeightedItemSelector;

import java.util.List;
import java.util.Objects;

public class PrizeSet implements WeightedItem<Prize> {
    public static final Codec<PrizeSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("chance").forGetter(PrizeSet::chance),
            Prize.CODEC.listOf().fieldOf("items").forGetter(PrizeSet::prizes)
    ).apply(instance, PrizeSet::new));

    private double chance;
    private List<Prize> prizes;
    private WeightedItemSelector<Prize> selector;


    public PrizeSet(double chance, List<Prize> prizes) {
        if (prizes.isEmpty()){
            throw new IllegalArgumentException("Prizes cannot be empty");
        }
        this.chance = chance;
        this.prizes = prizes;
        selector = new WeightedItemSelector<>(prizes);
    }

    public double chance() {
        return chance;
    }

    public List<Prize> prizes() {
        return prizes;
    }

    @Override
    public Prize value() {
        return selector.getRandomItem();
    }

    @Override
    public double weight() {
        return chance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrizeSet set = (PrizeSet) o;
        return Double.compare(chance, set.chance) == 0 && Objects.equals(prizes, set.prizes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chance, prizes);
    }
}
