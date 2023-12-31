package me.squidxtv.groupingsystem.storage.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import me.squidxtv.groupingsystem.storage.model.Group;

import java.sql.SQLException;
import java.util.List;

public class GroupDao extends BaseDaoImpl<Group, Long> {

    public GroupDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Group.class);
    }

    public List<Group> findByName(String name) throws SQLException {
        return queryForEq("name", name);
    }

    public List<Group> findByPrefix(String prefix) throws SQLException {
        return queryForEq("prefix", prefix);
    }

}
