import type { RelationshipResponse, AgentResponse } from "../types/api";

export type NetworkSseHandlers = {
  onSnapshot?: (snapshot: { agents: AgentResponse[]; relationships: RelationshipResponse[] }) => void;
  onHeartbeat?: (ts: string) => void;
  onWarning?: (warning: any) => void;
  onAgentDeployed?: (agent: AgentResponse) => void;
  onAgentUndeployed?: (payload: { agentId: string }) => void;
  onRelationshipCreated?: (rel: RelationshipResponse) => void;
  onRelationshipDeleted?: (payload: { relationshipId: string }) => void;
  onVersionDeployed?: (version: any) => void;
  onError?: (err: any) => void;
};

export function createNetworkSse(networkId: string, handlers: NetworkSseHandlers = {}) {
  let es: EventSource | null = null;

  function connect(lastEventId?: string) {
    disconnect();
    const url = `/api/networks/${encodeURIComponent(networkId)}/stream` + (lastEventId ? `?lastEventId=${encodeURIComponent(lastEventId)}` : "");
    es = new EventSource(url);

    es.addEventListener("open", () => {});

    es.addEventListener("snapshot", (ev: MessageEvent) => {
      try {
        const payload = JSON.parse(ev.data);
        handlers.onSnapshot?.(payload);
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.addEventListener("heartbeat", (ev: MessageEvent) => {
      handlers.onHeartbeat?.(ev.data as string);
    });

    es.addEventListener("stream-warning", (ev: MessageEvent) => {
      try {
        handlers.onWarning?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onWarning?.(ev.data);
      }
    });

    es.addEventListener("agent.deployed", (ev: MessageEvent) => {
      try {
        handlers.onAgentDeployed?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.addEventListener("agent.undeployed", (ev: MessageEvent) => {
      try {
        handlers.onAgentUndeployed?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.addEventListener("relationship.created", (ev: MessageEvent) => {
      try {
        handlers.onRelationshipCreated?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.addEventListener("relationship.deleted", (ev: MessageEvent) => {
      try {
        handlers.onRelationshipDeleted?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.addEventListener("version.deployed", (ev: MessageEvent) => {
      try {
        handlers.onVersionDeployed?.(JSON.parse(ev.data));
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    es.onerror = (err) => {
      // EventSource emits error during normal reconnect cycles; only bubble hard-closed states.
      if (es && es.readyState === EventSource.CONNECTING) {
        return;
      }
      handlers.onError?.(err);
    };
  }

  function disconnect() {
    if (es) {
      try {
        es.close();
      } catch (e) {
        // ignore
      }
      es = null;
    }
  }

  return { connect, disconnect };
}
