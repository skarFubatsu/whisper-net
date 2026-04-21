package com.whispernetwork.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

/**
 * JPA entity for timeline events.
 */
@Entity
@Table(
        name = "simulation_timeline_events",
        indexes = {
            @Index(name = "idx_sim_events_owner_run", columnList = "owner_id, run_id"),
            @Index(name = "idx_sim_events_owner_tracking", columnList = "owner_id, tracking_id")
        })
public class SimulationTimelineEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", length = 64, nullable = false)
    private String ownerId;

    @Column(name = "tracking_id", length = 64)
    private String trackingId;

    @Column(name = "run_id", length = 64)
    private String runId;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "network_id", length = 64)
    private String networkId;

    @Column(name = "tick_number")
    private Integer tickNumber;

    @Column(name = "updated_agents")
    private Integer updatedAgents;

    @Column(name = "completed_ticks")
    private Integer completedTicks;

    @Column(name = "reason", length = 400)
    private String reason;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "previous_opinion_value")
    private Double previousOpinionValue;

    @Column(name = "new_opinion_value")
    private Double newOpinionValue;

    @Column(name = "incoming_influence_magnitude")
    private Double incomingInfluenceMagnitude;

    @Convert(converter = StringListConverter.class)
    @Column(name = "contributing_source_agent_ids", length = 1200)
    private List<String> contributingSourceAgentIds;

    @Column(name = "relayed_as_is")
    private Boolean relayedAsIs;

    @Column(name = "relay_origin_agent_id", length = 64)
    private String relayOriginAgentId;

    @Column(name = "ignored_trust_and_weight")
    private Boolean ignoredTrustAndWeight;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public SimulationTimelineEventEntity() {}

    public Long getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public Integer getTickNumber() {
        return tickNumber;
    }

    public void setTickNumber(Integer tickNumber) {
        this.tickNumber = tickNumber;
    }

    public Integer getUpdatedAgents() {
        return updatedAgents;
    }

    public void setUpdatedAgents(Integer updatedAgents) {
        this.updatedAgents = updatedAgents;
    }

    public Integer getCompletedTicks() {
        return completedTicks;
    }

    public void setCompletedTicks(Integer completedTicks) {
        this.completedTicks = completedTicks;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public Double getPreviousOpinionValue() {
        return previousOpinionValue;
    }

    public void setPreviousOpinionValue(Double previousOpinionValue) {
        this.previousOpinionValue = previousOpinionValue;
    }

    public Double getNewOpinionValue() {
        return newOpinionValue;
    }

    public void setNewOpinionValue(Double newOpinionValue) {
        this.newOpinionValue = newOpinionValue;
    }

    public Double getIncomingInfluenceMagnitude() {
        return incomingInfluenceMagnitude;
    }

    public void setIncomingInfluenceMagnitude(Double incomingInfluenceMagnitude) {
        this.incomingInfluenceMagnitude = incomingInfluenceMagnitude;
    }

    public List<String> getContributingSourceAgentIds() {
        return contributingSourceAgentIds;
    }

    public void setContributingSourceAgentIds(List<String> contributingSourceAgentIds) {
        this.contributingSourceAgentIds = contributingSourceAgentIds;
    }

    public Boolean getRelayedAsIs() {
        return relayedAsIs;
    }

    public void setRelayedAsIs(Boolean relayedAsIs) {
        this.relayedAsIs = relayedAsIs;
    }

    public String getRelayOriginAgentId() {
        return relayOriginAgentId;
    }

    public void setRelayOriginAgentId(String relayOriginAgentId) {
        this.relayOriginAgentId = relayOriginAgentId;
    }

    public Boolean getIgnoredTrustAndWeight() {
        return ignoredTrustAndWeight;
    }

    public void setIgnoredTrustAndWeight(Boolean ignoredTrustAndWeight) {
        this.ignoredTrustAndWeight = ignoredTrustAndWeight;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
