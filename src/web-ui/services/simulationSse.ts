import type { SimulationTimelineEventSnapshot } from "../types/api";

export type SimulationSseHandlers = {
  onConnected?: (info: { id: string }) => void;
  onEvent?: (event: SimulationTimelineEventSnapshot) => void;
  onError?: (err: any) => void;
};

export function createSimulationSse(runId: string, handlers: SimulationSseHandlers = {}) {
  let es: EventSource | null = null;

  function connect() {
    disconnect();
    const url = `/api/simulations/${encodeURIComponent(runId)}/stream`;
    es = new EventSource(url);

    es.addEventListener("open", () => {});

    // The in-memory adapter sends a CONNECTED event first
    es.addEventListener("CONNECTED", (ev: MessageEvent) => {
      try {
        const payload = JSON.parse(ev.data);
        handlers.onConnected?.(payload);
      } catch (e) {
        handlers.onError?.(e);
      }
    });

    // Generic timeline events - event name corresponds to snapshot.eventType
    es.addEventListener("message", (ev: MessageEvent) => {
      try {
        const parsed = JSON.parse(ev.data) as SimulationTimelineEventSnapshot;
        handlers.onEvent?.(parsed);
      } catch (e) {
        // ignore parse errors but surface if consumer wants
        handlers.onError?.(e);
      }
    });

    es.onerror = (err) => {
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
