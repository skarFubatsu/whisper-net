/**
 * Redux store: relationships slice
 * Manages relationships state with normalized (byId) structure for efficient updates
 */

import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { createSelector } from "@reduxjs/toolkit";
import {
  createNetworkRelationship,
  deleteNetworkRelationship,
  listNetworkRelationships,
} from "@/lib/api-client";
import type { CreateRelationshipRequest, RelationshipResponse } from "@/types/api";
import type { RootState } from "@/store";

type RelationshipsState = {
  byId: Record<string, RelationshipResponse>;
  allIds: string[];
  loading: boolean;
  error: string | null;
};

const initialState: RelationshipsState = {
  byId: {},
  allIds: [],
  loading: false,
  error: null,
};

export const createRelationship = createAsyncThunk<
  RelationshipResponse,
  { networkId: string; request: CreateRelationshipRequest; mirror?: boolean },
  { rejectValue: string }
>(
  "relationships/create",
  async ({ networkId, request, mirror = true }, { rejectWithValue }) => {
    try {
      return await createNetworkRelationship(networkId, request, mirror);
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to create relationship"
      );
    }
  }
);

export const deleteRelationship = createAsyncThunk<
  string,
  { networkId: string; relationshipId: string },
  { rejectValue: string }
>(
  "relationships/delete",
  async ({ networkId, relationshipId }, { rejectWithValue }) => {
    try {
      await deleteNetworkRelationship(networkId, relationshipId);
      return relationshipId;
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to delete relationship"
      );
    }
  }
);

export const listRelationships = createAsyncThunk<
  RelationshipResponse[],
  string,
  { rejectValue: string }
>(
  "relationships/list",
  async (networkId: string, { rejectWithValue }) => {
    try {
      return await listNetworkRelationships(networkId);
    } catch (error) {
      return rejectWithValue(
        error instanceof Error ? error.message : "Failed to list relationships"
      );
    }
  }
);

const relationshipsSlice = createSlice({
  name: "relationships",
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    // Direct mutations for SSE updates
    addRelationshipFromSse: (state, action: PayloadAction<RelationshipResponse>) => {
      const rel = action.payload;
      if (!state.allIds.includes(rel.relationshipId)) {
        state.allIds.push(rel.relationshipId);
      }
      state.byId[rel.relationshipId] = rel;
    },
    removeRelationshipFromSse: (state, action: PayloadAction<string>) => {
      const relId = action.payload;
      state.allIds = state.allIds.filter((id) => id !== relId);
      delete state.byId[relId];
    },
    clearAllRelationships: (state) => {
      state.byId = {};
      state.allIds = [];
    },
    setRelationshipsFromNetwork: (state, action: PayloadAction<RelationshipResponse[]>) => {
      state.byId = {};
      state.allIds = [];
      action.payload.forEach((rel) => {
        state.byId[rel.relationshipId] = rel;
        state.allIds.push(rel.relationshipId);
      });
    },
  },
  extraReducers: (builder) => {
    builder.addCase(createRelationship.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(createRelationship.fulfilled, (state, action) => {
      state.loading = false;
      const rel = action.payload;
      if (!state.allIds.includes(rel.relationshipId)) {
        state.allIds.push(rel.relationshipId);
      }
      state.byId[rel.relationshipId] = rel;
    });
    builder.addCase(createRelationship.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(deleteRelationship.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(deleteRelationship.fulfilled, (state, action) => {
      state.loading = false;
      const relId = action.payload;
      state.allIds = state.allIds.filter((id) => id !== relId);
      delete state.byId[relId];
    });
    builder.addCase(deleteRelationship.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });

    builder.addCase(listRelationships.pending, (state) => {
      state.loading = true;
      state.error = null;
    });
    builder.addCase(listRelationships.fulfilled, (state, action) => {
      state.loading = false;
      const relationships = action.payload;
      state.byId = {};
      state.allIds = [];
      relationships.forEach((rel) => {
        state.byId[rel.relationshipId] = rel;
        state.allIds.push(rel.relationshipId);
      });
    });
    builder.addCase(listRelationships.rejected, (state, action) => {
      state.loading = false;
      state.error = action.payload as string;
    });
  },
});

export const {
  clearError,
  addRelationshipFromSse,
  removeRelationshipFromSse,
  clearAllRelationships,
  setRelationshipsFromNetwork,
} = relationshipsSlice.actions;

export default relationshipsSlice.reducer;

const selectRelationshipsState = (state: RootState) => state.relationships;

export const selectAllRelationships = createSelector(
  [selectRelationshipsState],
  (relationshipsState) =>
    relationshipsState.allIds
      .map((id) => relationshipsState.byId[id])
      .filter((rel): rel is RelationshipResponse => Boolean(rel))
);

export const selectRelationshipById =
  (id: string) => (state: RootState) =>
    state.relationships.byId[id];

export const selectRelationshipsByAgent =
  (agentId: string) =>
  createSelector([selectAllRelationships], (relationships) =>
    relationships.filter(
      (rel) => rel.sourceAgentId === agentId || rel.targetAgentId === agentId
    )
  );

export const selectRelationshipsLoading = (state: RootState) =>
  state.relationships.loading;

export const selectRelationshipsError = (state: RootState) =>
  state.relationships.error;
