package me.squidxtv.groupingsystem.listener;

import com.j256.ormlite.dao.DaoManager;
import me.squidxtv.groupingsystem.scoreboard.GroupScoreboards;
import me.squidxtv.groupingsystem.storage.DatabaseStorage;
import me.squidxtv.groupingsystem.storage.dao.GroupMemberDao;
import me.squidxtv.groupingsystem.storage.model.GroupMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {

    private final DatabaseStorage storage;
    private final GroupScoreboards scoreboards;

    public PlayerJoinListener(DatabaseStorage storage, GroupScoreboards scoreboards) {
        this.storage = storage;
        this.scoreboards = scoreboards;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);
            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());

            if (byUUID.isEmpty()) {
                Component response = Component.translatable("player.join-is-not-in-group", NamedTextColor.YELLOW)
                        .arguments(Component.text(player.getName()));
                event.joinMessage(response);
                return;
            }

            GroupMember member = byUUID.getFirst();
            player.setScoreboard(scoreboards.get(member.getGroup()));

            Component response = Component.translatable("player.join-is-in-group", NamedTextColor.YELLOW)
                    .arguments(Component.text(member.getGroup().getPrefix()), Component.text(player.getName()));
            event.joinMessage(response);
        });
    }



}
