import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import { CreateAgentModal } from "./CreateAgentModal";
import { TopTabs, type HomeTopTab } from "./TopTabs";
import { AgentListSection } from "./AgentListSection";
import { RelationshipComposerPanel } from "./RelationshipComposerPanel";
import { RelationshipListSection } from "./RelationshipListSection";
import type { AppDispatch } from "@/store";
import { listRelationships } from "@/store/slices/relationships-slice";
import {
  runSimulation,
  selectConnectionError,
  selectConnectionStatus,
  selectCurrentRunId,
  selectCurrentTickNumber,
  selectIsSimulationStarting,
} from "@/store/slices/simulation-slice";

type SidePanelDrawerProps = {
  activeTab: HomeTopTab;
  isOpen: boolean;
  networkId: string;
  onClose: () => void;
  onTabChange: (tab: HomeTopTab) => void;
  onToggle: () => void;
};

function AgentPanel({ networkId, onNewClick }: { networkId: string; onNewClick: () => void }) {
  return (
    <section
      aria-labelledby="tab-agents"
      className="flex flex-col h-full"
      id="panel-agents"
      role="tabpanel"
    >
      <button
        onClick={onNewClick}
        className="mx-md mt-md rounded border border-outline-variant px-sm py-xs text-label-caps text-on-surface transition-colors hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-primary"
        type="button"
      >
        + New
      </button>
      <AgentListSection networkId={networkId} />
    </section>
  );
}

function RelationshipPanel({ networkId }: { networkId: string }) {
  const dispatch = useDispatch<AppDispatch>();
  const isStarting = useSelector(selectIsSimulationStarting);
  const runId = useSelector(selectCurrentRunId);
  const connectionStatus = useSelector(selectConnectionStatus);
  const currentTickNumber = useSelector(selectCurrentTickNumber);
  const connectionError = useSelector(selectConnectionError);

  useEffect(() => {
    dispatch(listRelationships(networkId));
  }, [dispatch, networkId]);

  return (
    <section
      aria-labelledby="tab-relationships"
      className="flex h-full flex-col"
      id="panel-relationships"
      role="tabpanel"
    >
      <div className="mx-md mt-md rounded border border-outline-variant bg-surface-container-low p-sm">
        <div className="flex items-center justify-between gap-sm">
          <div className="min-w-0">
            <p className="text-label-caps text-on-surface-variant">Simulation</p>
            <p className="truncate text-body-sm text-on-surface">
              {runId ? `Run ${runId.slice(0, 8)}...` : "No active run"}
            </p>
            <p className="text-label-sm text-on-surface-variant">
              Status: {connectionStatus}
              {currentTickNumber !== null ? ` | Tick ${currentTickNumber}` : ""}
            </p>
            {connectionError ? (
              <p className="mt-xs text-label-sm text-error">{connectionError}</p>
            ) : null}
          </div>
          <button
            className="rounded border border-outline-variant px-sm py-xs text-label-caps text-on-surface transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isStarting || connectionStatus === "connecting" || connectionStatus === "open"}
            onClick={() => dispatch(runSimulation({ networkId, requestedTicks: 100 }))}
            type="button"
          >
            {isStarting ? "Starting..." : "Run Simulation"}
          </button>
        </div>
      </div>
      <RelationshipComposerPanel networkId={networkId} />
      <RelationshipListSection networkId={networkId} />
    </section>
  );
}

export function SidePanelDrawer({
  activeTab,
  isOpen,
  networkId,
  onClose,
  onTabChange,
  onToggle,
}: SidePanelDrawerProps) {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const expandedDesktopClasses = isOpen
    ? "md:w-drawer-expanded"
    : "md:w-drawer-collapsed"

  return (
    <>
      <CreateAgentModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
      />

      <div
        className={[
          "fixed inset-0 z-20 bg-inverse-surface/40 backdrop-blur-[1px] transition-opacity md:hidden",
          isOpen ? "opacity-100" : "pointer-events-none opacity-0",
        ].join(" ")}
        onClick={onClose}
      />

      <aside
        aria-label="Influence network controls"
        className={[
          "fixed left-0 top-0 z-30 h-screen border-r border-outline-variant bg-surface-container-lowest transition-all duration-300",
          "w-screen md:w-auto",
          isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0",
          expandedDesktopClasses,
        ].join(" ")}
      >
        <div className="flex h-full flex-col">
          <header className={[
            "flex items-center border-b border-outline-variant/60",
            isOpen ? "justify-between px-md py-md" : "md:flex-col md:items-center md:justify-start md:px-2 md:py-md md:gap-md",
          ].join(" ")}>
            <div className={isOpen ? "" : "md:sr-only"}>
              <p className="text-label-caps text-on-surface-variant">WhisperNet Control</p>
              <h1 className="text-h3 text-on-surface">Network Home</h1>
            </div>
            <button
              aria-expanded={isOpen}
              aria-label={isOpen ? "Collapse side panel" : "Expand side panel"}
              className={[
                "rounded border border-outline-variant text-on-surface transition-colors hover:bg-surface-container-high focus-visible:outline-2 focus-visible:outline-primary",
                isOpen ? "p-xs" : "md:p-2 md:flex md:items-center md:justify-center",
              ].join(" ")}
              onClick={onToggle}
              type="button"
            >
              <svg
                aria-hidden="true"
                className={`h-5 w-5 transition-transform ${
                  isOpen ? "" : "md:rotate-180"
                }`}
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                viewBox="0 0 24 24"
              >
                <polyline points="15 18 9 12 15 6" />
              </svg>
            </button>
          </header>

          <div className={isOpen ? "" : "md:sr-only"}>
            <TopTabs activeTab={activeTab} isExpanded={isOpen} onTabChange={onTabChange} />
          </div>

          <div className={["min-h-0 flex-1 overflow-y-auto", isOpen ? "" : "md:hidden"].join(" ")}>
            {activeTab === "agents" ? (
              <AgentPanel networkId={networkId} onNewClick={() => setIsCreateModalOpen(true)} />
            ) : (
              <RelationshipPanel networkId={networkId} />
            )}
          </div>
        </div>
      </aside>
    </>
  );
}
