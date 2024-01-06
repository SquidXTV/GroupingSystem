package me.squidxtv.groupingsystem.scoreboard;

import me.squidxtv.groupingsystem.storage.model.Group;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class GroupScoreboards {

    private final Map<Long, Scoreboard> scoreboards = new HashMap<>();

    public Scoreboard get(Group group) {
        return scoreboards.computeIfAbsent(group.getId(), id -> {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            update(scoreboard, group);
            return scoreboard;
        });
    }

    public void update(Group group) {
        Scoreboard board = get(group);
        update(board, group);
    }

    private void update(Scoreboard scoreboard, Group group) {
        Objective sidebar = scoreboard.getObjective("sidebar");
        if (sidebar == null) {
            sidebar = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, Component.text("Group", Style.style(TextDecoration.BOLD)));
        }

        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        sidebar.getScore("name").customName(Component.text(group.getName()));
        sidebar.getScore("prefix").customName(Component.text(group.getPrefix()));
    }

    public void remove(long id) {
        scoreboards.remove(id);
    }

    public void remove(Group group) {
        remove(group.getId());
    }

}
