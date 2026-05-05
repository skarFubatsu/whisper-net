import type { Agent } from "@/types/ui";

type AgentRowCardProps = {
  agent: Agent;
  isDeployed: boolean;
  onDeploy?: () => void;
  onRemove?: () => void;
  isLoading?: boolean;
};

export function AgentRowCard({
  agent,
  isDeployed,
  onDeploy,
  onRemove,
  isLoading = false,
}: AgentRowCardProps) {
  return (
    <div className="flex items-center justify-between rounded border border-outline-variant bg-surface-container-low p-md hover:bg-surface-container transition-colors">
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-sm">
          <h3 className="text-body-md font-medium text-on-surface truncate">
            {agent.nickname}
          </h3>
          <span className="inline-flex rounded px-xs py-0.5 text-label-caps text-xs font-medium bg-tertiary-container text-on-tertiary-container whitespace-nowrap">
            {agent.role}
          </span>
          <span
            className={[
              "inline-flex rounded px-xs py-0.5 text-label-caps text-xs font-medium whitespace-nowrap",
              isDeployed
                ? "bg-tertiary text-on-tertiary"
                : "bg-outline-variant text-on-surface-variant",
            ].join(" ")}
          >
            {isDeployed ? "Deployed" : "Available"}
          </span>
        </div>
        <p className="mt-xs text-body-sm text-on-surface-variant truncate">
          ID: {agent.agentId}
        </p>
      </div>

      <div className="ml-md flex gap-sm">
        {!isDeployed && onDeploy && (
          <button
            onClick={onDeploy}
            disabled={isLoading}
            className={[
              "rounded px-sm py-xs text-label-caps font-medium transition-colors",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
              isLoading
                ? "bg-primary/50 text-on-primary/50 cursor-not-allowed"
                : "bg-primary text-on-primary hover:bg-primary-container",
            ].join(" ")}
            type="button"
          >
            Deploy
          </button>
        )}
        {isDeployed && onRemove && (
          <button
            onClick={onRemove}
            disabled={isLoading}
            aria-label={`Remove agent ${agent.nickname}`}
            className={[
              "rounded p-xs text-on-surface transition-colors",
              "focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary",
              isLoading
                ? "text-on-surface/50 cursor-not-allowed"
                : "hover:bg-error/10",
            ].join(" ")}
            type="button"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              viewBox="0 0 24 24"
            >
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              <line x1={10} y1={11} x2={10} y2={17} />
              <line x1={14} y1={11} x2={14} y2={17} />
            </svg>
          </button>
        )}
      </div>
    </div>
  );
}
