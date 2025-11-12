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
      const isEndpoint = node.type === 'endpoint';
      const isService = node.type === 'service';
      const isRepository = node.type === 'repository';
      
      const displayLabel = node.label.length > 25 
        ? node.label.substring(0, 22) + '...' 
        : node.label;
      
      // Get icon based on type
      const getIcon = () => {
        if (isDataset) return '📊';
        if (isEndpoint) return '🌐';
        if (isService) return '⚙️';
        if (isRepository) return '💾';
        return '🔹';
      };
      
      // Use only the existing color palette, no gradients
      const getColors = () => {
        if (isDataset) return {
          background: '#1ee6be', // RGB(30, 230, 190)
          color: '#3c4b61',      // RGB(60, 75, 97)
          border: '#00ffc3'      // RGB(0, 255, 195)
        };
        if (isEndpoint) return {
          background: '#00e8f7', // RGB(0, 232, 247)
          color: '#3c4b61',
          border: '#00f000'      // RGB(0, 240, 0)
        };
        if (isService) return {
          background: '#3c4b61',
          color: '#fff',
          border: '#00e8f7'
        };
        if (isRepository) return {
          background: '#3c4b61',
          color: '#1ee6be',
          border: '#1ee6be'
        };
        return {
          background: '#3c4b61',
          color: '#fff',
          border: '#00e8f7'
        };
      };

      const colors = getColors();

      return {
        id: node.id,
        data: { 
          label: (
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                {getIcon()} {displayLabel}
              </div>
              {node.metadata && Object.keys(node.metadata).length > 0 && (
                <div style={{ fontSize: '10px', opacity: 0.8 }}>
                  {node.type.toUpperCase()}
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
          background: colors.background,
          color: colors.color,
          border: `2px solid ${colors.border}`,
          borderRadius: '10px',
          padding: '14px',
          minWidth: 180,
          fontSize: '13px',
          fontWeight: '500',
          boxShadow: '0 2px 8px rgba(60,75,97,0.10)',
          transition: 'all 0.3s ease'
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
      style: { stroke: '#00e8f7', strokeWidth: 3 },
      labelStyle: { 
        fill: '#3c4b61', // RGB(60, 75, 97)
        fontWeight: 700,
        fontSize: '11px',
        background: '#fff',
        padding: '4px 8px',
        borderRadius: '4px',
        border: '1px solid #00e8f7'
      },
      labelBgPadding: [8, 4] as [number, number],
      labelBgBorderRadius: 4,
      labelBgStyle: { 
        fill: '#fff',
        fillOpacity: 0.95,
        stroke: '#00e8f7',
        strokeWidth: 2
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: '#00e8f7'
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
  <div style={{ width: '100%', height: '700px', background: '#fff' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={handleNodeClick}
        fitView
        fitViewOptions={{ padding: 0.3, maxZoom: 1 }}
        attributionPosition="bottom-left"
        minZoom={0.1}
        maxZoom={2}
      >
        <Controls 
          style={{
            background: 'rgb(60, 75, 97)',
            border: '1px solid rgb(0, 240, 0)',
            borderRadius: '8px'
          }}
        />
        <MiniMap 
          nodeColor={(node: any) => {
            const nodeData = node.data as { type: string };
            switch(nodeData.type) {
              case 'dataset': return 'rgb(30, 230, 190)';
              case 'endpoint': return 'rgb(0, 232, 247)';
              case 'service': return 'rgb(60, 75, 97)';
              case 'repository': return 'rgb(60, 75, 97)';
              default: return 'rgb(100, 100, 100)';
            }
          }}
          style={{ 
            background: 'rgb(255, 255, 255)',
            border: '1px solid rgb(0, 240, 0)',
            borderRadius: '8px'
          }}
          maskColor="rgba(60, 75, 97, 0.1)"
        />
        <Background 
          color="rgba(60, 75, 97, 0.15)" 
          gap={24} 
          size={1}
        />
      </ReactFlow>
    </div>
  );
};
export default LineageGraph;
