/**
 * Redux store: graph slice
 * Manages graph visualization state (selection, viewport, interaction)
 */

import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

type GraphState = {
  selectedNodeId: string | null;
  viewportState: {
    x: number;
    y: number;
    z: number;
  } | null;
  interactionMode: "select" | "pan" | "zoom";
};

const initialState: GraphState = {
  selectedNodeId: null,
  viewportState: null,
  interactionMode: "select",
};

const graphSlice = createSlice({
  name: "graph",
  initialState,
  reducers: {
    selectNode: (state, action: PayloadAction<string | null>) => {
      state.selectedNodeId = action.payload;
    },
    setViewport: (
      state,
      action: PayloadAction<{ x: number; y: number; z: number }>
    ) => {
      state.viewportState = action.payload;
    },
    setInteractionMode: (
      state,
      action: PayloadAction<"select" | "pan" | "zoom">
    ) => {
      state.interactionMode = action.payload;
    },
    resetGraph: (state) => {
      state.selectedNodeId = null;
      state.viewportState = null;
      state.interactionMode = "select";
    },
  },
});

export const { selectNode, setViewport, setInteractionMode, resetGraph } =
  graphSlice.actions;

export default graphSlice.reducer;

export const selectSelectedNodeId = (state: { graph: GraphState }) =>
  state.graph.selectedNodeId;

export const selectViewportState = (state: { graph: GraphState }) =>
  state.graph.viewportState;

export const selectInteractionMode = (state: { graph: GraphState }) =>
  state.graph.interactionMode;

export const selectIsNodeSelected = (nodeId: string) => (state: { graph: GraphState }) =>
  state.graph.selectedNodeId === nodeId;
