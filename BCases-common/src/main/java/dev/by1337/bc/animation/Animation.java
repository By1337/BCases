package dev.by1337.bc.animation;

import dev.by1337.bc.annotations.SyncOnly;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public interface Animation {
    void play();
    // принудительно завершаем анимацию
    void forceStop();
    @SyncOnly
    boolean onClick(int intId, Player clicker);
    @SyncOnly
    void onInteract(PlayerInteractEvent event);
    Player getPlayer();
}
