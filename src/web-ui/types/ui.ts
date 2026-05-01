/**
 * UI-specific types derived from API contracts
 * Adaptations for UI state management and forms
 */

import type { AgentResponse, CreateAgentRequest, RelationshipResponse } from "./api";

/** UI representation of Agent with computed/UI-specific fields */
export type Agent = AgentResponse & {
  isDeployed?: boolean;
};

/** Relationship with human-readable metadata for UI */
export type Relationship = RelationshipResponse & {
  // Can extend with UI-specific computed fields later
};

/** Form state for create agent modal */
export type CreateAgentFormData = Omit<CreateAgentRequest, "agentId"> & {
  agentId: string | null;
};

/** Validation state for form fields */
export type FormFieldError = {
  field: keyof CreateAgentFormData;
  message: string;
};

/** Loading/async state */
export type AsyncState = {
  loading: boolean;
  error: string | null;
};

/** UI toast notification */
export type Toast = {
  id: string;
  message: string;
  type: "success" | "error" | "info";
  duration?: number; // ms, undefined = persistent
};
