package com.example.roomatch.model;

import java.util.List;

public class GroupChat {
    private String id;
    private String groupId;
    private String apartmentId;
    private List<String> memberIds;
    private String ownerId;
    private long createdAt;

    // חובה: constructor ריק לפיירבייס
    public GroupChat() {
    }

    public GroupChat(String id, String groupId, String apartmentId, List<String> memberIds, String ownerId, long createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.apartmentId = apartmentId;
        this.memberIds = memberIds;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getApartmentId() {
        return apartmentId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setApartmentId(String apartmentId) {
        this.apartmentId = apartmentId;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
