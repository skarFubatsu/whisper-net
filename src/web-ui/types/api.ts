/**
 * API Contract types for whisper-net backend
 * Aligned to server-side DTOs and domain models
 */

/** Agent traits: psychological properties [0.0-1.0] */
export type Persona = {
  bias: number;
  stubbornness: number;
  susceptibility: number;
  suspiciousness: number;
};

export type AgentRole = "NORMAL" | "TRIGGER" | "RELAY";

export type CreateAgentRequest = {
  agentId?: string | null;
  nickname: string;
  role?: AgentRole;
  bias: number;
  stubbornness: number;
  susceptibility: number;
  suspiciousness: number;
};

export type AgentResponse = {
  agentId: string;
  nickname: string;
  role: AgentRole;
  bias: number;
  stubbornness: number;
  susceptibility: number;
  suspiciousness: number;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  total: number;
};

export type ListAgentsResponse = PageResponse<AgentResponse>;

export type CreateNetworkRequest = {
  name: string;
  networkId?: string;
};

/** Network response from backend */
export type NetworkResponse = {
  networkId: string;
  name: string;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
};

/** Relationship types enum — canonical values from simulation-service core/model */
export type RelationshipType =
  | "NEUTRAL"
  | "FAMILY"
  | "FRIEND"
  | "PEER"
  | "ANTAGONIST";

export type RelationshipTransmissionMode = "NORMAL_FLOW" | "RELAY_CHANNEL";

export type CreateRelationshipRequest = {
  relationshipId?: string;
  mirrorGroupId?: string;
  reverseRelationshipId?: string;
  sourceAgentId: string;
  targetAgentId: string;
  weight: number;
  trustValue: number;
  relationshipType: RelationshipType;
  transmissionMode: RelationshipTransmissionMode;
};

export type ListRelationshipsResponse = PageResponse<RelationshipResponse>;

export type RelationshipResponse = {
  relationshipId: string;
  mirrorGroupId?: string | null;
  reverseRelationshipId?: string | null;
  sourceAgentId: string;
  targetAgentId: string;
  weight?: number | null;
  trustValue?: number | null;
  relationshipType: RelationshipType;
  transmissionMode?: RelationshipTransmissionMode | null;
  createdAt?: string; // ISO 8601
};

export type NetworkVersionResponse = {
  networkId: string;
  versionNumber: number;
  description?: string | null;
  createdBy?: string | null;
  createdAt: string;
};

export type AgentSnapshotRequest = {
  agentId: string;
  nickname: string;
  role?: AgentRole | null;
  bias: number;
  stubbornness: number;
  susceptibility: number;
  suspiciousness: number;
};

export type NetworkVersionRequest = {
  description?: string | null;
  baseVersionNumber?: number;
  agents: AgentSnapshotRequest[];
  relationships: CreateRelationshipRequest[];
};

export type StartSimulationRequest = {
  networkId: string;
  networkVersionNumber: number;
  ownerId: string;
  actorId: string;
  clientRequestId: string;
  requestedTicks: number;
};

export type StartSimulationResponse = {
  simulationRunId: string;
};

export type ApiError = {
  status: number;
  message: string;
  timestamp: string;
};

/** Simulation timeline event snapshot (SSE payload) */
export type SimulationTimelineEventSnapshot = {
  eventType: string;
  simulationRunId: string;
  networkId: string;
  tickNumber?: number | null;
  updatedAgents?: number | null;
  completedTicks?: number | null;
  reason?: string | null;
  agentId?: string | null;
  previousOpinionValue?: number | null;
  newOpinionValue?: number | null;
  incomingInfluenceMagnitude?: number | null;
  contributingSourceAgentIds?: string[] | null;
  relayedAsIs?: boolean | null;
  relayOriginAgentId?: string | null;
  ignoredTrustAndWeight?: boolean | null;
  occurredAt: string; // ISO timestamp
};
