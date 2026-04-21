package com.whispernetwork.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "agents",
        indexes = {@Index(name = "idx_agents_owner", columnList = "owner_id")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_agents_owner_agentid",
                    columnNames = {"owner_id", "agent_id"})
        })
public class AgentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", length = 64, nullable = false)
    private String ownerId;

    @Column(name = "agent_id", length = 64, nullable = false)
    private String agentId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "role", length = 32)
    private String role;

    @Column(name = "bias", nullable = false)
    private double bias;

    @Column(name = "stubbornness", nullable = false)
    private double stubbornness;

    @Column(name = "susceptibility", nullable = false)
    private double susceptibility;

    @Column(name = "suspiciousness", nullable = false)
    private double suspiciousness;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 64, nullable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 64, nullable = false)
    private String updatedBy;

    public AgentEntity() {}

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getStubbornness() {
        return stubbornness;
    }

    public void setStubbornness(double stubbornness) {
        this.stubbornness = stubbornness;
    }

    public double getSusceptibility() {
        return susceptibility;
    }

    public void setSusceptibility(double susceptibility) {
        this.susceptibility = susceptibility;
    }

    public double getSuspiciousness() {
        return suspiciousness;
    }

    public void setSuspiciousness(double suspiciousness) {
        this.suspiciousness = suspiciousness;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
