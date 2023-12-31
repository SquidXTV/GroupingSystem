package me.squidxtv.groupingsystem;

import co.aikar.commands.PaperCommandManager;
import com.j256.ormlite.dao.DaoManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.squidxtv.groupingsystem.commands.GroupCommand;
import me.squidxtv.groupingsystem.listener.PlayerJoinListener;
import me.squidxtv.groupingsystem.listener.PlayerMessageEvent;
import me.squidxtv.groupingsystem.storage.DatabaseStorage;
import me.squidxtv.groupingsystem.storage.dao.GroupDao;
import me.squidxtv.groupingsystem.storage.dao.GroupMemberDao;
import me.squidxtv.groupingsystem.storage.model.Group;
import me.squidxtv.groupingsystem.storage.model.GroupMember;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GroupingSystem extends JavaPlugin {

    private DatabaseStorage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        storage = getStorage(config, getLogger());

        initLangFiles();

        initCommands();

        initEvents(config.getInt("ticks-per-group-update", 100));
    }

    private void initLangFiles() {
        Logger logger = getLogger();

        saveResource("lang/en_US.properties", false);

        Path languageFolder = getDataFolder().toPath().resolve("lang");
        if (!Files.exists(languageFolder)) {
            logger.log(Level.WARNING, "Initialization of language files has been stopped. The 'lang' folder does not exist.");
            return;
        }

        TranslationRegistry registry = TranslationRegistry.create(Key.key("groupingsystem", "main"));

        try (DirectoryStream<Path> directory = Files.newDirectoryStream(languageFolder, "*.properties")) {
            for (Path file : directory) {
                try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
                    ResourceBundle bundle = new PropertyResourceBundle(input);
                    Locale locale = Locale.forLanguageTag(getFileNameWithoutExtension(file.getFileName().toString()));
                    registry.registerAll(locale, bundle, false);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to initialize language files.", e);
        }

        GlobalTranslator.translator().addSource(registry);
    }

    private static String getFileNameWithoutExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        filename = filename.replace('_', '-');
        return dotIndex == -1 ? filename : filename.substring(0, dotIndex);
    }

    private void initCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.getCommandCompletions().registerAsyncCompletion("groups", context -> {
            List<Group> groups = new ArrayList<>();
            storage.useUncheckedConnectionSource(source -> {
                GroupDao groupDao = DaoManager.createDao(source, Group.class);
                groups.addAll(groupDao.queryBuilder().where().eq("open", "true").query());
            });
            return groups.stream().map(Group::getPrefix).toList();
        });
        commandManager.registerCommand(new GroupCommand(storage));
    }

    private void initEvents(int ticksPerGroupUpdate) {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(storage), this);
        pluginManager.registerEvents(new PlayerMessageEvent(storage), this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> storage.useUncheckedConnectionSource(source -> {
            GroupMemberDao memberDao = DaoManager.createDao(source, GroupMember.class);
            List<GroupMember> members = memberDao.queryBuilder().where().isNotNull("expiration").query();
            Timestamp now = new Timestamp(System.currentTimeMillis());

            for (GroupMember member : members) {
                Timestamp expirationDate = member.getExpirationDate();
                if (expirationDate.before(now)) {
                    member.setGroup(null);
                    member.setExpirationDate(null);
                    memberDao.update(member);
                }
            }
        }), 0, ticksPerGroupUpdate);
    }

    private static DatabaseStorage getStorage(FileConfiguration config, Logger logger) {
        HikariConfig databaseConfig = getDatabaseConfig(config);
        DataSource ds = new HikariDataSource(databaseConfig);
        return new DatabaseStorage(logger, ds, databaseConfig.getJdbcUrl());
    }

    private static HikariConfig getDatabaseConfig(FileConfiguration pluginConfig) {
        HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setJdbcUrl(pluginConfig.getString("db.url"));
        databaseConfig.setUsername(pluginConfig.getString("db.username"));
        databaseConfig.setPassword(pluginConfig.getString("db.password"));
        databaseConfig.setDriverClassName("org.postgresql.Driver");
        return databaseConfig;
    }

}
