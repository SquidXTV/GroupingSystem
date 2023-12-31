package me.squidxtv.groupingsystem.storage.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.squidxtv.groupingsystem.storage.dao.GroupMemberDao;

import java.sql.Timestamp;
import java.util.UUID;

@DatabaseTable(tableName = "member", daoClass = GroupMemberDao.class)
public class GroupMember {

    @DatabaseField(id = true, canBeNull = false, unique = true)
    private UUID uuid;

    @DatabaseField(foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    private Group group;

    @DatabaseField()
    private Timestamp expiration;

    @DatabaseField(defaultValue = "MEMBER")
    private Role role;

    public enum Role {
        MEMBER,
        ADMIN
    }

    public GroupMember() {

    }

    public GroupMember(UUID uuid, Group group, Timestamp expiration, Role role) {
        this.uuid = uuid;
        this.group = group;
        this.expiration = expiration;
        this.role = role;
    }


    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Timestamp getExpirationDate() {
        return expiration;
    }

    public void setExpirationDate(Timestamp expiration) {
        this.expiration = expiration;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

}
