package dev.by1337.bc.prize;

import blib.com.mojang.datafixers.util.Pair;
import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.DataResult;
import blib.com.mojang.serialization.DynamicOps;
import org.by1337.blib.random.WeightedItem;
import org.by1337.blib.random.WeightedItemSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PrizeSelector {
    public static final Codec<PrizeSelector> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PrizeSelector, T>> decode(DynamicOps<T> ops, T t) {
            return ops.getStream(t).flatMap(stream -> {
                List<WeightedItem<Prize>> prizes = new ArrayList<>();
                for (T t1 : stream.toList()) {
                    var try1 = PrizeSet.CODEC.decode(ops, t1);
                    if (try1.isError()) {
                        var try2 = Prize.CODEC.decode(ops, t1);
                        if (try2.isError()) return DataResult.error(try2.error().get().messageSupplier());
                        prizes.add(try2.getOrThrow().getFirst());
                    } else {
                        prizes.add(try1.getOrThrow().getFirst());
                    }
                }
                return DataResult.success(Pair.of(new PrizeSelector(prizes), t));
            });
        }

        @Override
        public <T> DataResult<T> encode(PrizeSelector selector, DynamicOps<T> ops, T t) {
            List<T> list = new ArrayList<>(selector.prizes.size());
            for (WeightedItem<Prize> prize : selector.prizes) {
                if (prize instanceof Prize p) {
                    var res = Prize.CODEC.encode(p, ops, t);
                    if (res.isError()) return res;
                    list.add(res.getOrThrow());
                } else if (prize instanceof PrizeSet set) {
                    var res = PrizeSet.CODEC.encode(set, ops, t);
                    if (res.isError()) return res;
                    list.add(res.getOrThrow());
                }
            }
            return DataResult.success(ops.createList(list.stream()));
        }
    };
    private final List<? extends WeightedItem<Prize>> prizes;
    private final WeightedItemSelector<Prize> selector;

    public PrizeSelector(List<? extends WeightedItem<Prize>> prizes) {
        this.prizes = prizes;
        selector = new WeightedItemSelector<>(prizes);
    }

    public Prize getRandomPrize() {
        return selector.getRandomItem();
    }

    public Prize getRandomPrize(double exponent) {
        return selector.getRandomItem(exponent);
    }

    public List<? extends WeightedItem<Prize>> prizes() {
        return prizes;
    }

    public WeightedItemSelector<Prize> selector() {
        return selector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrizeSelector that = (PrizeSelector) o;
        return Objects.equals(prizes, that.prizes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prizes);
    }
}
