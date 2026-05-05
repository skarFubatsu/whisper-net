import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";

import type { AppDispatch } from "@/store";
import {
  selectAllAgents,
  selectDeployedAgents,
  selectAvailableAgents,
  selectAgentsLoading,
  deployAgent,
  removeAgent,
} from "@/store/slices/agents-slice";
import type { AgentSubTab } from "./SubTabNav";
import { SubTabNav } from "./SubTabNav";
import { AgentRowCard } from "./AgentRowCard";

type AgentListSectionProps = {
  networkId: string;
};

export function AgentListSection({ networkId }: AgentListSectionProps) {
  const dispatch = useDispatch<AppDispatch>();
  const [subTab, setSubTab] = useState<AgentSubTab>("deployed");
  
  const allAgents = useSelector(selectAllAgents);
  const deployedAgents = useSelector(selectDeployedAgents);
  const availableAgents = useSelector(selectAvailableAgents);
  const isLoading = useSelector(selectAgentsLoading);

  const filteredAgents =
    subTab === "deployed"
      ? deployedAgents
      : subTab === "available"
        ? availableAgents
        : allAgents;

  const handleDeploy = (agentId: string) => {
    dispatch(deployAgent({ networkId, agentId }));
  };

  const handleRemove = (agentId: string) => {
    dispatch(removeAgent({ networkId, agentId }));
  };

  return (
    <div className="flex flex-col h-full">
      <SubTabNav activeTab={subTab} onTabChange={setSubTab} />

      <div className="min-h-0 flex-1 overflow-y-auto p-md space-y-md">
        {filteredAgents.length === 0 ? (
          <p className="text-body-md text-on-surface-variant text-center py-lg">
            {subTab === "deployed" ? "No deployed agents" : "No available agents"}
          </p>
        ) : (
          filteredAgents.map((agent) => (
            <AgentRowCard
              key={agent.agentId}
              agent={agent}
              isDeployed={deployedAgents.some((a) => a.agentId === agent.agentId)}
              onDeploy={() => handleDeploy(agent.agentId)}
              onRemove={() => handleRemove(agent.agentId)}
              isLoading={isLoading}
            />
          ))
        )}
      </div>
    </div>
  );
}
