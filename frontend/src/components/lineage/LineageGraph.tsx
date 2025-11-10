import React, { useEffect } from 'react';
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType,
  Position
} from 'reactflow';
import 'reactflow/dist/style.css';
import type { LineageGraph as LineageGraphType, LineageNode as LineageNodeType } from '../../services/lineageService';

interface LineageGraphProps {
  lineageData: LineageGraphType;
  onNodeClick?: (node: LineageNodeType) => void;
}

// Calculate automatic layout using Dagre algorithm (simple version)
const getLayoutedElements = (nodes: Node[], edges: Edge[]) => {
  const horizontalSpacing = 300;
  const verticalSpacing = 100;
  const nodeHeight = 80;

  // Simple layered layout
  const inDegree = new Map<string, number>();
  const layers: string[][] = [];
  
  // Calculate in-degrees
  for (const node of nodes) {
    inDegree.set(node.id, 0);
  }
  for (const edge of edges) {
    inDegree.set(edge.target, (inDegree.get(edge.target) || 0) + 1);
  }

  // Topological sort to create layers
  let currentLayer = nodes.filter(n => inDegree.get(n.id) === 0).map(n => n.id);
  const processed = new Set<string>();

  while (currentLayer.length > 0) {
    layers.push([...currentLayer]);
    const nextLayer: string[] = [];

    for (const nodeId of currentLayer) {
      processed.add(nodeId);
      const outgoingEdges = edges.filter(e => e.source === nodeId);
      
      for (const edge of outgoingEdges) {
        const targetDegree = (inDegree.get(edge.target) || 0) - 1;
        inDegree.set(edge.target, targetDegree);
        
        if (targetDegree === 0 && !processed.has(edge.target)) {
          nextLayer.push(edge.target);
        }
      }
    }

    currentLayer = nextLayer;
  }

  // Handle any remaining nodes (cycles)
  for (const node of nodes) {
    if (!processed.has(node.id)) {
      if (layers.length === 0) layers.push([]);
      layers.at(-1)!.push(node.id);
    }
  }

  // Position nodes
  const layoutedNodes = nodes.map(node => {
    const layerIndex = layers.findIndex(layer => layer.includes(node.id));
    const positionInLayer = layers[layerIndex].indexOf(node.id);
    const layerSize = layers[layerIndex].length;

    return {
      ...node,
      position: {
        x: layerIndex * horizontalSpacing,
        y: positionInLayer * (nodeHeight + verticalSpacing) - (layerSize * (nodeHeight + verticalSpacing)) / 2
      }
    };
  });

  return { nodes: layoutedNodes, edges };
};

const LineageGraph: React.FC<LineageGraphProps> = ({ lineageData, onNodeClick }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  useEffect(() => {
    // Transform lineage data to ReactFlow format
    const flowNodes: Node[] = lineageData.nodes.map((node: LineageNodeType) => {
      const isDataset = node.type === 'dataset';
      const displayLabel = node.label.length > 25 
        ? node.label.substring(0, 22) + '...' 
        : node.label;
      
      return {
        id: node.id,
        data: { 
          label: (
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                {displayLabel}
              </div>
              {node.metadata && Object.keys(node.metadata).length > 0 && (
                <div style={{ fontSize: '10px', opacity: 0.8 }}>
                  {isDataset ? '📊 Dataset' : '⚙️ Operation'}
                </div>
              )}
            </div>
          ),
          metadata: node.metadata,
          type: node.type,
          fullLabel: node.label
        },
        position: { x: 0, y: 0 },
        style: {
          background: isDataset ? 'rgb(30, 230, 190)' : 'rgb(60, 75, 97)',
          color: isDataset ? 'rgb(60, 75, 97)' : 'rgb(255, 255, 255)',
          border: isDataset ? '2px solid rgb(0, 255, 195)' : '2px solid rgb(0, 232, 247)',
          borderRadius: '8px',
          padding: '12px',
          minWidth: 180,
          fontSize: '12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
        },
        sourcePosition: Position.Right,
        targetPosition: Position.Left
      };
    });

    const flowEdges: Edge[] = lineageData.edges.map((edge: any, index: number) => ({
      id: `edge-${index}`,
      source: edge.source,
      target: edge.target,
      label: edge.label,
      type: 'smoothstep',
      animated: true,
      style: { stroke: 'rgb(0, 240, 0)', strokeWidth: 2 },
      labelStyle: { 
        fill: 'rgb(60, 75, 97)', 
        fontWeight: 700,
        fontSize: '11px',
        background: 'rgba(255, 255, 255, 0.9)',
        padding: '4px 8px',
        borderRadius: '4px',
        border: '1px solid rgb(0, 240, 0)'
      },
      labelBgPadding: [8, 4] as [number, number],
      labelBgBorderRadius: 4,
      labelBgStyle: { 
        fill: 'rgba(255, 255, 255, 0.9)', 
        fillOpacity: 0.9,
        stroke: 'rgb(0, 240, 0)',
        strokeWidth: 1
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: 'rgb(0, 240, 0)'
      }
    }));

    // Apply automatic layout
    const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(flowNodes, flowEdges);
    
    setNodes(layoutedNodes);
    setEdges(layoutedEdges);
  }, [lineageData, setNodes, setEdges]);

  const handleNodeClick = (_: React.MouseEvent, node: Node) => {
    if (onNodeClick) {
      const lineageNode = lineageData.nodes.find((n: LineageNodeType) => n.id === node.id);
      if (lineageNode) {
        onNodeClick(lineageNode);
      }
    }
  };

  return (
    <div style={{ width: '100%', height: '600px', background: 'rgb(255, 255, 255)' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        fitView
        attributionPosition="bottom-left"
      >
        <Controls />
        <MiniMap 
          nodeColor={(node: any) => {
            const nodeData = node.data as { type: string };
            return nodeData.type === 'dataset' ? 'rgb(30, 230, 190)' : 'rgb(60, 75, 97)';
          }}
          style={{ background: 'rgb(255, 255, 255)' }}
        />
        <Background color="rgb(60, 75, 97)" gap={16} />
      </ReactFlow>
    </div>
  );
};

export default LineageGraph;
