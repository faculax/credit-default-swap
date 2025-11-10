import React, { useState, useEffect } from 'react';
import LineageGraph from '../components/lineage/LineageGraph';
import lineageService, { LineageEvent, LineageNode } from '../services/lineageService';

const LineagePage: React.FC = () => {
  const [datasets, setDatasets] = useState<string[]>([]);
  const [selectedDataset, setSelectedDataset] = useState<string>('');
  const [selectedRunId, setSelectedRunId] = useState<string>('');
  const [lineageEvents, setLineageEvents] = useState<LineageEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedNode, setSelectedNode] = useState<LineageNode | null>(null);
  const [searchType, setSearchType] = useState<'dataset' | 'run'>('dataset');

  // Load available datasets on mount
  useEffect(() => {
    const fetchDatasets = async () => {
      try {
        const datasetList = await lineageService.getAllDatasets();
        setDatasets(datasetList);
      } catch (err) {
        console.error('Failed to load datasets:', err);
      }
    };
    fetchDatasets();
  }, []);

  // Fetch lineage data when selection changes
  const fetchLineageData = async () => {
    if (!selectedDataset && !selectedRunId) {
      setError('Please select a dataset or enter a run ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      let events: LineageEvent[] = [];
      
      if (searchType === 'dataset' && selectedDataset) {
        events = await lineageService.getLineageByDataset(selectedDataset);
      } else if (searchType === 'run' && selectedRunId) {
        events = await lineageService.getLineageByRun(selectedRunId);
      }

      setLineageEvents(events);
      
      if (events.length === 0) {
        setError('No lineage events found');
      }
    } catch (err) {
      setError(`Failed to fetch lineage: ${err}`);
      setLineageEvents([]);
    } finally {
      setLoading(false);
    }
  };

  const handleNodeClick = (node: LineageNode) => {
    setSelectedNode(node);
  };

  const clearSelection = () => {
    setSelectedNode(null);
  };

  const lineageGraph = lineageService.transformToGraph(lineageEvents);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
        <h1 className="text-3xl font-bold text-fd-text mb-2">Data Lineage Explorer</h1>
        <p className="text-fd-text-muted">Visualize data flow and transformations across CDS operations</p>
      </div>

      {/* Controls */}
      <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
        <div className="space-y-4">
          {/* Search Type Selector */}
          <div className="flex items-center space-x-6">
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="radio"
                value="dataset"
                checked={searchType === 'dataset'}
                onChange={(e) => setSearchType(e.target.value as 'dataset' | 'run')}
                className="text-fd-green focus:ring-fd-green"
              />
              <span className="text-fd-text font-medium">Search by Dataset</span>
            </label>
            <label className="flex items-center space-x-2 cursor-pointer">
              <input
                type="radio"
                value="run"
                checked={searchType === 'run'}
                onChange={(e) => setSearchType(e.target.value as 'dataset' | 'run')}
                className="text-fd-green focus:ring-fd-green"
              />
              <span className="text-fd-text font-medium">Search by Run ID</span>
            </label>
          </div>

          {/* Search Inputs */}
          <div className="flex items-end space-x-4">
            {searchType === 'dataset' ? (
              <div className="flex-1">
                <label htmlFor="dataset-select" className="block text-sm font-medium text-fd-text mb-2">
                  Dataset
                </label>
                <select
                  id="dataset-select"
                  value={selectedDataset}
                  onChange={(e) => setSelectedDataset(e.target.value)}
                  disabled={loading}
                  className="w-full px-4 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent disabled:opacity-50"
                >
                  <option value="">Select a dataset...</option>
                  {datasets.map((dataset) => (
                    <option key={dataset} value={dataset}>
                      {dataset}
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="flex-1">
                <label htmlFor="run-id-input" className="block text-sm font-medium text-fd-text mb-2">
                  Run ID
                </label>
                <input
                  id="run-id-input"
                  type="text"
                  value={selectedRunId}
                  onChange={(e) => setSelectedRunId(e.target.value)}
                  placeholder="Enter run ID..."
                  disabled={loading}
                  className="w-full px-4 py-2 bg-fd-dark border border-fd-border rounded-md text-fd-text placeholder-fd-text-muted focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent disabled:opacity-50"
                />
              </div>
            )}

            <button
              onClick={fetchLineageData}
              disabled={loading || (!selectedDataset && !selectedRunId)}
              className="px-6 py-2 bg-fd-green text-fd-dark font-semibold rounded-md hover:bg-fd-green/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? 'Loading...' : 'Fetch Lineage'}
            </button>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-900/20 border border-red-500 rounded-lg p-4">
          <p className="text-red-400">{error}</p>
        </div>
      )}

      {/* Stats Cards */}
      {lineageEvents.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-fd-text-muted text-sm font-medium mb-2">Total Events</div>
            <div className="text-fd-green text-3xl font-bold">{lineageEvents.length}</div>
          </div>
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-fd-text-muted text-sm font-medium mb-2">Unique Operations</div>
            <div className="text-fd-green text-3xl font-bold">
              {new Set(lineageEvents.map((e) => e.operation)).size}
            </div>
          </div>
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-fd-text-muted text-sm font-medium mb-2">Graph Complexity</div>
            <div className="text-fd-green text-3xl font-bold">
              {lineageGraph.nodes.length} nodes, {lineageGraph.edges.length} edges
            </div>
          </div>
        </div>
      )}

      {/* Debug Info - Collapsible */}
      {lineageEvents.length > 0 && (
        <details className="bg-fd-darker border border-fd-border rounded-lg p-6">
          <summary className="text-fd-text font-bold cursor-pointer hover:text-fd-green">
            🔍 Debug: View Raw Lineage Data (Click to expand)
          </summary>
          <div className="mt-4 space-y-4">
            <div className="bg-fd-dark rounded p-4">
              <h4 className="text-fd-green font-bold mb-2">Latest Event Inputs:</h4>
              <pre className="text-fd-text text-xs overflow-auto">
                {JSON.stringify(lineageEvents[0]?.inputs, null, 2)}
              </pre>
            </div>
            <div className="bg-fd-dark rounded p-4">
              <h4 className="text-fd-green font-bold mb-2">Latest Event Outputs:</h4>
              <pre className="text-fd-text text-xs overflow-auto">
                {JSON.stringify(lineageEvents[0]?.outputs, null, 2)}
              </pre>
            </div>
            <div className="bg-fd-dark rounded p-4">
              <h4 className="text-fd-green font-bold mb-2">Graph Nodes ({lineageGraph.nodes.length}):</h4>
              <pre className="text-fd-text text-xs overflow-auto max-h-60">
                {JSON.stringify(lineageGraph.nodes.map(n => ({ id: n.id, label: n.label, type: n.type })), null, 2)}
              </pre>
            </div>
            <div className="bg-fd-dark rounded p-4">
              <h4 className="text-fd-green font-bold mb-2">Graph Edges ({lineageGraph.edges.length}):</h4>
              <pre className="text-fd-text text-xs overflow-auto max-h-60">
                {JSON.stringify(lineageGraph.edges, null, 2)}
              </pre>
            </div>
          </div>
        </details>
      )}

      {/* Lineage Graph */}
      {lineageEvents.length > 0 && (
        <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
          <h2 className="text-xl font-bold text-fd-text mb-4">Lineage Graph</h2>
          <div className="bg-white rounded-lg">
            <LineageGraph lineageData={lineageGraph} onNodeClick={handleNodeClick} />
          </div>
        </div>
      )}

      {/* Node Details Modal */}
      {selectedNode && (
        <div 
          className="fixed inset-0 bg-black/70 flex items-center justify-center z-50"
          onClick={clearSelection}
          role="dialog"
          aria-modal="true"
          onKeyDown={(e) => e.key === 'Escape' && clearSelection()}
          tabIndex={-1}
        >
          <div 
            className="bg-fd-darker border border-fd-border rounded-lg max-w-2xl w-full max-h-[80vh] overflow-y-auto m-4"
            onClick={(e) => e.stopPropagation()}
            role="document"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Escape' && clearSelection()}
          >
            <div className="flex items-center justify-between p-6 border-b border-fd-border">
              <h3 className="text-xl font-bold text-fd-text">{selectedNode.label}</h3>
              <button
                onClick={clearSelection}
                className="text-fd-text-muted hover:text-fd-green text-3xl leading-none"
              >
                ×
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <span className="text-fd-green font-medium">Type:</span>
                  <span className="text-fd-text">{selectedNode.type}</span>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-fd-green font-medium">ID:</span>
                  <span className="text-fd-text font-mono text-sm">{selectedNode.id}</span>
                </div>
              </div>
              {selectedNode.metadata && (
                <>
                  <h4 className="text-lg font-semibold text-fd-text mt-4">Metadata</h4>
                  <pre className="bg-fd-dark border border-fd-border rounded-lg p-4 text-fd-text text-sm overflow-x-auto">
                    {JSON.stringify(selectedNode.metadata, null, 2)}
                  </pre>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Events Table */}
      {lineageEvents.length > 0 && (
        <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
          <h2 className="text-xl font-bold text-fd-text mb-4">Lineage Events</h2>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-fd-border">
                  <th className="text-left py-3 px-4 text-fd-text font-semibold">Timestamp</th>
                  <th className="text-left py-3 px-4 text-fd-text font-semibold">Dataset</th>
                  <th className="text-left py-3 px-4 text-fd-text font-semibold">Operation</th>
                  <th className="text-left py-3 px-4 text-fd-text font-semibold">Run ID</th>
                  <th className="text-left py-3 px-4 text-fd-text font-semibold">User</th>
                </tr>
              </thead>
              <tbody>
                {lineageEvents.map((event) => (
                  <tr key={event.id} className="border-b border-fd-border/50 hover:bg-fd-dark/50">
                    <td className="py-3 px-4 text-fd-text-muted text-sm">
                      {new Date(event.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4">
                      <code className="text-fd-green text-sm bg-fd-dark px-2 py-1 rounded">
                        {event.dataset}
                      </code>
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-block px-3 py-1 bg-fd-green/20 text-fd-green text-xs font-semibold rounded-full">
                        {event.operation}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <code className="text-fd-text-muted text-xs">{event.runId}</code>
                    </td>
                    <td className="py-3 px-4 text-fd-text">{event.userName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default LineagePage;
