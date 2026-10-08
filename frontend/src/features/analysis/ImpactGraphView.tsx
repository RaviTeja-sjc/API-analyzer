import React, { useCallback, useEffect } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  MarkerType,
  BackgroundVariant
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './ImpactGraphView.css';
// @ts-ignore
import dagre from 'dagre';

interface ImpactGraphViewProps {
  graphData?: {
    nodes: any[];
    edges: any[];
  };
}

const emptyNodes: any[] = [];
const emptyEdges: any[] = [];

export const ImpactGraphView: React.FC<ImpactGraphViewProps> = ({ graphData }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState<any>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<any>([]);

  useEffect(() => {
    if (graphData && graphData.nodes && graphData.nodes.length > 0) {
      const g = new dagre.graphlib.Graph();
      g.setGraph({ rankdir: 'LR' });
      g.setDefaultEdgeLabel(() => ({}));
      
      const nodeWidth = 200;
      const nodeHeight = 80;

      graphData.nodes.forEach((node) => {
        g.setNode(node.id, { width: nodeWidth, height: nodeHeight });
      });
      graphData.edges.forEach((edge) => {
        g.setEdge(edge.source, edge.target);
      });

      dagre.layout(g);

      const positionedNodes = graphData.nodes.map((n) => {
        const nodeWithPosition = g.node(n.id);
        const x = nodeWithPosition.x - nodeWidth / 2;
        const y = nodeWithPosition.y - nodeHeight / 2;
        
        let bgColor = 'rgba(30, 41, 59, 0.8)';
        let borderColor = 'rgba(148, 163, 184, 0.5)';
        let shadowColor = 'transparent';
        
        if (n.className === 'node-change') {
          bgColor = 'rgba(69, 10, 10, 0.9)';
          borderColor = '#ef4444';
          shadowColor = 'rgba(239, 68, 68, 0.6)';
        } else if (n.className === 'node-transitive') {
          bgColor = 'rgba(69, 26, 3, 0.9)';
          borderColor = '#f59e0b';
          shadowColor = 'rgba(245, 158, 11, 0.6)';
        } else if (n.className === 'node-endpoint') {
          bgColor = 'rgba(6, 78, 59, 0.9)';
          borderColor = '#10b981';
          shadowColor = 'rgba(16, 185, 129, 0.6)';
        } else {
          bgColor = 'rgba(15, 23, 42, 0.9)';
          borderColor = '#3b82f6';
          shadowColor = 'rgba(59, 130, 246, 0.4)';
        }

        return {
          ...n,
          position: { x, y },
          style: {
            background: bgColor,
            color: '#fff',
            border: `2px solid ${borderColor}`,
            borderRadius: '12px',
            padding: '15px 25px',
            fontFamily: '"Fira Code", monospace',
            fontSize: '14px',
            fontWeight: 'bold',
            boxShadow: `0 0 20px ${shadowColor}`,
            textShadow: '0 2px 4px rgba(0,0,0,0.5)',
            width: 200,
            backdropFilter: 'blur(8px)',
            transition: 'all 0.3s ease'
          }
        };
      });

      const formattedEdges = graphData.edges.map((e: any) => ({
        ...e,
        animated: true,
        style: { stroke: 'url(#edge-gradient)', strokeWidth: 3 },
        markerEnd: { type: MarkerType.ArrowClosed, color: '#ec4899' }
      }));

      setNodes(positionedNodes);
      setEdges(formattedEdges);
    } else {
      setNodes(emptyNodes);
      setEdges(emptyEdges);
    }
  }, [graphData]);

  const onConnect = useCallback((params: any) => setEdges((eds) => addEdge(params, eds)), [setEdges]);

  if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
    return (
      <div style={{ width: '100%', height: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '16px', border: '1px dashed rgba(255,255,255,0.2)', color: '#94a3b8', background: 'rgba(0,0,0,0.2)' }}>
        No dependency graph data generated. Run "Analyze Repository" to build the graph.
      </div>
    );
  }

  return (
    <div style={{ width: '100%', height: '600px', borderRadius: '16px', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.1)', background: '#020617', boxShadow: '0 20px 40px rgba(0,0,0,0.6)' }}>
      <svg style={{ position: 'absolute', width: 0, height: 0 }}>
        <defs>
          <linearGradient id="edge-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#6366f1" />
            <stop offset="100%" stopColor="#ec4899" />
          </linearGradient>
        </defs>
      </svg>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        fitView
        colorMode="dark"
      >
        <Controls style={{ background: 'rgba(15,23,42,0.8)', border: 'none', borderRadius: '8px' }} />
        <MiniMap 
          nodeStrokeWidth={3} 
          nodeColor={(n) => {
            if (n.className === 'node-change') return '#ef4444';
            if (n.className === 'node-transitive') return '#f59e0b';
            if (n.className === 'node-endpoint') return '#10b981';
            return '#3b82f6';
          }}
          maskColor="rgba(2, 6, 23, 0.7)"
          style={{ background: 'rgba(15,23,42,0.8)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
        />
        <Background gap={24} size={2} color="#1e293b" variant={BackgroundVariant.Dots} />
      </ReactFlow>
    </div>
  );
};
