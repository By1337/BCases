package dev.by1337.bc.prize;

import org.by1337.blib.configuration.YamlOps;
import org.by1337.blib.random.WeightedItem;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrizeMapTest {

    @Test
    public void run() {
        List<WeightedItem<Prize>> prizes = new ArrayList<>();
        prizes.add(createPrize());
        prizes.add(createPrize());
        prizes.add(createPrize());
        prizes.add(new PrizeSet(100, List.of(createPrize(), createPrize(), createPrize())));

        PrizeMap prizeMap = new PrizeMap(Map.of("default", new PrizeSelector(prizes)));


        var v = PrizeMap.CODEC.encodeStart(YamlOps.INSTANCE, prizeMap).getOrThrow();

        PrizeMap decoded = PrizeMap.CODEC.decode(YamlOps.INSTANCE, v).getOrThrow().getFirst();

        Assert.assertEquals(prizeMap, decoded);


    }

    private Prize createPrize() {
        return new Prize(
                100,
                "Test",
                true,
                List.of("cmd1", "cmd2"),
                "STONE"
        );
    }
}