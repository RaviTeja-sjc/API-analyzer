import React, { useCallback } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  MarkerType
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './ImpactGraphView.css';

const initialNodes = [
  { id: 'change_1', position: { x: 50, y: 50 }, data: { label: 'API Change (PARAMETER_REMOVED)' }, className: 'node-change' },
  { id: 'ep_1', position: { x: 350, y: 50 }, data: { label: 'Endpoint: GET /api/v1/payments' }, className: 'node-endpoint' },
  { id: 'class_1', position: { x: 650, y: -20 }, data: { label: 'Class: PaymentClient' }, className: 'node-consumer' },
  { id: 'method_1', position: { x: 650, y: 120 }, data: { label: 'Method: fetchPayment()' }, className: 'node-consumer' },
  { id: 'method_2', position: { x: 950, y: 120 }, data: { label: 'Method: PaymentService.process()' }, className: 'node-transitive' },
];

const initialEdges = [
  { id: 'e1', source: 'change_1', target: 'ep_1', animated: true, style: { stroke: '#ef4444', strokeWidth: 2 }, markerEnd: { type: MarkerType.ArrowClosed, color: '#ef4444' } },
  { id: 'e2', source: 'ep_1', target: 'class_1', label: '100% Confirmed', style: { stroke: '#3b82f6' }, markerEnd: { type: MarkerType.ArrowClosed, color: '#3b82f6' } },
  { id: 'e3', source: 'ep_1', target: 'method_1', label: '100% Confirmed', style: { stroke: '#3b82f6' }, markerEnd: { type: MarkerType.ArrowClosed, color: '#3b82f6' } },
  { id: 'e4', source: 'method_1', target: 'method_2', label: 'Heuristic (75%)', animated: true, style: { stroke: '#f59e0b', strokeDasharray: '5,5' }, markerEnd: { type: MarkerType.ArrowClosed, color: '#f59e0b' } },
];

export const ImpactGraphView: React.FC = () => {
  const [nodes, , onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback((params: any) => setEdges((eds) => addEdge(params, eds)), [setEdges]);

  return (
    <div style={{ width: '100%', height: '600px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        fitView
        colorMode="dark"
      >
        <Controls />
        <MiniMap nodeStrokeWidth={3} nodeColor={(n) => {
          if (n.className === 'node-change') return '#ef4444';
          if (n.className === 'node-transitive') return '#f59e0b';
          return '#3b82f6';
        }} />
        <Background gap={12} size={1} />
      </ReactFlow>
    </div>
  );
};
