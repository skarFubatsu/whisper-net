import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import { SidePanelDrawer } from "./SidePanelDrawer";
import { GraphCanvas } from "./GraphCanvas";
import {
  fetchAgents,
  fetchDeployedAgents,
  clearDeployedAgents,
  markAgentDeployed,
  markAgentUndeployed,
  setDeployedAgentsFromNetwork,
} from "@/store/slices/agents-slice";
import {
  addRelationshipFromSse,
  clearAllRelationships,
  removeRelationshipFromSse,
  setRelationshipsFromNetwork,
} from "@/store/slices/relationships-slice";
import { createNetwork, listNetworks } from "@/lib/api-client";
import { createNetworkSse } from "@/services/networkSse";
import { createSimulationSse } from "@/services/simulationSse";
import type { AppDispatch } from "@/store";
import {
  addTimelineEvent,
  selectCurrentRunId,
  setConnectionError,
  setConnectionStatus,
  setPlayState,
} from "@/store/slices/simulation-slice";
import type { HomeTopTab } from "./TopTabs";

const DEFAULT_NETWORK_ID = "default-network";
const DEFAULT_NETWORK_NAME = "Default Network";

export function HomePageShell() {
  const dispatch = useDispatch<AppDispatch>();
  const currentRunId = useSelector(selectCurrentRunId);
  const [activeTab, setActiveTab] = useState<HomeTopTab>("agents");
  const [panelOpen, setPanelOpen] = useState<boolean>(true);
  const [networkId, setNetworkId] = useState<string>(DEFAULT_NETWORK_ID);
  const [isNetworkReady, setIsNetworkReady] = useState<boolean>(false);
  const [networkBootstrapError, setNetworkBootstrapError] = useState<string | null>(null);

  useEffect(() => {
    dispatch(fetchAgents());
  }, [dispatch]);

  useEffect(() => {
    if (!isNetworkReady) {
      return;
    }

    dispatch(fetchDeployedAgents(networkId));
  }, [dispatch, isNetworkReady, networkId]);

  useEffect(() => {
    if (!isNetworkReady) {
      return;
    }

    const stream = createNetworkSse(networkId, {
      onSnapshot: (snapshot) => {
        dispatch(setDeployedAgentsFromNetwork(snapshot.agents.map((agent) => agent.agentId)));
        dispatch(setRelationshipsFromNetwork(snapshot.relationships));
      },
      onAgentDeployed: (agent) => {
        dispatch(markAgentDeployed(agent.agentId));
      },
      onAgentUndeployed: ({ agentId }) => {
        dispatch(markAgentUndeployed(agentId));
      },
      onRelationshipCreated: (relationship) => {
        dispatch(addRelationshipFromSse(relationship));
      },
      onRelationshipDeleted: ({ relationshipId }) => {
        dispatch(removeRelationshipFromSse(relationshipId));
      },
      onWarning: (warning) => {
        // Keep the stream alive; the UI can surface this later if needed.
        console.warn("Network SSE warning", warning);
      },
      onError: (error) => {
        console.warn("Network SSE connection closed", error);
      },
    });

    stream.connect();

    return () => {
      stream.disconnect();
      dispatch(clearDeployedAgents());
      dispatch(clearAllRelationships());
    };
  }, [dispatch, isNetworkReady, networkId]);

  useEffect(() => {
    if (!currentRunId) {
      return;
    }

    dispatch(setConnectionStatus("connecting"));

    const stream = createSimulationSse(currentRunId, {
      onConnected: () => {
        dispatch(setConnectionStatus("open"));
      },
      onEvent: (event) => {
        dispatch(addTimelineEvent(event));

        const eventType = event.eventType.toUpperCase();
        const isTerminalEvent =
          eventType === "COMPLETED" ||
          eventType === "FAILED" ||
          eventType === "CANCELLED" ||
          eventType === "SIMULATION_COMPLETED" ||
          eventType === "SIMULATION_FAILED" ||
          eventType === "SIMULATION_CANCELLED";

        dispatch(setPlayState(!isTerminalEvent));
        if (isTerminalEvent) {
          dispatch(setConnectionStatus("closed"));
        }
      },
      onError: (error) => {
        dispatch(setConnectionStatus("error"));
        dispatch(
          setConnectionError(
            error instanceof Error ? error.message : "Simulation stream error"
          )
        );
      },
    });

    stream.connect();

    return () => {
      stream.disconnect();
      dispatch(setPlayState(false));
      dispatch(setConnectionStatus("closed"));
    };
  }, [currentRunId, dispatch]);

  useEffect(() => {
    let cancelled = false;

    const ensureDefaultNetwork = async () => {
      try {
        setNetworkBootstrapError(null);

        const networks = await listNetworks();
        const existingDefault =
          networks.find((network) => network.networkId === DEFAULT_NETWORK_ID) ??
          networks.find((network) => network.name === DEFAULT_NETWORK_NAME);

        if (cancelled) {
          return;
        }

        if (existingDefault) {
          setNetworkId(existingDefault.networkId);
          setIsNetworkReady(true);
          return;
        }

        const createdNetwork = await createNetwork({
          name: DEFAULT_NETWORK_NAME,
          networkId: DEFAULT_NETWORK_ID,
        });

        if (cancelled) {
          return;
        }

        setNetworkId(createdNetwork.networkId);
        setIsNetworkReady(true);
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message = error instanceof Error ? error.message : "Failed to initialize default network";
        setNetworkBootstrapError(message);
      }
    };

    ensureDefaultNetwork();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(max-width: 767px)");

    const applyPanelState = (isMobile: boolean) => {
      setPanelOpen(!isMobile);
    };

    applyPanelState(mediaQuery.matches);

    const listener = (event: MediaQueryListEvent) => {
      applyPanelState(event.matches);
    };

    mediaQuery.addEventListener("change", listener);

    return () => {
      mediaQuery.removeEventListener("change", listener);
    };
  }, []);

  return (
    <main className="relative h-screen w-full overflow-hidden bg-surface text-on-surface">
      {isNetworkReady ? (
        <>
          <GraphCanvas networkId={networkId} />
          <SidePanelDrawer
            activeTab={activeTab}
            isOpen={panelOpen}
            networkId={networkId}
            onClose={() => setPanelOpen(false)}
            onTabChange={setActiveTab}
            onToggle={() => setPanelOpen((current) => !current)}
          />
        </>
      ) : (
        <div className="absolute inset-0 z-20 flex items-center justify-center px-md">
          <div className="rounded-lg border border-outline-variant bg-surface-container-lowest/95 px-lg py-md shadow-[0_2px_0_rgba(13,28,45,0.1)]">
            <p className="text-label-caps text-on-surface-variant">Network Bootstrap</p>
            <p className="text-body-md text-on-surface">
              {networkBootstrapError ?? "Preparing default network..."}
            </p>
          </div>
        </div>
      )}
    </main>
  );
}
