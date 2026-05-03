/**
 * Redux store configuration
 * Combines all slices and configures the store
 */

import { configureStore } from "@reduxjs/toolkit";

import agentsReducer from "./slices/agents-slice";
import relationshipsReducer from "./slices/relationships-slice";
import graphReducer from "./slices/graph-slice";
import simulationReducer from "./slices/simulation-slice";

export const store = configureStore({
  reducer: {
    agents: agentsReducer,
    relationships: relationshipsReducer,
    graph: graphReducer,
    simulation: simulationReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
