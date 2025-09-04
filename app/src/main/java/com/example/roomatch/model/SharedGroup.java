package com.example.roomatch.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedGroup implements Serializable {
    private String id;
    private String creatorId;

    private String name;
    private List<String> memberIds;
    private Map<String, String> roles = new HashMap<>(); // userId -> role (e.g., "admin", "member")

    public SharedGroup() {
        this.roles = new HashMap<>();
    }

    public SharedGroup(String id, String name, List<String> memberIds, Map<String, String> roles) {
        this.id = id;
        this.name = name;
        this.memberIds = memberIds;
        this.roles = roles != null ? roles : new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public Map<String, String> getRoles() { return roles; }
    public void setRoles(Map<String, String> roles) { this.roles = roles; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
}