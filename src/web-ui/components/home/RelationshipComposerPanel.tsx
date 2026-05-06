import { useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import type { AppDispatch } from "@/store";
import { deployAgent, selectAllAgents } from "@/store/slices/agents-slice";
import {
  createRelationship,
  selectRelationshipsLoading,
} from "@/store/slices/relationships-slice";
import type { CreateRelationshipRequest, RelationshipType, RelationshipTransmissionMode } from "@/types/api";

const RELATIONSHIP_TYPES: RelationshipType[] = [
  "FAMILY",
  "FRIEND",
  "PEER",
  "ANTAGONIST",
  "NEUTRAL",
];

const TRANSMISSION_MODES: RelationshipTransmissionMode[] = ["NORMAL_FLOW", "RELAY_CHANNEL"];

type RelationshipComposerPanelProps = {
  networkId: string;
};

type AgentSuggestion = {
  agentId: string;
  nickname: string;
  isDeployed: boolean;
};

function StatusBadge({ deployed }: { deployed: boolean }) {
  return (
    <span
      className={[
        "inline-flex items-center gap-1 rounded-full border px-2 py-[2px] text-[10px] font-semibold uppercase tracking-wide",
        deployed
          ? "border-primary/30 bg-primary/10 text-primary"
          : "border-outline-variant bg-surface-container text-on-surface-variant",
      ].join(" ")}
    >
      {deployed ? (
        <svg aria-hidden="true" className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M20 6 9 17l-5-5" />
        </svg>
      ) : (
        <svg aria-hidden="true" className="h-3 w-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 8v4" />
          <circle cx="12" cy="16" r="0.8" fill="currentColor" stroke="none" />
        </svg>
      )}
      {deployed ? "Deployed" : "Not deployed"}
    </span>
  );
}

export function RelationshipComposerPanel({ networkId }: RelationshipComposerPanelProps) {
  const dispatch = useDispatch<AppDispatch>();
  const agents = useSelector(selectAllAgents);
  const deployedAgentIds = useSelector(
    (state: { agents: { deployedAgentIds: string[] } }) => state.agents.deployedAgentIds
  );
  const loading = useSelector(selectRelationshipsLoading);

  const [sourceAgentId, setSourceAgentId] = useState("");
  const [targetAgentId, setTargetAgentId] = useState("");
  const [sourceQuery, setSourceQuery] = useState("");
  const [targetQuery, setTargetQuery] = useState("");
  const [showSourceSuggestions, setShowSourceSuggestions] = useState(false);
  const [showTargetSuggestions, setShowTargetSuggestions] = useState(false);
  const [relationshipType, setRelationshipType] = useState<RelationshipType>("NEUTRAL");
  const [weight, setWeight] = useState<number>(0.5);
  const [trustValue, setTrustValue] = useState<number>(0.5);
  const [transmissionMode, setTransmissionMode] = useState<RelationshipTransmissionMode>("NORMAL_FLOW");
  const [formError, setFormError] = useState<string | null>(null);

  const deployedIdSet = useMemo(() => new Set(deployedAgentIds), [deployedAgentIds]);
  const agentById = useMemo(() => new Map(agents.map((agent) => [agent.agentId, agent])), [agents]);

  const agentSuggestions = useMemo<AgentSuggestion[]>(() => {
    const sorted = [...agents].sort((left, right) => {
      const leftDeployed = deployedIdSet.has(left.agentId);
      const rightDeployed = deployedIdSet.has(right.agentId);
      if (leftDeployed !== rightDeployed) {
        return leftDeployed ? -1 : 1;
      }
      return left.nickname.localeCompare(right.nickname);
    });

    return sorted.map((agent) => ({
      agentId: agent.agentId,
      nickname: agent.nickname,
      isDeployed: deployedIdSet.has(agent.agentId),
    }));
  }, [agents, deployedIdSet]);

  const getFilteredSuggestions = (query: string) => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return agentSuggestions;
    }

    return agentSuggestions.filter((agent) => {
      return (
        agent.nickname.toLowerCase().includes(normalized) ||
        agent.agentId.toLowerCase().includes(normalized)
      );
    });
  };

  const sourceSuggestions = useMemo(
    () => getFilteredSuggestions(sourceQuery),
    [agentSuggestions, sourceQuery]
  );
  const targetSuggestions = useMemo(
    () => getFilteredSuggestions(targetQuery),
    [agentSuggestions, targetQuery]
  );

  const pendingDeployAgents = useMemo(() => {
    const ids = [sourceAgentId, targetAgentId].filter((id) => id.length > 0);
    const uniqueIds = Array.from(new Set(ids));
    return uniqueIds
      .map((id) => agentById.get(id))
      .filter((agent): agent is NonNullable<typeof agent> => Boolean(agent))
      .filter((agent) => !deployedIdSet.has(agent.agentId));
  }, [agentById, deployedIdSet, sourceAgentId, targetAgentId]);

  const canSubmit = useMemo(() => {
    const sourceExists = agentById.has(sourceAgentId);
    const targetExists = agentById.has(targetAgentId);

    return (
      !loading &&
      sourceAgentId.length > 0 &&
      targetAgentId.length > 0 &&
      sourceExists &&
      targetExists &&
      sourceAgentId !== targetAgentId &&
      weight >= 0 &&
      weight <= 1 &&
      trustValue >= 0 &&
      trustValue <= 1
    );
  }, [agentById, loading, sourceAgentId, targetAgentId, trustValue, weight]);

  const getAgentName = (agentId: string) => {
    return agentById.get(agentId)?.nickname ?? agentId;
  };

  const ensureAgentIsDeployed = async (agentId: string) => {
    if (deployedIdSet.has(agentId)) {
      return;
    }

    try {
      await dispatch(deployAgent({ networkId, agentId })).unwrap();
    } catch (error) {
      const message = typeof error === "string" ? error : error instanceof Error ? error.message : "";
      const alreadyDeployed = message.includes("HTTP 409") || message.toLowerCase().includes("already deployed");

      if (!alreadyDeployed) {
        throw error;
      }
    }
  };

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (sourceAgentId === targetAgentId) {
      setFormError("Source and target must be different agents.");
      return;
    }

    if (!agentById.has(sourceAgentId) || !agentById.has(targetAgentId)) {
      setFormError("Select valid source and target agents from suggestions.");
      return;
    }

    const request: CreateRelationshipRequest = {
      sourceAgentId,
      targetAgentId,
      relationshipType,
      weight,
      trustValue,
      transmissionMode,
    };

    try {
      const deployIds = Array.from(new Set([sourceAgentId, targetAgentId]));
      for (const agentId of deployIds) {
        await ensureAgentIsDeployed(agentId);
      }

      const mirror = true;
      await dispatch(createRelationship({ networkId, request, mirror })).unwrap();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Failed to create relationship.");
      return;
    }

    setTargetAgentId("");
    setTargetQuery("");
  };

  const handleSourceSelect = (agentId: string) => {
    const selected = agentById.get(agentId);
    if (!selected) {
      return;
    }

    setSourceAgentId(selected.agentId);
    setSourceQuery(selected.nickname);
    setShowSourceSuggestions(false);
  };

  const handleTargetSelect = (agentId: string) => {
    const selected = agentById.get(agentId);
    if (!selected) {
      return;
    }

    setTargetAgentId(selected.agentId);
    setTargetQuery(selected.nickname);
    setShowTargetSuggestions(false);
  };

  const selectedSource = sourceAgentId ? agentById.get(sourceAgentId) : null;
  const selectedTarget = targetAgentId ? agentById.get(targetAgentId) : null;

  return (
    <section className="space-y-md border-b border-outline-variant/60 p-md">
      <h2 className="text-label-caps text-on-surface-variant">Create Relationship</h2>

      {formError && (
        <p className="rounded border border-error bg-error/10 p-xs text-label-sm text-error">
          {formError}
        </p>
      )}

      {pendingDeployAgents.length > 0 && (
        <p className="rounded border border-secondary-container bg-secondary-container/35 p-xs text-label-sm text-on-surface">
          {pendingDeployAgents.length === 1
            ? `${pendingDeployAgents[0].nickname} agent will be deployed in the current network when this relationship is created.`
            : `${pendingDeployAgents.map((agent) => agent.nickname).join(", ")} agents will be deployed in the current network when this relationship is created.`}
        </p>
      )}

      <form className="space-y-sm" onSubmit={onSubmit}>
        <div className="grid grid-cols-1 gap-sm">
          <label className="space-y-xs">
            <span className="text-label-sm text-on-surface">Source Agent</span>
            <div className="relative">
              <input
                className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
                placeholder="Search source by name or ID"
                type="search"
                value={sourceQuery}
                onBlur={() => {
                  window.setTimeout(() => setShowSourceSuggestions(false), 120);
                }}
                onChange={(event) => {
                  const nextValue = event.target.value;
                  setSourceQuery(nextValue);
                  setSourceAgentId("");
                  setShowSourceSuggestions(true);
                }}
                onFocus={() => setShowSourceSuggestions(true)}
              />
              {showSourceSuggestions && (
                <ul className="absolute z-20 mt-1 max-h-44 w-full overflow-y-auto rounded border border-outline-variant bg-surface-container-lowest shadow-lg">
                  {sourceSuggestions.length > 0 ? (
                    sourceSuggestions.map((agent) => (
                      <li key={`src-option-${agent.agentId}`}>
                        <button
                          className="flex w-full items-center justify-between gap-sm px-sm py-xs text-left hover:bg-surface-container"
                          onMouseDown={(event) => event.preventDefault()}
                          onClick={() => handleSourceSelect(agent.agentId)}
                          type="button"
                        >
                          <span className="min-w-0">
                            <span className="block truncate text-body-sm text-on-surface">{agent.nickname}</span>
                            <span className="block truncate text-label-xs text-on-surface-variant">{agent.agentId}</span>
                          </span>
                          <StatusBadge deployed={agent.isDeployed} />
                        </button>
                      </li>
                    ))
                  ) : (
                    <li className="px-sm py-xs text-label-sm text-on-surface-variant">No matching agents</li>
                  )}
                </ul>
              )}
            </div>
            {sourceQuery.length > 0 && (
              <span className="inline-flex items-center gap-2 text-label-xs text-on-surface-variant">
                {selectedSource ? (
                  <>
                    <span>Selected: {getAgentName(sourceAgentId)}</span>
                    <StatusBadge deployed={deployedIdSet.has(sourceAgentId)} />
                  </>
                ) : (
                  "No matching agent selected yet. Choose from suggestions."
                )}
              </span>
            )}
          </label>

          <label className="space-y-xs">
            <span className="text-label-sm text-on-surface">Target Agent</span>
            <div className="relative">
              <input
                className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
                placeholder="Search target by name or ID"
                type="search"
                value={targetQuery}
                onBlur={() => {
                  window.setTimeout(() => setShowTargetSuggestions(false), 120);
                }}
                onChange={(event) => {
                  const nextValue = event.target.value;
                  setTargetQuery(nextValue);
                  setTargetAgentId("");
                  setShowTargetSuggestions(true);
                }}
                onFocus={() => setShowTargetSuggestions(true)}
              />
              {showTargetSuggestions && (
                <ul className="absolute z-20 mt-1 max-h-44 w-full overflow-y-auto rounded border border-outline-variant bg-surface-container-lowest shadow-lg">
                  {targetSuggestions.length > 0 ? (
                    targetSuggestions.map((agent) => (
                      <li key={`tgt-option-${agent.agentId}`}>
                        <button
                          className="flex w-full items-center justify-between gap-sm px-sm py-xs text-left hover:bg-surface-container"
                          onMouseDown={(event) => event.preventDefault()}
                          onClick={() => handleTargetSelect(agent.agentId)}
                          type="button"
                        >
                          <span className="min-w-0">
                            <span className="block truncate text-body-sm text-on-surface">{agent.nickname}</span>
                            <span className="block truncate text-label-xs text-on-surface-variant">{agent.agentId}</span>
                          </span>
                          <StatusBadge deployed={agent.isDeployed} />
                        </button>
                      </li>
                    ))
                  ) : (
                    <li className="px-sm py-xs text-label-sm text-on-surface-variant">No matching agents</li>
                  )}
                </ul>
              )}
            </div>
            {targetQuery.length > 0 && (
              <span className="inline-flex items-center gap-2 text-label-xs text-on-surface-variant">
                {selectedTarget ? (
                  <>
                    <span>Selected: {getAgentName(targetAgentId)}</span>
                    <StatusBadge deployed={deployedIdSet.has(targetAgentId)} />
                  </>
                ) : (
                  "No matching agent selected yet. Choose from suggestions."
                )}
              </span>
            )}
          </label>

          <label className="space-y-xs">
            <span className="text-label-sm text-on-surface">Relationship Type</span>
            <select
              className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
              value={relationshipType}
              onChange={(event) => setRelationshipType(event.target.value as RelationshipType)}
            >
              {RELATIONSHIP_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </label>

          <div className="grid grid-cols-2 gap-sm">
            <label className="space-y-xs">
              <span className="text-label-sm text-on-surface">Weight</span>
              <input
                className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
                type="number"
                min={0}
                max={1}
                step={0.05}
                value={weight}
                onChange={(event) => setWeight(Number(event.target.value))}
              />
            </label>

            <label className="space-y-xs">
              <span className="text-label-sm text-on-surface">Trust</span>
              <input
                className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
                type="number"
                min={0}
                max={1}
                step={0.05}
                value={trustValue}
                onChange={(event) => setTrustValue(Number(event.target.value))}
              />
            </label>
          </div>

          <label className="space-y-xs">
            <span className="text-label-sm text-on-surface">Transmission Mode</span>
            <select
              className="w-full rounded border border-outline-variant bg-surface px-sm py-xs text-body-md text-on-surface focus-visible:outline-2 focus-visible:outline-primary"
              value={transmissionMode}
              onChange={(event) => setTransmissionMode(event.target.value as RelationshipTransmissionMode)}
            >
              {TRANSMISSION_MODES.map((mode) => (
                <option key={mode} value={mode}>
                  {mode}
                </option>
              ))}
            </select>
          </label>

          <label className="flex items-center gap-xs text-label-sm text-on-surface">
            <input
              checked
              disabled
              type="checkbox"
            />
            Create mirrored relationship (always on)
          </label>
        </div>

        <button
          className="w-full rounded bg-primary px-md py-xs text-label-caps text-on-primary transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:opacity-50"
          disabled={!canSubmit}
          type="submit"
        >
          {loading ? "Creating..." : "Create Relationship"}
        </button>
      </form>
    </section>
  );
}
