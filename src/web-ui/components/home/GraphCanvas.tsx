import { useEffect, useMemo, useRef, useState } from "react";
import { useSelector } from "react-redux";

import { selectDeployedAgents } from "@/store/slices/agents-slice";
import { selectAllRelationships } from "@/store/slices/relationships-slice";

type GraphCanvasProps = {
  networkId: string;
};

interface GraphNode {
  id: string;
  name: string;
  val: number;
}

interface GraphLink {
  source: string;
  target: string;
  relationshipType: string;
}

interface GraphData {
  nodes: GraphNode[];
  links: GraphLink[];
}

export function GraphCanvas({ networkId }: GraphCanvasProps) {
  const fgRef = useRef<any>(null);
  const [ForceGraph3D, setForceGraph3D] = useState<null | React.ComponentType<any>>(null);
  const deployedAgents = useSelector(selectDeployedAgents);
  const relationships = useSelector(selectAllRelationships);
  const initialCameraDistance = 260;
  const initializedRef = useRef(false);

  const graphData = useMemo<GraphData>(() => {
    const nodes: GraphNode[] = deployedAgents.map((agent) => ({
      id: agent.agentId,
      name: agent.nickname,
      val: 15,
    }));

    const deployedAgentIdSet = new Set(deployedAgents.map((agent) => agent.agentId));
    const links: GraphLink[] = relationships
      .filter(
        (rel) => deployedAgentIdSet.has(rel.sourceAgentId) && deployedAgentIdSet.has(rel.targetAgentId)
      )
      .map((rel) => ({
        source: rel.sourceAgentId,
        target: rel.targetAgentId,
        relationshipType: rel.relationshipType,
      }));

    return { nodes, links };
  }, [deployedAgents, relationships, networkId]);

  useEffect(() => {
    if (!ForceGraph3D || !fgRef.current) return;

    try {
      if (typeof fgRef.current.graphData === "function") {
        requestAnimationFrame(() => fgRef.current.graphData(graphData));
      }
    } catch (e) {
      // If the instance doesn't support graphData yet, let the prop update path handle it.
      // We intentionally swallow errors here to avoid noisy console logs from transient states.
    }
  }, [graphData, ForceGraph3D]);

  useEffect(() => {
    let cancelled = false;

    const loadGraph = async () => {
      const module = await import("react-force-graph-3d");
      if (!cancelled) {
        setForceGraph3D(() => module.default);
      }
    };

    loadGraph();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="relative h-full w-full">
      {graphData.nodes.length === 0 ? (
        <div className="absolute inset-0 flex items-center justify-center bg-surface">
          <div className="rounded-lg border border-outline-variant bg-surface-container-lowest/90 p-md shadow-[0_2px_0_rgba(13,28,45,0.1)]">
            <p className="text-label-caps text-on-surface-variant">Graph Visualization</p>
            <p className="text-body-md text-on-surface">
              Deploy agents to see them appear in the force-directed graph.
            </p>
          </div>
        </div>
      ) : ForceGraph3D ? (
        <ForceGraph3D
          ref={fgRef}
          graphData={graphData}
          nodeAutoColorBy="id"
          nodeLabel={(node: GraphNode) => `${node.name} (${node.id.slice(0, 8)}...)`}
          linkColor={() => "rgba(144,111,112,0.3)"}
          linkWidth={1.5}
          linkDistance={110}
          linkLabel={(link: GraphLink) => link.relationshipType}
          backgroundColor="#f8f9ff"
          onEngineStop={() => {
            // Only set the initial camera once after the engine finishes layout for the first time.
            if (fgRef.current && !initializedRef.current) {
              fgRef.current.cameraPosition(
                { x: 0, y: 0, z: initialCameraDistance },
                { x: 0, y: 0, z: 0 },
                0
              );
              initializedRef.current = true;
            }
          }}
        />
      ) : (
        <div className="absolute inset-0 flex items-center justify-center bg-surface">
          <div className="rounded-lg border border-outline-variant bg-surface-container-lowest/90 p-md shadow-[0_2px_0_rgba(13,28,45,0.1)]">
            <p className="text-label-caps text-on-surface-variant">Graph Loading</p>
            <p className="text-body-md text-on-surface">Loading 3D graph renderer...</p>
          </div>
        </div>
      )}
    </div>
  );
}
