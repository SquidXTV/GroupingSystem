package me.squidxtv.groupingsystem.listener;

import com.j256.ormlite.dao.DaoManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.squidxtv.groupingsystem.storage.DatabaseStorage;
import me.squidxtv.groupingsystem.storage.dao.GroupMemberDao;
import me.squidxtv.groupingsystem.storage.model.GroupMember;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class PlayerMessageEvent implements Listener {

    private final DatabaseStorage storage;

    public PlayerMessageEvent(DatabaseStorage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onMessage(AsyncChatEvent event) {
        Player player = event.getPlayer();
        storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);
            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());

            if (byUUID.isEmpty()) {
                event.renderer((s, displayName, message, viewer) -> Component.translatable("player.message-is-not-in-group")
                        .arguments(displayName, message));
                return;
            }

            String prefix = byUUID.getFirst().getGroup().getPrefix();

            event.renderer((s, displayName, message, viewer) -> Component.translatable("player.message-is-in-group")
                    .arguments(Component.text(prefix), displayName, message));
        });
    }

}
