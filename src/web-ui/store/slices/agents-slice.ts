/**
 * Redux store: agents slice
 * Manages agent catalog state
 */

import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { createSelector } from "reselect";

import {
  createAgent,
  getAgent,
  listAgents,
  listNetworkAgents,
  deployNetworkAgent,
  undeployNetworkAgent,
} from "@/lib/api-client";
import type { Agent, CreateAgentFormData } from "@/types/ui";
import type { CreateAgentRequest } from "@/types/api";

type AgentsState = {
  agents: Agent[];
  deployedAgentIds: string[]; // Track which agents are deployed in network
  selectedAgentId: string | null;
  loading: boolean;
  error: string | null;
};

const initialState: AgentsState = {
  agents: [],
  deployedAgentIds: [],
  selectedAgentId: null,
  loading: false,
  error: null,
};

export const fetchAgents = createAsyncThunk(
  "agents/fetchAgents",
  async (_, { rejectWithValue }) => {
    try {
      return await listAgents();
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to fetch agents"
      );
    }
  }
);

export const createNewAgent = createAsyncThunk(
  "agents/createNewAgent",
  async (formData: CreateAgentFormData, { rejectWithValue }) => {
    try {
      const request: CreateAgentRequest = {
        ...formData,
        agentId: formData.agentId || undefined,
      };
      return await createAgent(request);
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to create agent"
      );
    }
  }
);

export const fetchSingleAgent = createAsyncThunk(
  "agents/fetchSingleAgent",
  async (agentId: string, { rejectWithValue }) => {
    try {
      return await getAgent(agentId);
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to fetch agent"
      );
    }
  }
);

export const deployAgent = createAsyncThunk(
  "agents/deploy",
  async (
    { networkId, agentId }: { networkId: string; agentId: string },
    { rejectWithValue }
  ) => {
    try {
      const deployed = await deployNetworkAgent(networkId, agentId);
      return deployed;
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to deploy agent"
      );
    }
  }
);

export const fetchDeployedAgents = createAsyncThunk(
  "agents/fetchDeployedAgents",
  async (networkId: string, { rejectWithValue }) => {
    try {
      return await listNetworkAgents(networkId);
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to fetch deployed agents"
      );
    }
  }
);

export const removeAgent = createAsyncThunk(
  "agents/remove",
  async (
    { networkId, agentId }: { networkId: string; agentId: string },
    { rejectWithValue }
  ) => {
    try {
      await undeployNetworkAgent(networkId, agentId);
      return { networkId, agentId };
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to remove agent"
      );
    }
  }
);

const agentsSlice = createSlice({
  name: "agents",
  initialState,
  reducers: {
    setSelectedAgent: (state, action: PayloadAction<string | null>) => {
      state.selectedAgentId = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
    // SSE updates
    markAgentDeployed: (state, action: PayloadAction<string>) => {
      const id = action.payload;
      if (!state.deployedAgentIds.includes(id)) {
        state.deployedAgentIds.push(id);
      }
    },
    markAgentUndeployed: (state, action: PayloadAction<string>) => {
      state.deployedAgentIds = state.deployedAgentIds.filter((id) => id !== action.payload);
    },
    clearDeployedAgents: (state) => {
      state.deployedAgentIds = [];
    },
    setDeployedAgentsFromNetwork: (state, action: PayloadAction<string[]>) => {
      state.deployedAgentIds = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder.addCase(fetchAgents.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(fetchAgents.fulfilled, (state, action) => {
      state.loading = false;
      state.agents = action.payload;
    });
    builder.addCase(fetchAgents.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(createNewAgent.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(createNewAgent.fulfilled, (state, action) => {
      state.loading = false;
      state.agents.push(action.payload);
    });
    builder.addCase(createNewAgent.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(fetchSingleAgent.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(fetchSingleAgent.fulfilled, (state, action) => {
      state.loading = false;
      const existing = state.agents.find(
        (a) => a.agentId === action.payload.agentId
      );
      if (existing) {
        Object.assign(existing, action.payload);
      } else {
        state.agents.push(action.payload);
      }
    });
    builder.addCase(fetchSingleAgent.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(fetchDeployedAgents.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(fetchDeployedAgents.fulfilled, (state, action) => {
      state.loading = false;

      // Keep agent catalog in sync with network response and rebuild deployment set.
      for (const agent of action.payload) {
        const existing = state.agents.find((current) => current.agentId === agent.agentId);
        if (existing) {
          Object.assign(existing, agent);
        } else {
          state.agents.push(agent);
        }
      }

      state.deployedAgentIds = action.payload.map((agent) => agent.agentId);
    });
    builder.addCase(fetchDeployedAgents.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(deployAgent.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(deployAgent.fulfilled, (state, action) => {
      state.loading = false;
      const agentId = action.payload.agentId;
      if (!state.deployedAgentIds.includes(agentId)) {
        state.deployedAgentIds.push(agentId);
      }
    });
    builder.addCase(deployAgent.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(removeAgent.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(removeAgent.fulfilled, (state, action) => {
      state.loading = false;
      const agentId = action.payload.agentId;
      state.deployedAgentIds = state.deployedAgentIds.filter((id) => id !== agentId);
    });
    builder.addCase(removeAgent.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });
  },
});

export const {
  setSelectedAgent,
  clearError,
  markAgentDeployed,
  markAgentUndeployed,
  clearDeployedAgents,
  setDeployedAgentsFromNetwork,
} = agentsSlice.actions;
export default agentsSlice.reducer;

const selectAgentsState = (state: { agents: AgentsState }) => state.agents;

export const selectAllAgents = createSelector(
  [selectAgentsState],
  (agentsState) => agentsState.agents
);

export const selectDeployedAgents = createSelector(
  [(state: { agents: AgentsState }) => state.agents.agents, 
   (state: { agents: AgentsState }) => state.agents.deployedAgentIds],
  (agents, deployedIds) => agents.filter((a) => deployedIds.includes(a.agentId))
);

export const selectAvailableAgents = createSelector(
  [(state: { agents: AgentsState }) => state.agents.agents, 
   (state: { agents: AgentsState }) => state.agents.deployedAgentIds],
  (agents, deployedIds) => agents.filter((a) => !deployedIds.includes(a.agentId))
);

export const selectAgentById = (id: string) =>
  createSelector(
    [(state: { agents: AgentsState }) => state.agents.agents],
    (agents) => agents.find((a) => a.agentId === id) || null
  );

export const selectIsAgentDeployed = (id: string) =>
  createSelector(
    [(state: { agents: AgentsState }) => state.agents.deployedAgentIds],
    (deployedIds) => deployedIds.includes(id)
  );

export const selectAgentsLoading = createSelector(
  [selectAgentsState],
  (agentsState) => agentsState.loading
);

export const selectAgentsError = createSelector(
  [selectAgentsState],
  (agentsState) => agentsState.error
);
