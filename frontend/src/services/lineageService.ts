import axios from 'axios';

// Use relative URL to leverage the proxy in package.json (pointing to gateway:8081)
const API_BASE_URL = '/api/lineage';

export interface LineageEvent {
  id: string;
  dataset: string;
  operation: string;
  inputs: Record<string, any>;
  outputs: Record<string, any>;
  userName: string;
  runId: string;
  createdAt: string;
}

export interface LineageNode {
  id: string;
  label: string;
  type: 'dataset' | 'operation';
  metadata?: Record<string, any>;
}

export interface LineageEdge {
  source: string;
  target: string;
  label?: string;
}

export interface LineageGraph {
  nodes: LineageNode[];
  edges: LineageEdge[];
}

class LineageService {
  /**
   * Fetch all lineage events for a specific dataset
   */
  async getLineageByDataset(dataset: string): Promise<LineageEvent[]> {
    const response = await axios.get(API_BASE_URL, {
      params: { dataset }
    });
    return response.data;
  }

  /**
   * Fetch lineage events for a specific run
   */
  async getLineageByRun(runId: string): Promise<LineageEvent[]> {
    const response = await axios.get(`${API_BASE_URL}/run/${runId}`);
    return response.data;
  }

  /**
   * Get all unique datasets from lineage events
   */
  async getAllDatasets(): Promise<string[]> {
    try {
      const response = await axios.get(`${API_BASE_URL}/datasets`);
      return response.data;
    } catch (error) {
      console.error('Failed to fetch datasets:', error);
      return [];
    }
  }

  /**
   * Transform lineage events into a graph structure
   */
  transformToGraph(events: LineageEvent[]): LineageGraph {
    const nodes: LineageNode[] = [];
    const edges: LineageEdge[] = [];
    const nodeMap = new Map<string, LineageNode>();

    events.forEach(event => {
      // Create operation node
      const operationNodeId = `${event.operation}-${event.id}`;
      if (!nodeMap.has(operationNodeId)) {
        const operationNode: LineageNode = {
          id: operationNodeId,
          label: event.operation,
          type: 'operation',
          metadata: {
            runId: event.runId,
            userName: event.userName,
            createdAt: event.createdAt,
            inputs: event.inputs,
            outputs: event.outputs
          }
        };
        nodes.push(operationNode);
        nodeMap.set(operationNodeId, operationNode);
      }

      // Process inputs - extract datasets from nested structure
      if (event.inputs && typeof event.inputs === 'object') {
        Object.entries(event.inputs).forEach(([inputKey, inputValue]: [string, any]) => {
          if (inputValue && typeof inputValue === 'object' && inputValue.dataset) {
            const inputDatasetName = inputValue.dataset;
            const inputNodeId = `dataset-${inputDatasetName}`;
            
            if (!nodeMap.has(inputNodeId)) {
              const inputNode: LineageNode = {
                id: inputNodeId,
                label: inputDatasetName,
                type: 'dataset',
                metadata: inputValue
              };
              nodes.push(inputNode);
              nodeMap.set(inputNodeId, inputNode);
            }
            
            edges.push({
              source: inputNodeId,
              target: operationNodeId,
              label: inputKey.replace(/_/g, ' ')
            });
          }
        });
      }

      // Process outputs - extract datasets from nested structure
      if (event.outputs && typeof event.outputs === 'object') {
        Object.entries(event.outputs).forEach(([outputKey, outputValue]: [string, any]) => {
          if (outputValue && typeof outputValue === 'object' && outputValue.dataset) {
            const outputDatasetName = outputValue.dataset;
            const outputNodeId = `dataset-${outputDatasetName}`;
            
            if (!nodeMap.has(outputNodeId)) {
              const outputNode: LineageNode = {
                id: outputNodeId,
                label: outputDatasetName,
                type: 'dataset',
                metadata: outputValue
              };
              nodes.push(outputNode);
              nodeMap.set(outputNodeId, outputNode);
            }
            
            edges.push({
              source: operationNodeId,
              target: outputNodeId,
              label: outputKey.replace(/_/g, ' ')
            });
          }
        });
      }

      // Fallback: if no datasets found in inputs/outputs, link to main dataset
      if (edges.filter(e => e.source === operationNodeId || e.target === operationNodeId).length === 0) {
        const datasetNodeId = `dataset-${event.dataset}`;
        if (!nodeMap.has(datasetNodeId)) {
          const datasetNode: LineageNode = {
            id: datasetNodeId,
            label: event.dataset,
            type: 'dataset'
          };
          nodes.push(datasetNode);
          nodeMap.set(datasetNodeId, datasetNode);
        }
        edges.push({
          source: operationNodeId,
          target: datasetNodeId,
          label: 'produces'
        });
      }
    });

    return { nodes, edges };
  }

  /**
   * Create a lineage event
   */
  async createLineageEvent(event: Omit<LineageEvent, 'id' | 'createdAt'>): Promise<LineageEvent> {
    const response = await axios.post(API_BASE_URL, event);
    return response.data;
  }
}

export default new LineageService();
