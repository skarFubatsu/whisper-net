/**
 * API client for whisper-net backend
 * Centralized HTTP client with error handling and type safety
 */

import type {
  AgentResponse,
  ApiError,
  CreateNetworkRequest,
  CreateAgentRequest,
  CreateRelationshipRequest,
  ListAgentsResponse,
  NetworkResponse,
  NetworkVersionRequest,
  NetworkVersionResponse,
  ListRelationshipsResponse,
  RelationshipResponse,
  StartSimulationRequest,
  StartSimulationResponse,
} from "@/types/api";

const API_BASE = "/api";

function parseError(response: Response): ApiError {
  return {
    status: response.status,
    message: `HTTP ${response.status}: ${response.statusText}`,
    timestamp: new Date().toISOString(),
  };
}

export async function createAgent(
  request: CreateAgentRequest
): Promise<AgentResponse> {
  const response = await fetch(`${API_BASE}/agents`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function listAgents(): Promise<AgentResponse[]> {
  const response = await fetch(`${API_BASE}/agents`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  const pageResponse: ListAgentsResponse = await response.json();
  return pageResponse.items;
}

export async function getAgent(agentId: string): Promise<AgentResponse> {
  const response = await fetch(`${API_BASE}/agents/${agentId}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function listNetworks(): Promise<NetworkResponse[]> {
  const response = await fetch(`${API_BASE}/networks`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function createNetwork(
  request: CreateNetworkRequest
): Promise<NetworkResponse> {
  const response = await fetch(`${API_BASE}/networks`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function listNetworkAgents(
  networkId: string
): Promise<AgentResponse[]> {
  const response = await fetch(`${API_BASE}/networks/${networkId}/agents`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  const pageResponse: ListAgentsResponse = await response.json();
  return pageResponse.items;
}

export async function listNetworkRelationships(
  networkId: string
): Promise<RelationshipResponse[]> {
  const response = await fetch(`${API_BASE}/networks/${networkId}/relationships`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  const pageResponse: ListRelationshipsResponse = await response.json();
  return pageResponse.items;
}

export async function createNetworkRelationship(
  networkId: string,
  request: CreateRelationshipRequest,
  mirror = true
): Promise<RelationshipResponse> {
  const response = await fetch(
    `${API_BASE}/networks/${networkId}/relationships?mirror=${mirror}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    }
  );

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function deleteNetworkRelationship(
  networkId: string,
  relationshipId: string
): Promise<void> {
  const response = await fetch(
    `${API_BASE}/networks/${networkId}/relationships/${relationshipId}`,
    {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
    }
  );

  if (!response.ok) {
    throw parseError(response);
  }
}

export async function deployNetworkAgent(
  networkId: string,
  agentId: string
): Promise<AgentResponse> {
  const response = await fetch(`${API_BASE}/networks/${networkId}/agents`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ agentId }),
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function undeployNetworkAgent(
  networkId: string,
  agentId: string
): Promise<void> {
  const response = await fetch(
    `${API_BASE}/networks/${networkId}/agents/${agentId}`,
    {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
    }
  );

  if (!response.ok) {
    throw parseError(response);
  }
}

export async function listNetworkVersions(
  networkId: string
): Promise<NetworkVersionResponse[]> {
  const response = await fetch(`${API_BASE}/networks/${networkId}/versions`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function createNetworkVersion(
  networkId: string,
  request: NetworkVersionRequest
): Promise<NetworkVersionResponse> {
  const response = await fetch(`${API_BASE}/networks/${networkId}/versions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}

export async function startSimulation(
  request: StartSimulationRequest
): Promise<StartSimulationResponse> {
  const response = await fetch(`${API_BASE}/simulations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw parseError(response);
  }

  return response.json();
}
