package dev.by1337.bc;

import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.annotations.SyncOnly;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeMap;
import dev.by1337.bc.world.WorldGetter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.chat.Placeholderable;
import org.by1337.blib.geom.Vec3i;
import org.by1337.bmenu.MenuLoader;

public interface CaseBlock extends Placeholderable {
    Vec3i pos();

    WorldGetter worldGetter();

    @SyncOnly
    void hideBlock();

    @SyncOnly
    void showBlock();

    @SyncOnly
    void hideHologram();

    @SyncOnly
    void showHologram();

    @SyncOnly
    void givePrize(Prize prize, Player player);

    Plugin plugin();

    Database getDatabase();

    PrizeMap prizeMap();

    default MenuLoader menuLoader() {
        return getBCasesApi().getMenuLoader();
    }

    AnimationContext animationContext();

    void playAnimation(Player player, String animation, String prizes);

    BCasesApi getBCasesApi();
}
