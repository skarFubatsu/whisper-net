/**
 * Redux store: simulation slice
 * Manages simulation playback state (SSE connection, timeline events, run info)
 */

import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { createAsyncThunk } from "@reduxjs/toolkit";
import {
  createNetwork,
  createNetworkVersion,
  listNetworks,
  listNetworkVersions,
  startSimulation as startSimulationApi,
} from "@/lib/api-client";
import type { SimulationTimelineEventSnapshot } from "@/types/api";
import type { RootState } from "@/store";

type ConnectionStatus = "idle" | "connecting" | "open" | "closed" | "error";

type SimulationState = {
  currentRunId: string | null;
  connectionStatus: ConnectionStatus;
  connectionError: string | null;
  isStarting: boolean;
  timelineEvents: SimulationTimelineEventSnapshot[];
  isPlaying: boolean;
  currentTickNumber: number | null;
};

function getApiErrorStatus(error: unknown): number | null {
  if (typeof error === "object" && error !== null && "status" in error) {
    const status = (error as { status?: unknown }).status;
    if (typeof status === "number") {
      return status;
    }
  }
  return null;
}

function getApiErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === "object" && error !== null && "message" in error) {
    const message = (error as { message?: unknown }).message;
    if (typeof message === "string") {
      return message;
    }
  }
  return "Failed to start simulation";
}

const initialState: SimulationState = {
  currentRunId: null,
  connectionStatus: "idle",
  connectionError: null,
  isStarting: false,
  timelineEvents: [],
  isPlaying: false,
  currentTickNumber: null,
};

export const runSimulation = createAsyncThunk<
  string,
  { networkId: string; requestedTicks?: number },
  { state: RootState; rejectValue: string }
>("simulation/run", async ({ networkId, requestedTicks = 100 }, { getState, rejectWithValue }) => {
  try {
    const state = getState();
    const deployedAgents = state.agents.agents.filter((agent) =>
      state.agents.deployedAgentIds.includes(agent.agentId)
    );
    const relationships = state.relationships.allIds
      .map((id) => state.relationships.byId[id])
      .filter(Boolean);

    if (deployedAgents.length === 0) {
      return rejectWithValue("Deploy at least one agent before running simulation");
    }

    // Ensure the target network exists for the current actor before version APIs.
    const networks = await listNetworks();
    const hasNetwork = networks.some((network) => network.networkId === networkId);
    if (!hasNetwork) {
      const networkName = networkId === "default-network" ? "Default Network" : networkId;
      try {
        await createNetwork({ networkId, name: networkName });
      } catch (error) {
        // Ignore duplicate create races; any other failure should stop the run.
        if (getApiErrorStatus(error) !== 409) {
          throw error;
        }
      }
    }

    let versionNumber: number | null = null;
    const existingVersions = await listNetworkVersions(networkId);
    if (existingVersions.length > 0) {
      versionNumber = existingVersions[existingVersions.length - 1].versionNumber;
    } else {
      const createdVersion = await createNetworkVersion(networkId, {
        description: "Auto snapshot from web-ui",
        agents: deployedAgents.map((agent) => ({
          agentId: agent.agentId,
          nickname: agent.nickname,
          role: agent.role,
          bias: agent.bias,
          stubbornness: agent.stubbornness,
          susceptibility: agent.susceptibility,
          suspiciousness: agent.suspiciousness,
        })),
        relationships: relationships.map((relationship) => ({
          relationshipId: relationship.relationshipId,
          mirrorGroupId: relationship.mirrorGroupId ?? undefined,
          reverseRelationshipId: relationship.reverseRelationshipId ?? undefined,
          sourceAgentId: relationship.sourceAgentId,
          targetAgentId: relationship.targetAgentId,
          weight: relationship.weight ?? 0,
          trustValue: relationship.trustValue ?? 0,
          relationshipType: relationship.relationshipType,
          transmissionMode: relationship.transmissionMode ?? "NORMAL_FLOW",
        })),
      });
      versionNumber = createdVersion.versionNumber;
    }

    const requestId =
      typeof globalThis.crypto?.randomUUID === "function"
        ? globalThis.crypto.randomUUID()
        : `run-${Date.now()}`;

    const actorId = import.meta.env.VITE_API_ACTOR_ID ?? "guest";

    const response = await startSimulationApi({
      networkId,
      networkVersionNumber: versionNumber,
      ownerId: actorId,
      actorId,
      clientRequestId: requestId,
      requestedTicks,
    });

    return response.simulationRunId;
  } catch (error) {
    return rejectWithValue(getApiErrorMessage(error));
  }
});

const simulationSlice = createSlice({
  name: "simulation",
  initialState,
  reducers: {
    startSimulation: (state, action: PayloadAction<string>) => {
      state.currentRunId = action.payload;
      state.connectionStatus = "connecting";
      state.connectionError = null;
      state.timelineEvents = [];
      state.isPlaying = false;
      state.currentTickNumber = null;
    },
    setConnectionStatus: (state, action: PayloadAction<ConnectionStatus>) => {
      state.connectionStatus = action.payload;
    },
    setConnectionError: (state, action: PayloadAction<string | null>) => {
      state.connectionError = action.payload;
    },
    addTimelineEvent: (
      state,
      action: PayloadAction<SimulationTimelineEventSnapshot>
    ) => {
      state.timelineEvents.push(action.payload);
      // Update current tick if applicable
      if (action.payload.tickNumber !== undefined && action.payload.tickNumber !== null) {
        state.currentTickNumber = action.payload.tickNumber;
      }
    },
    setPlayState: (state, action: PayloadAction<boolean>) => {
      state.isPlaying = action.payload;
    },
    clearSimulation: (state) => {
      state.currentRunId = null;
      state.connectionStatus = "idle";
      state.connectionError = null;
      state.isStarting = false;
      state.timelineEvents = [];
      state.isPlaying = false;
      state.currentTickNumber = null;
    },
  },
  extraReducers: (builder) => {
    builder.addCase(runSimulation.pending, (state) => {
      state.isStarting = true;
      state.connectionStatus = "connecting";
      state.connectionError = null;
      state.timelineEvents = [];
      state.currentTickNumber = null;
      state.isPlaying = false;
    });
    builder.addCase(runSimulation.fulfilled, (state, action) => {
      state.isStarting = false;
      state.currentRunId = action.payload;
      state.connectionStatus = "connecting";
      state.connectionError = null;
    });
    builder.addCase(runSimulation.rejected, (state, action) => {
      state.isStarting = false;
      state.connectionStatus = "error";
      state.connectionError = action.payload ?? "Failed to start simulation";
    });
  },
});

export const {
  startSimulation,
  setConnectionStatus,
  setConnectionError,
  addTimelineEvent,
  setPlayState,
  clearSimulation,
} = simulationSlice.actions;

export default simulationSlice.reducer;

export const selectCurrentRunId = (state: { simulation: SimulationState }) =>
  state.simulation.currentRunId;

export const selectConnectionStatus = (state: { simulation: SimulationState }) =>
  state.simulation.connectionStatus;

export const selectConnectionError = (state: { simulation: SimulationState }) =>
  state.simulation.connectionError;

export const selectTimelineEvents = (state: { simulation: SimulationState }) =>
  state.simulation.timelineEvents;

export const selectIsSimulationStarting = (state: { simulation: SimulationState }) =>
  state.simulation.isStarting;

export const selectIsSimulationPlaying = (state: { simulation: SimulationState }) =>
  state.simulation.isPlaying;

export const selectCurrentTickNumber = (state: { simulation: SimulationState }) =>
  state.simulation.currentTickNumber;

export const selectTimelineEventsByType = (eventType: string) => (state: { simulation: SimulationState }) =>
  state.simulation.timelineEvents.filter((ev) => ev.eventType === eventType);

export const selectLastTimelineEvent = (state: { simulation: SimulationState }) =>
  state.simulation.timelineEvents[state.simulation.timelineEvents.length - 1] || null;
