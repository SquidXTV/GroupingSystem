package me.squidxtv.groupingsystem.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.j256.ormlite.dao.DaoManager;
import me.squidxtv.groupingsystem.scoreboard.GroupScoreboards;
import me.squidxtv.groupingsystem.storage.DatabaseStorage;
import me.squidxtv.groupingsystem.storage.dao.GroupDao;
import me.squidxtv.groupingsystem.storage.dao.GroupMemberDao;
import me.squidxtv.groupingsystem.storage.model.Group;
import me.squidxtv.groupingsystem.storage.model.GroupMember;
import me.squidxtv.groupingsystem.util.Timespan;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

@CommandAlias("group|g")
@SuppressWarnings("unused")
public class GroupCommand extends BaseCommand {

    private final DatabaseStorage storage;
    private final GroupScoreboards scoreboards;

    public GroupCommand(DatabaseStorage storage, GroupScoreboards scoreboards) {
        this.storage = storage;
        this.scoreboards = scoreboards;
    }

    @Default
    @Subcommand("info")
    @Syntax("[group]")
    @CommandCompletion("@groups")
    @Description("Displays information about a group. If no group is specified, shows info about the player's current group.")
    public void info(Player player, @Optional String prefix) {
        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            Group group;
            if (prefix == null) {
                List<GroupMember> members = memberDao.findByUUID(player.getUniqueId());

                if (members.isEmpty()) {
                    Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                    player.sendMessage(fail);
                    return;
                }
                group = members.getFirst().getGroup();
            } else {
                List<Group> groups = groupDao.findByPrefix(prefix);

                if (groups.isEmpty()) {
                    Component fail = Component.translatable("group.group-not-found-with-prefix", NamedTextColor.RED)
                            .arguments(Component.text(prefix));
                    player.sendMessage(fail);
                    return;
                }
                group = groups.getFirst();
            }

            int count = memberDao.findByGroup(group).size();

            Component success = Component.translatable("info.success")
                    .arguments(Component.text(group.getName()), Component.text(group.getPrefix()), Component.text(count))
                    .decorate(TextDecoration.BOLD);
            player.sendMessage(success);
        });
    }

    @Subcommand("leave")
    @Description("Leave your current group")
    public void leave(Player player) {
        storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);
            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());

            if (byUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            GroupMember member = byUUID.getFirst();
            Group group = member.getGroup();
            memberDao.delete(member);

            Component success = Component.translatable("leave.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.getName()), Component.text(group.getPrefix()));
            player.sendMessage(success);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        });
    }

    @Subcommand("create")
    @Syntax("<name> <prefix> [open]")
    @Description("Creates a new group with the specified name and prefix. The 'open' parameter determines whether the group is open or not. Defaults to true.")
    public void create(Player player, String name, String prefix, @Optional @Default("true") boolean open) {
        if (name.length() <= 5 || name.length() > 50) {
            Component fail = Component.translatable("group.invalid-name-length", NamedTextColor.RED)
                    .arguments(Component.text(name));
            player.sendMessage(fail);
            return;
        }

        if (prefix.length() < 3 || prefix.length() > 5) {
            Component fail = Component.translatable("group.invalid-prefix-length", NamedTextColor.RED)
                    .arguments(Component.text(prefix));
            player.sendMessage(fail);
            return;
        }

        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            if (!memberDao.findByUUID(player.getUniqueId()).isEmpty()) {
                Component fail = Component.translatable("member.leave-group");
                player.sendMessage(fail);
                return;
            }

            if (!groupDao.findByPrefix(prefix).isEmpty()) {
                Component fail = Component.translatable("group.prefix-already-exists", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            Group group = new Group();
            group.setName(name);
            group.setPrefix(prefix);
            group.setOpen(open);

            groupDao.create(group);

            GroupMember member = new GroupMember();
            member.setUuid(player.getUniqueId());
            member.setGroup(group);
            member.setRole(GroupMember.Role.ADMIN);

            memberDao.create(member);

            player.setScoreboard(scoreboards.get(group));

            Component success = Component.translatable("create.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.getName()), Component.text(group.getPrefix()));
            player.sendMessage(success);
        });
    }

    @Subcommand("join")
    @CommandCompletion("@groups")
    @Syntax("<name/prefix>")
    @Description("Joins a group by either its name or prefix. If the group is not open, the player cannot join.")
    public void join(Player player, String query) {
        if (query.length() < 3 || query.length() > 50) {
            Component fail = Component.translatable("group.invalid-name-or-prefix-length", NamedTextColor.RED)
                    .arguments(Component.text(query));
            player.sendMessage(fail);
            return;
        }

        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            if (!memberDao.findByUUID(player.getUniqueId()).isEmpty()) {
                Component fail = Component.translatable("member.leave-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            List<Group> groups;
            if (query.length() > 5) {
                groups = groupDao.findByName(query);
            } else {
                groups = groupDao.findByPrefix(query);
            }

            if (groups.isEmpty()) {
                Component fail = Component.translatable("group.group-not-found-with-name-or-prefix", NamedTextColor.RED)
                        .arguments(Component.text(query));
                player.sendMessage(fail);
                return;
            }

            Group group = groups.getFirst();
            if (!group.isOpen()) {
                Component fail = Component.translatable("group.group-closed", NamedTextColor.RED)
                        .arguments(Component.text(group.getName()), Component.text(group.getPrefix()));
                player.sendMessage(fail);
                return;
            }

            GroupMember member = new GroupMember();
            member.setUuid(player.getUniqueId());
            member.setGroup(group);
            member.setRole(GroupMember.Role.MEMBER);

            memberDao.create(member);

            player.setScoreboard(scoreboards.get(group));

            Component success = Component.translatable("join.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.getName()), Component.text(group.getPrefix()));
            player.sendMessage(success);
        });
    }

    @Subcommand("invite")
    @CommandCompletion("@players")
    @Syntax("<player> [expiration date]")
    @Description("Invites a player to join your group. Optionally, you can specify an expiration date for the player.")
    public void invite(Player player, Player target, @Optional String expirationDate) {
        storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());
            if (byUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission

            Group group = byUUID.getFirst().getGroup();

            Timespan timespan = Timespan.parseTimespan(player, expirationDate);
            Component request = Component.translatable("invite.request")
                    .arguments(Component.text(group.getName()), Component.text(group.getPrefix()))
                    .style(builder -> builder.clickEvent(getAcceptEvent(group.getId(), timespan)).color(NamedTextColor.YELLOW));
            target.sendMessage(request);

            Component success = Component.translatable("invite.success", NamedTextColor.GREEN)
                    .arguments(Component.text(target.getName()));
            player.sendMessage(success);
        });
    }

    private ClickEvent getAcceptEvent(long groupId, Timespan timespan) {
        return ClickEvent.callback(audience -> {
            if (!(audience instanceof Player target)) {
                return;
            }

            storage.useUncheckedConnectionSource(source -> {
                GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);
                GroupDao groupDao = DaoManager.createDao(source, Group.class);

                if (!memberDao.findByUUID(target.getUniqueId()).isEmpty()) {
                    Component fail = Component.translatable("member.leave-group", NamedTextColor.RED);
                    target.sendMessage(fail);
                    return;
                }

                if (groupDao.idExists(groupId)) {
                    Component fail = Component.translatable("group.group-not-found-with-id", NamedTextColor.RED)
                            .arguments(Component.text(groupId));
                    target.sendMessage(fail);
                    return;
                }

                Group group = groupDao.queryForId(groupId);

                GroupMember member = new GroupMember(target.getUniqueId(), group, timespan.toFutureTimestamp(), GroupMember.Role.MEMBER);
                memberDao.create(member);

                target.setScoreboard(scoreboards.get(group));

                Component success = Component.translatable("accept.success", NamedTextColor.GREEN)
                        .arguments(Component.text(group.getName()), Component.text(group.getPrefix()));
                target.sendMessage(success);
            });
        });
    }

    @Subcommand("kick")
    @Syntax("<player>")
    @Description("Removes a player from your group. You must be part of a group to use this command. The player must be part of your group.")
    public void kick(Player player, Player target) {
        storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> userByUUID = memberDao.findByUUID(player.getUniqueId());
            if (userByUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            List<GroupMember> targetByUUID = memberDao.findByUUID(target.getUniqueId());
            if (targetByUUID.isEmpty()) {
                Component fail = Component.translatable("kick.target-not-in-group", NamedTextColor.RED)
                        .arguments(Component.text(target.getName()));
                player.sendMessage(fail);
                return;
            }

            GroupMember commandUser = userByUUID.getFirst();
            GroupMember targetMember = targetByUUID.getFirst();

            if (commandUser.getGroup().getId() != targetMember.getGroup().getId()) {
                Component fail = Component.translatable("kick.target-not-in-group", NamedTextColor.RED)
                        .arguments(Component.text(target.getName()));
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission
            if (commandUser.getRole() != GroupMember.Role.ADMIN) {
                Component fail = Component.translatable("group.permission-missing", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            memberDao.delete(targetMember);

            if (target.isOnline()) {
                Component alert = Component.translatable("kick.alert-target", NamedTextColor.RED)
                        .arguments(Component.text(player.getName()));

                target.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                target.sendMessage(alert);
            }

            Component success = Component.translatable("kick.success", NamedTextColor.GREEN)
                    .arguments(Component.text(target.getName()));
            player.sendMessage(success);
        });
    }

    @Subcommand("set-visibility")
    @Syntax("<open>")
    @Description("Changes the visibility of your group. You must be an admin to use this command.")
    public void setVisibility(Player player, boolean open) {
        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> userByUUID = memberDao.findByUUID(player.getUniqueId());
            if (userByUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission
            GroupMember commandUser = userByUUID.getFirst();
            if (commandUser.getRole() != GroupMember.Role.ADMIN) {
                Component fail = Component.translatable("group.permission-missing", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            Group group = commandUser.getGroup();
            group.setOpen(open);
            groupDao.update(group);

            Component success = Component.translatable("set-visibility.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.isOpen() ? "open" : "closed"));
            player.sendMessage(success);
        });
    }

    @Subcommand("set-name")
    @Syntax("<name>")
    @Description("Changes the name of your group. You must be an admin of the group to use this command.")
    public void setName(Player player, String name) {
        if (name.length() <= 5 || name.length() > 50) {
            Component fail = Component.translatable("group.invalid-name-length", NamedTextColor.RED)
                    .arguments(Component.text(name));
            player.sendMessage(fail);
            return;
        }

        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());
            if (byUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission
            GroupMember commandUser = byUUID.getFirst();
            if (commandUser.getRole() != GroupMember.Role.ADMIN) {
                Component fail = Component.translatable("group.permission-missing", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            Group group = commandUser.getGroup();
            group.setName(name);
            groupDao.update(group);

            scoreboards.update(group);

            Component success = Component.translatable("set-name.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.getName()));
            player.sendMessage(success);
        });
    }

    @Subcommand("set-prefix")
    @Syntax("<prefix>")
    @Description("Changes the prefix of your group. You must be an admin of the group to use this command.")
    public void setPrefix(Player player, String prefix) {
        if (prefix.length() < 3 || prefix.length() > 5) {
            Component fail = Component.translatable("group.invalid-prefix-length", NamedTextColor.RED)
                    .arguments(Component.text(prefix));
            player.sendMessage(fail);
            return;
        }

        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> userByUUID = memberDao.findByUUID(player.getUniqueId());
            if (userByUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission
            GroupMember commandUser = userByUUID.getFirst();
            if (commandUser.getRole() != GroupMember.Role.ADMIN) {
                Component fail = Component.translatable("group.permission-missing", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            Group group = commandUser.getGroup();
            group.setPrefix(prefix);
            groupDao.update(group);

            scoreboards.update(group);

            Component success = Component.translatable("set-prefix.success", NamedTextColor.GREEN)
                    .arguments(Component.text(group.getPrefix()));
            player.sendMessage(success);
        });
    }

    @Subcommand("delete")
    @Description("Deletes your group. You must be an admin of the group to use this command.")
    public void delete(Player player) {
        storage.useUncheckedConnectionSource(source -> {
            GroupDao groupDao = DaoManager.createDao(source, Group.class);
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);

            List<GroupMember> byUUID = memberDao.findByUUID(player.getUniqueId());
            if (byUUID.isEmpty()) {
                Component fail = Component.translatable("member.not-in-group", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            // ToDo: check bukkit permission
            GroupMember commandUser = byUUID.getFirst();
            if (commandUser.getRole() != GroupMember.Role.ADMIN) {
                Component fail = Component.translatable("group.permission-missing", NamedTextColor.RED);
                player.sendMessage(fail);
                return;
            }

            Group group = commandUser.getGroup();
            List<GroupMember> byGroup = memberDao.findByGroup(group);
            byGroup.stream().map(GroupMember::getUuid)
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .filter(Player::isOnline)
                    .forEach(member -> {
                        Component deleted = Component.translatable("group.deleted");
                        member.sendMessage(deleted);
                        member.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    });
            memberDao.delete(byGroup);

            scoreboards.remove(group);
            groupDao.delete(group);

            Component success = Component.translatable("delete.success", NamedTextColor.GREEN);
            player.sendMessage(success);
        });
    }

}
