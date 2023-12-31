package me.squidxtv.groupingsystem.storage.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.squidxtv.groupingsystem.storage.dao.GroupDao;

@DatabaseTable(tableName = "group", daoClass = GroupDao.class)
public class Group {

    @DatabaseField(generatedId = true)
    private long id;

    @DatabaseField(canBeNull = false, width = 50)
    private String name;

    @DatabaseField(canBeNull = false, unique = true, width = 5)
    private String prefix;

    @DatabaseField(defaultValue = "true")
    private boolean open;

    public Group() {

    }

    public Group(long id, String name, String prefix, boolean open) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.open = open;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

}
