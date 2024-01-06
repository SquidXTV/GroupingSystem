package me.squidxtv.groupingsystem.storage;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.squidxtv.groupingsystem.storage.model.Group;
import me.squidxtv.groupingsystem.storage.model.GroupMember;
import me.squidxtv.groupingsystem.util.UncheckedConsumer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseStorage {

    private final Logger logger;

    private final DataSource source;
    private final String jdbcUrl;

    public DatabaseStorage(Logger logger, DataSource source, String jdbcUrl) {
        this.logger = logger;
        this.source = source;
        this.jdbcUrl = jdbcUrl;

        // throws exception: https://github.com/j256/ormlite-core/issues/20
        useConnectionSource(s -> {
            try {
                TableUtils.createTableIfNotExists(s, Group.class);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Group table already exists.");
            }

            try {
                TableUtils.createTableIfNotExists(s, GroupMember.class);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Member table already exists.");
            }
        });
    }

    public void useConnectionSource(Consumer<ConnectionSource> consumer) {
        try (ConnectionSource connectionSource = new DataSourceConnectionSource(source, jdbcUrl)) {
            consumer.accept(connectionSource);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQL Exception occurred while creating ConnectionSource.", e);
        } catch (Exception e) {
            logger.log(Level.WARNING, "SQL Exception occurred while closing ConnectionSource.", e);
        }
    }

    public void useUncheckedConnectionSource(UncheckedConsumer<ConnectionSource> consumer) {
        useConnectionSource(connectionSource -> {
            try {
                consumer.accept(connectionSource);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception occurred while using unchecked connection source.", e);
            }
        });
    }

    public void useStatement(Consumer<Statement> consumer) {
        try (Connection connection = source.getConnection();
             Statement statement = connection.createStatement()) {
            consumer.accept(statement);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQL Exception occurred while using Statement.", e);
        }
    }

    public void usePreparedStatement(Consumer<PreparedStatement> consumer, String query) {
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            consumer.accept(statement);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQL Exception occurred while using PreparedStatement.", e);
        }
    }

    public void useUncheckedStatement(UncheckedConsumer<Statement> consumer) {
        useStatement(statement -> {
            try {
                consumer.accept(statement);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception occurred while using unchecked Statement.", e);
            }
        });
    }

    public void useUncheckedPreparedStatement(UncheckedConsumer<PreparedStatement> consumer, String query) {
        usePreparedStatement(preparedStatement -> {
            try {
                consumer.accept(preparedStatement);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception occurred while using unchecked PreparedStatement.", e);
            }
        }, query);
    }

    public DataSource getSource() {
        return source;
    }

}
