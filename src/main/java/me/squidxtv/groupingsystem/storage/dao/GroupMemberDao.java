package me.squidxtv.groupingsystem.storage.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import me.squidxtv.groupingsystem.storage.model.Group;
import me.squidxtv.groupingsystem.storage.model.GroupMember;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class GroupMemberDao extends BaseDaoImpl<GroupMember, Long> {

    public GroupMemberDao(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, GroupMember.class);
    }

    public List<GroupMember> findByUUID(UUID uuid) throws SQLException {
        return queryForEq("uuid", uuid);
    }

    public List<GroupMember> findByGroup(Group group) throws SQLException {
        return queryForEq("group_id", group);
    }

}
