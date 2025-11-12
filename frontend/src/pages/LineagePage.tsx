import React, { useState, useEffect } from 'react';
import axios from 'axios';
import LineageGraph from '../components/lineage/LineageGraph';
import lineageService, { LineageEvent, LineageNode, LineageGraph as LineageGraphType } from '../services/lineageService';

const LineagePage: React.FC = () => {
  const [datasets, setDatasets] = useState<string[]>([]);
  const [selectedDataset, setSelectedDataset] = useState<string>('');
  const [selectedRunId, setSelectedRunId] = useState<string>('');
  const [lineageEvents, setLineageEvents] = useState<LineageEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedNode, setSelectedNode] = useState<LineageNode | null>(null);
  const [searchType, setSearchType] = useState<'dataset' | 'run' | 'correlation' | 'recent'>('correlation');
  const [correlationId, setCorrelationId] = useState<string>('');
  const [useGraphAPI, setUseGraphAPI] = useState(true);
  const [lineageGraph, setLineageGraph] = useState<LineageGraphType>({ nodes: [], edges: [] });
  const [activeTab, setActiveTab] = useState<'graph' | 'intelligence'>('graph');
  const [intelligenceSubTab, setIntelligenceSubTab] = useState<'path-detail' | 'origin' | 'transformations' | 'consumers' | 'metadata'>('path-detail');
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [selectedEventForIntelligence, setSelectedEventForIntelligence] = useState<LineageEvent | null>(null);
  const [eventFilter, setEventFilter] = useState<string>('');
  const [operationFilter, setOperationFilter] = useState<string>('all');
  const [datasetFilter, setDatasetFilter] = useState<string>('all');
  const [fullscreenMode, setFullscreenMode] = useState<'none' | 'graph' | 'intelligence' | 'events'>('none');

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

  // Handle ESC key to exit fullscreen
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && fullscreenMode !== 'none') {
        setFullscreenMode('none');
      }
    };
    window.addEventListener('keydown', handleEscKey);
    return () => window.removeEventListener('keydown', handleEscKey);
  }, [fullscreenMode]);

  // Fetch lineage data when selection changes
  const fetchLineageData = async () => {
    if (!selectedDataset && !selectedRunId && !correlationId && searchType !== 'recent') {
      setError('Please select a dataset, enter a run ID, or correlation ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (useGraphAPI) {
        let graph: LineageGraphType;
        let events: LineageEvent[] = [];
        
        if (searchType === 'dataset' && selectedDataset) {
          graph = await lineageService.getGraphForDataset(selectedDataset);
          events = await lineageService.getLineageByDataset(selectedDataset);
        } else if (searchType === 'correlation' && correlationId) {
          graph = await lineageService.getGraphForCorrelation(correlationId);
          const allEvents = await axios.get('/api/lineage');
          events = allEvents.data.filter((e: LineageEvent) => 
            e.outputs._correlation_id === correlationId
          );
        } else if (searchType === 'recent') {
          graph = await lineageService.getRecentActivityGraph(100);
          const recentEvents = await axios.get('/api/lineage');
          events = recentEvents.data.slice(0, 100);
        } else if (searchType === 'run' && selectedRunId) {
          graph = await lineageService.getGraphForRun(selectedRunId);
          events = await lineageService.getLineageByRun(selectedRunId);
        } else {
          events = [];
          graph = { nodes: [], edges: [] };
        }
        
        setLineageGraph(graph);
        setLineageEvents(events);
        
        if (graph.nodes.length === 0) {
          setError('No lineage data found');
        }
      } else {
        let events: LineageEvent[] = [];
        
        if (searchType === 'dataset' && selectedDataset) {
          events = await lineageService.getLineageByDataset(selectedDataset);
        } else if (searchType === 'run' && selectedRunId) {
          events = await lineageService.getLineageByRun(selectedRunId);
        }

        setLineageEvents(events);
        const graph = lineageService.transformToGraph(events);
        setLineageGraph(graph);
        
        if (events.length === 0) {
          setError('No lineage events found');
        }
      }
    } catch (err) {
      setError(`Failed to fetch lineage: ${err}`);
      setLineageEvents([]);
      setLineageGraph({ nodes: [], edges: [] });
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

  const loadEventLineage = async (event: LineageEvent) => {
    setLoading(true);
    setSelectedEventId(event.id);
    setSelectedEventForIntelligence(event);
    
    // Switch to intelligence tab to show the selected event's details
    setActiveTab('intelligence');
    
    try {
      const correlationIdToUse = event.outputs?._correlation_id || event.runId;
      
      if (correlationIdToUse && event.outputs?._correlation_id) {
        const graph = await lineageService.getGraphForCorrelation(correlationIdToUse);
        setLineageGraph(graph);
      } else if (event.runId) {
        const graph = await lineageService.getGraphForRun(event.runId);
        setLineageGraph(graph);
      }
    } catch (err) {
      console.error('Failed to load lineage for event:', err);
      setError(`Failed to load lineage: ${err}`);
    } finally {
      setLoading(false);
    }
  };

  // Get unique operations and datasets for filter dropdowns
  const uniqueOperations = Array.from(new Set(lineageEvents.map(e => e.operation)));
  const uniqueDatasets = Array.from(new Set(lineageEvents.map(e => e.dataset)));

  // Filter events based on search and dropdown filters, then sort by date descending (newest first)
  const filteredEvents = lineageEvents
    .filter(event => {
      const matchesSearch = eventFilter === '' || 
        event.dataset.toLowerCase().includes(eventFilter.toLowerCase()) ||
        event.operation.toLowerCase().includes(eventFilter.toLowerCase()) ||
        event.id.toLowerCase().includes(eventFilter.toLowerCase());
      
      const matchesOperation = operationFilter === 'all' || event.operation === operationFilter;
      const matchesDataset = datasetFilter === 'all' || event.dataset === datasetFilter;
      
      return matchesSearch && matchesOperation && matchesDataset;
    })
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <div className="min-h-screen bg-fd-dark flex flex-col">
      <div className="max-w-[1800px] mx-auto px-6 py-8 space-y-6 flex flex-col flex-1 w-full">
        {/* Header */}
        <div className="bg-fd-darker border border-fd-border rounded-lg p-6 text-center">
          <h1 className="text-4xl font-bold text-fd-green mb-3">
            Data Lineage Explorer
          </h1>
          <p className="text-fd-text-muted text-lg">
            Visualize data flow and transformations across your CDS operations
          </p>
        </div>

        {/* Search Controls - Compact Card */}
        <div className="bg-fd-darker border border-fd-border rounded-lg shadow-lg p-6">
          {/* Search Type Pills */}
          <div className="flex items-center justify-center gap-3 mb-6">
            {[
              { value: 'correlation', icon: '🔍', label: 'Correlation ID' },
              { value: 'dataset', icon: '📊', label: 'Dataset' },
              { value: 'run', icon: '🏃', label: 'Run ID' },
              { value: 'recent', icon: '⏱️', label: 'Recent' }
            ].map(({ value, icon, label }) => (
              <button
                key={value}
                onClick={() => setSearchType(value as any)}
                className={`px-6 py-3 rounded-lg font-medium transition-all duration-200 ${
                  searchType === value
                    ? 'bg-fd-green text-fd-dark'
                    : 'bg-fd-dark text-fd-text-muted hover:bg-fd-dark/70 hover:text-fd-text'
                }`}
              >
                <span className="mr-2">{icon}</span>
                {label}
              </button>
            ))}
          </div>

          {/* Search Input */}
          <div className="flex items-end gap-4">
            {searchType === 'correlation' && (
              <div className="flex-1">
                <label className="block text-sm font-medium text-fd-text mb-2">Correlation ID</label>
                <input
                  type="text"
                  value={correlationId}
                  onChange={(e) => setCorrelationId(e.target.value)}
                  placeholder="e.g., e3c41615-63bf-4e6c-8e50-2265884fee0b"
                  disabled={loading}
                  className="w-full px-4 py-3 bg-fd-dark border border-fd-border rounded-lg text-fd-text placeholder-fd-text-muted focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent disabled:opacity-50"
                />
              </div>
            )}
            {searchType === 'dataset' && (
              <div className="flex-1">
                <label className="block text-sm font-medium text-fd-text mb-2">Dataset</label>
                <select
                  value={selectedDataset}
                  onChange={(e) => setSelectedDataset(e.target.value)}
                  disabled={loading}
                  className="w-full px-4 py-3 bg-fd-dark border border-fd-border rounded-lg text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent disabled:opacity-50"
                >
                  <option value="">Select a dataset...</option>
                  {datasets.map((dataset) => (
                    <option key={dataset} value={dataset}>{dataset}</option>
                  ))}
                </select>
              </div>
            )}
            {searchType === 'run' && (
              <div className="flex-1">
                <label className="block text-sm font-medium text-fd-text mb-2">Run ID</label>
                <input
                  type="text"
                  value={selectedRunId}
                  onChange={(e) => setSelectedRunId(e.target.value)}
                  placeholder="e.g., portfolio-CREATE-23"
                  disabled={loading}
                  className="w-full px-4 py-3 bg-fd-dark border border-fd-border rounded-lg text-fd-text placeholder-fd-text-muted focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent disabled:opacity-50"
                />
              </div>
            )}
            {searchType === 'recent' && (
              <div className="flex-1">
                <p className="text-fd-text-muted">Showing latest 100 lineage operations</p>
              </div>
            )}

            <button
              onClick={fetchLineageData}
              disabled={loading || (searchType !== 'recent' && !selectedDataset && !selectedRunId && !correlationId)}
              className="px-8 py-3 bg-fd-green text-fd-dark font-bold rounded-lg hover:bg-fd-green/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {loading ? (
                <span className="flex items-center gap-2">
                  <span className="animate-spin">⟳</span>
                  Loading...
                </span>
              ) : (
                'Explore Lineage'
              )}
            </button>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-900/20 border border-red-500 rounded-lg p-4">
            <div className="flex items-center gap-3">
              <span className="text-2xl">⚠️</span>
              <p className="text-red-400 font-medium">{error}</p>
            </div>
          </div>
        )}

        {/* Main Content - Single Box with Two Columns */}
        {lineageGraph.nodes.length > 0 && (
          <div className="bg-fd-darker border border-fd-border rounded-lg shadow-lg overflow-hidden flex flex-1">
            {/* Left Column: Event History (Compact) */}
            {lineageEvents.length > 0 && (
              <div className="flex flex-col border-r border-fd-border" style={{ width: '400px' }}>
                <div className="bg-fd-dark px-4 py-3 border-b border-fd-border">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <span className="text-xl">📄</span>
                      <div>
                        <h2 className="text-lg font-bold text-fd-text">Event History</h2>
                        <p className="text-xs text-fd-text-muted">{filteredEvents.length} of {lineageEvents.length} events</p>
                      </div>
                    </div>
                    <button
                      onClick={() => setFullscreenMode('events')}
                      className="px-2 py-1 text-xs bg-fd-darker hover:bg-fd-dark border border-fd-border rounded text-fd-text hover:text-fd-green transition-all flex items-center gap-1"
                      title="Fullscreen"
                    >
                      <span>⛶</span>
                    </button>
                  </div>
                  
                  {/* Filter Controls */}
                  <div className="space-y-2">
                    <input
                      type="text"
                      placeholder="🔍 Search events..."
                      value={eventFilter}
                      onChange={(e) => setEventFilter(e.target.value)}
                      className="w-full px-3 py-1.5 bg-fd-darker border border-fd-border rounded text-fd-text text-xs placeholder-fd-text-muted focus:outline-none focus:ring-1 focus:ring-fd-green"
                    />
                    <div className="grid grid-cols-2 gap-2">
                      <select
                        value={operationFilter}
                        onChange={(e) => setOperationFilter(e.target.value)}
                        className="px-2 py-1.5 bg-fd-darker border border-fd-border rounded text-fd-text text-xs focus:outline-none focus:ring-1 focus:ring-fd-green"
                      >
                        <option value="all">All Operations</option>
                        {uniqueOperations.map(op => (
                          <option key={op} value={op}>{op}</option>
                        ))}
                      </select>
                      <select
                        value={datasetFilter}
                        onChange={(e) => setDatasetFilter(e.target.value)}
                        className="px-2 py-1.5 bg-fd-darker border border-fd-border rounded text-fd-text text-xs focus:outline-none focus:ring-1 focus:ring-fd-green"
                      >
                        <option value="all">All Datasets</option>
                        {uniqueDatasets.map(ds => (
                          <option key={ds} value={ds}>{ds}</option>
                        ))}
                      </select>
                    </div>
                    {(eventFilter || operationFilter !== 'all' || datasetFilter !== 'all') && (
                      <button
                        onClick={() => {
                          setEventFilter('');
                          setOperationFilter('all');
                          setDatasetFilter('all');
                        }}
                        className="w-full px-2 py-1 text-xs text-fd-text-muted hover:text-fd-green transition-all"
                      >
                        Clear Filters
                      </button>
                    )}
                  </div>
                </div>
                <div className="flex-1 overflow-y-auto">
                  <table className="w-full">
                    <thead className="sticky top-0 bg-fd-dark">
                      <tr className="border-b border-fd-border/30">
                        <th className="text-left py-2 px-3 text-fd-text-muted font-semibold text-xs">Date & Time</th>
                        <th className="text-left py-2 px-3 text-fd-text-muted font-semibold text-xs">Dataset</th>
                        <th className="text-left py-2 px-3 text-fd-text-muted font-semibold text-xs">Op</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredEvents.map((event) => (
                        <tr
                          key={event.id}
                          className={`border-b border-fd-border/10 hover:bg-fd-dark/40 cursor-pointer transition-all ${
                            selectedEventId === event.id ? 'bg-fd-green/10 border-l-4 border-l-fd-green' : ''
                          }`}
                          onClick={() => loadEventLineage(event)}
                          title="Click to view lineage intelligence"
                        >
                          <td className="py-2 px-3 text-fd-text-muted text-xs">
                            <div className="flex flex-col">
                              <span>{new Date(event.createdAt).toLocaleDateString()}</span>
                              <span className="text-fd-text-muted/70">{new Date(event.createdAt).toLocaleTimeString()}</span>
                            </div>
                          </td>
                          <td className="py-2 px-3">
                            <code className="text-fd-green text-xs bg-fd-dark/30 px-1 py-0.5 rounded block truncate" title={event.dataset}>
                              {event.dataset.length > 15 ? event.dataset.substring(0, 15) + '...' : event.dataset}
                            </code>
                          </td>
                          <td className="py-2 px-3">
                            <span className="inline-block px-2 py-0.5 bg-fd-cyan/20 text-fd-cyan text-xs font-semibold rounded-full">
                              {event.operation}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {filteredEvents.length === 0 && (
                    <div className="p-8 text-center text-fd-text-muted">
                      <span className="text-4xl mb-2 block">🔍</span>
                      <p className="text-sm">No events match your filters</p>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Right Column: Graph/Intelligence Tabs */}
            <div className="flex flex-col flex-1 overflow-hidden">
              {/* Main Tab Navigation */}
              <div className="flex border-b border-fd-border bg-fd-dark">
                <button
                  onClick={() => setActiveTab('graph')}
                  className={`flex-1 px-6 py-4 font-medium transition-all duration-200 ${
                    activeTab === 'graph'
                      ? 'text-fd-green border-b-2 border-fd-green bg-fd-darker/50'
                      : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-darker/30'
                  }`}
                >
                  <span className="mr-2">🌐</span>
                  Flow Diagram
                </button>
                <button
                  onClick={() => setActiveTab('intelligence')}
                  className={`flex-1 px-6 py-4 font-medium transition-all duration-200 ${
                    activeTab === 'intelligence'
                      ? 'text-fd-green border-b-2 border-fd-green bg-fd-darker/50'
                      : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-darker/30'
                  }`}
                >
                  <span className="mr-2">📜</span>
                  Lineage Intelligence
                </button>
              </div>

              {/* Graph Tab */}
              {activeTab === 'graph' && (
                <div className="flex flex-col flex-1 overflow-hidden">
                  <div className="bg-fd-dark px-6 py-4 border-b border-fd-border">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">🌐</span>
                        <div>
                          <h2 className="text-xl font-bold text-fd-text">Lineage Flow Diagram</h2>
                          <p className="text-sm text-fd-text-muted">
                            {selectedEventId ? (
                              <>Event: <code className="text-fd-green bg-fd-darker px-2 py-0.5 rounded ml-1">{selectedEventId}</code></>
                            ) : (
                              'Interactive visualization'
                            )}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-4 text-sm">
                        <div className="px-3 py-1 bg-fd-green/20 rounded-full">
                          <span className="text-fd-green font-semibold">{lineageGraph.nodes.length} Nodes</span>
                        </div>
                        <div className="px-3 py-1 bg-fd-cyan/20 rounded-full">
                          <span className="text-fd-cyan font-semibold">{lineageGraph.edges.length} Edges</span>
                        </div>
                        <button
                          onClick={() => setFullscreenMode('graph')}
                          className="px-3 py-1.5 text-xs bg-fd-dark hover:bg-fd-darker border border-fd-border rounded text-fd-text hover:text-fd-green transition-all flex items-center gap-2"
                          title="Fullscreen"
                        >
                          <span>⛶</span>
                          Fullscreen
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="bg-white flex-1 overflow-hidden">
                    <LineageGraph lineageData={lineageGraph} onNodeClick={handleNodeClick} />
                  </div>
                </div>
              )}

              {/* Intelligence Tab */}
              {activeTab === 'intelligence' && lineageEvents.length > 0 && (selectedEventForIntelligence || lineageEvents[0]).outputs && (
                <div className="flex flex-col flex-1 overflow-hidden">
                  <div className="bg-fd-dark px-6 py-4 border-b border-fd-border">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">📜</span>
                        <div>
                          <h2 className="text-xl font-bold text-fd-text">Lineage Intelligence</h2>
                          <p className="text-sm text-fd-text-muted">
                            {selectedEventForIntelligence ? (
                              <>
                                Event: <code className="text-fd-green bg-fd-darker px-2 py-0.5 rounded ml-1">{selectedEventForIntelligence.dataset}</code>
                                <span className="text-fd-text-muted mx-2">•</span>
                                <span className="text-fd-cyan">{selectedEventForIntelligence.operation}</span>
                              </>
                            ) : (
                              'Audit trail and transformations'
                            )}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {selectedEventForIntelligence && (
                          <button
                            onClick={() => {
                              setSelectedEventForIntelligence(null);
                              setSelectedEventId(null);
                            }}
                            className="px-3 py-1.5 text-xs bg-fd-dark hover:bg-fd-darker border border-fd-border rounded text-fd-text-muted hover:text-fd-green transition-all"
                          >
                            View Default Event
                          </button>
                        )}
                        <button
                          onClick={() => setFullscreenMode('intelligence')}
                          className="px-3 py-1.5 text-xs bg-fd-dark hover:bg-fd-darker border border-fd-border rounded text-fd-text hover:text-fd-green transition-all flex items-center gap-2"
                          title="Fullscreen"
                        >
                          <span>⛶</span>
                          Fullscreen
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Intelligence Sub-tabs */}
                  <div className="flex border-b border-fd-border bg-fd-dark/50 overflow-x-auto">
                    {(() => {
                      const currentEvent = selectedEventForIntelligence || lineageEvents[0];
                      return [
                        { id: 'path-detail' as const, icon: '🛤️', label: 'Path', condition: currentEvent.outputs.path },
                        { id: 'origin' as const, icon: '📍', label: 'Origin', condition: currentEvent.outputs.origin },
                        { id: 'transformations' as const, icon: '🔄', label: 'Transform', condition: currentEvent.outputs.transformations },
                        { id: 'consumers' as const, icon: '📊', label: 'Consumers', condition: currentEvent.outputs.consumers },
                        { id: 'metadata' as const, icon: '📋', label: 'Metadata', condition: currentEvent.outputs.metadata }
                      ].filter(tab => tab.condition);
                    })().map((tab) => (
                      <button
                        key={tab.id}
                        onClick={() => setIntelligenceSubTab(tab.id)}
                        className={`px-4 py-3 font-medium transition-all duration-200 whitespace-nowrap ${
                          intelligenceSubTab === tab.id
                            ? 'text-fd-green border-b-2 border-fd-green bg-fd-darker/50'
                            : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-darker/30'
                        }`}
                      >
                        <span className="mr-1">{tab.icon}</span>
                        {tab.label}
                      </button>
                    ))}
                  </div>

                  {/* Intelligence Content - Scrollable */}
                  <div className="flex-1 overflow-y-auto p-6">
                    {(() => {
                      const currentEvent = selectedEventForIntelligence || lineageEvents[0];
                      return (
                        <>
                          {/* Path Detail Tab */}
                          {intelligenceSubTab === 'path-detail' && currentEvent.outputs.path && Array.isArray(currentEvent.outputs.path) && (
                            <div className="space-y-4">
                              {currentEvent.outputs.path.map((stage: any, idx: number) => (
                                <div key={idx} className="flex items-start gap-4">
                                  <div className="flex flex-col items-center">
                                    <div className="w-10 h-10 rounded-full bg-fd-green text-fd-darker flex items-center justify-center font-bold shadow-lg">
                                      {idx + 1}
                                    </div>
                                    {idx < currentEvent.outputs.path.length - 1 && (
                                      <div className="w-0.5 h-16 bg-fd-green my-2"></div>
                                    )}
                                  </div>
                            <div className="flex-1 bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4 hover:border-fd-green/50 transition-all">
                              <div className="flex items-center justify-between mb-3">
                                <div className="flex items-center gap-3">
                                  <span className="text-fd-text font-bold text-lg">{stage.stage}</span>
                                  <span className="px-3 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full font-medium">
                                    {stage.layer}
                                  </span>
                                </div>
                                {stage.timestamp && (
                                  <span className="text-xs text-fd-text-muted font-mono">
                                    {new Date(parseInt(stage.timestamp)).toLocaleTimeString()}
                                  </span>
                                )}
                              </div>
                              <div className="space-y-2 text-sm">
                                {stage.endpoint && (
                                  <div className="flex items-center gap-2">
                                    <span className="text-fd-text-muted">→</span>
                                    <code className="text-fd-green bg-fd-darker/50 px-2 py-1 rounded">{stage.method} {stage.endpoint}</code>
                                  </div>
                                )}
                                {stage.class && (
                                  <div className="flex items-center gap-2">
                                    <span className="text-fd-text-muted">→</span>
                                    <code className="text-fd-cyan bg-fd-darker/50 px-2 py-1 rounded">{stage.class}.{stage.method}()</code>
                                  </div>
                                )}
                                {stage.interface && (
                                  <div className="flex items-center gap-2">
                                    <span className="text-fd-text-muted">→</span>
                                    <code className="text-fd-green-light bg-fd-darker/50 px-2 py-1 rounded">{stage.interface}.{stage.method}()</code>
                                    {stage.type && <span className="text-fd-text-muted text-xs">({stage.type})</span>}
                                  </div>
                                )}
                                {stage.dataset && (
                                  <div className="flex items-center gap-2">
                                    <span className="text-fd-text-muted">→</span>
                                    <code className="text-fd-green bg-fd-darker/50 px-2 py-1 rounded font-bold">{stage.dataset}</code>
                                    {stage.operation && <span className="text-fd-text-muted text-xs">({stage.operation})</span>}
                                  </div>
                                )}
                              </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Origin Tab */}
                          {intelligenceSubTab === 'origin' && currentEvent.outputs.origin && (
                            <div className="space-y-6">
                              <div className="grid grid-cols-2 gap-4">
                                <div className="bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4">
                                  <div className="text-fd-text-muted text-sm mb-2">Primary Dataset</div>
                                  <code className="text-fd-green text-lg font-bold">{currentEvent.outputs.origin.primary_dataset}</code>
                                </div>
                                <div className="bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4">
                                  <div className="text-fd-text-muted text-sm mb-2">Source Type</div>
                                  <span className="text-fd-text font-medium">{currentEvent.outputs.origin.source_type}</span>
                                </div>
                              </div>
                              {currentEvent.outputs.origin.input_sources && currentEvent.outputs.origin.input_sources.length > 0 && (
                                <div>
                                  <h3 className="text-fd-green font-semibold mb-3">Input Sources</h3>
                                  <div className="space-y-3">
                                    {currentEvent.outputs.origin.input_sources.map((source: any, idx: number) => (
                                <div key={idx} className="bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4 hover:border-fd-green/50 transition-all">
                                  <div className="flex items-center justify-between mb-2">
                                    <code className="text-fd-cyan font-semibold">{source.input_name}</code>
                                    {source.source_system && (
                                      <span className="px-2 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full">
                                        {source.source_system}
                                      </span>
                                    )}
                                  </div>
                                  {source.dataset && (
                                    <div className="text-sm text-fd-text-muted">
                                      Dataset: <code className="text-fd-green">{source.dataset}</code>
                                    </div>
                                  )}
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}
                            </div>
                          )}

                          {/* Transformations Tab */}
                          {intelligenceSubTab === 'transformations' && currentEvent.outputs.transformations && Array.isArray(currentEvent.outputs.transformations) && (
                            <div className="space-y-3">
                              {currentEvent.outputs.transformations.map((transform: any, idx: number) => (
                          <div key={idx} className="bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4 hover:border-fd-green/50 transition-all">
                            <div className="flex items-center gap-3 mb-2">
                              <span className="px-3 py-1 text-xs bg-fd-cyan/20 text-fd-cyan rounded-full font-medium">
                                {transform.type}
                              </span>
                              {transform.operation && (
                                <code className="text-fd-green font-semibold">{transform.operation}</code>
                              )}
                              {transform.name && (
                                <code className="text-fd-green font-semibold">{transform.name}</code>
                              )}
                              {transform.table && (
                                <code className="text-fd-green font-semibold bg-fd-dark/50 px-2 py-1 rounded">{transform.table}</code>
                              )}
                            </div>
                            {transform.description && (
                              <p className="text-fd-text-muted text-sm mb-2">{transform.description}</p>
                            )}
                            {transform.purpose && (
                              <p className="text-fd-text text-sm flex items-start gap-2">
                                <span className="text-fd-text-muted">Purpose:</span>
                                <span>{transform.purpose}</span>
                              </p>
                            )}
                            {transform.event_context && (
                              <p className="text-fd-text text-sm flex items-start gap-2 mt-1">
                                <span className="text-fd-text-muted">Context:</span>
                                <code className="text-fd-cyan text-xs">{transform.event_context}</code>
                              </p>
                            )}
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Consumers Tab */}
                          {intelligenceSubTab === 'consumers' && currentEvent.outputs.consumers && Array.isArray(currentEvent.outputs.consumers) && (
                            <div className="space-y-3">
                              {currentEvent.outputs.consumers.map((consumer: any, idx: number) => (
                          <div key={idx} className="bg-fd-dark/30 border border-fd-border/30 rounded-lg p-4 hover:border-fd-green/50 transition-all">
                            <div className="flex items-center gap-3 mb-2">
                              <span className="px-3 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full font-medium">
                                {consumer.type}
                              </span>
                              <code className="text-fd-cyan font-semibold">{consumer.name}</code>
                            </div>
                            {consumer.description && (
                              <p className="text-fd-text-muted text-sm">{consumer.description}</p>
                            )}
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Metadata Tab */}
                          {intelligenceSubTab === 'metadata' && currentEvent.outputs.metadata && (
                            <div className="space-y-6">
                              <div className="grid grid-cols-2 gap-6">
                                <div className="space-y-3">
                                  <h3 className="text-fd-green font-semibold mb-3">Compliance</h3>
                                  {currentEvent.outputs.metadata.recorded_at && (
                                    <div className="flex justify-between text-sm">
                                      <span className="text-fd-text-muted">Recorded</span>
                                      <span className="text-fd-text font-mono">{new Date(currentEvent.outputs.metadata.recorded_at).toLocaleString()}</span>
                                    </div>
                                  )}
                                  {currentEvent.outputs.metadata.user && (
                                    <div className="flex justify-between text-sm">
                                      <span className="text-fd-text-muted">User</span>
                                      <span className="text-fd-text">{currentEvent.outputs.metadata.user}</span>
                                    </div>
                                  )}
                                  {currentEvent.outputs.metadata.run_id && (
                                    <div className="flex justify-between text-sm">
                                      <span className="text-fd-text-muted">Run ID</span>
                                      <code className="text-fd-green text-xs">{currentEvent.outputs.metadata.run_id}</code>
                                    </div>
                                  )}
                                </div>
                                <div className="space-y-3">
                                  <h3 className="text-fd-green font-semibold mb-3">Performance</h3>
                                  {currentEvent.outputs.metadata.duration_ms !== undefined && (
                                    <div className="flex justify-between text-sm">
                                      <span className="text-fd-text-muted">Duration</span>
                                      <span className="text-fd-green font-bold">{currentEvent.outputs.metadata.duration_ms}ms</span>
                                    </div>
                                  )}
                                  {currentEvent.outputs.metadata.audit_ip_address && (
                                    <div className="flex justify-between text-sm">
                                      <span className="text-fd-text-muted">IP Address</span>
                                      <code className="text-fd-text text-xs">{currentEvent.outputs.metadata.audit_ip_address}</code>
                                    </div>
                                  )}
                                </div>
                              </div>
                            </div>
                          )}
                        </>
                      );
                    })()}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Node Details Modal */}
        {selectedNode && (
          <div
            className="fixed inset-0 bg-black/70 flex items-center justify-center z-50"
            onClick={clearSelection}
          >
            <div
              className="bg-fd-darker border-2 border-fd-green rounded-lg max-w-2xl w-full max-h-[80vh] overflow-y-auto m-4 shadow-xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between p-6 border-b border-fd-border bg-fd-dark">
                <div>
                  <h3 className="text-xl font-bold text-fd-text">{selectedNode.label}</h3>
                  <span className="text-sm text-fd-text-muted">{selectedNode.type}</span>
                </div>
                <button
                  onClick={clearSelection}
                  className="text-fd-text-muted hover:text-fd-green text-3xl leading-none transition-all"
                >
                  ×
                </button>
              </div>
              <div className="p-6">
                {selectedNode.metadata && (
                  <pre className="bg-fd-dark border border-fd-border rounded-lg p-4 text-fd-text text-sm overflow-x-auto">
                    {JSON.stringify(selectedNode.metadata, null, 2)}
                  </pre>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Fullscreen Graph Modal */}
        {fullscreenMode === 'graph' && (
          <div className="fixed inset-0 bg-fd-darker z-50 flex flex-col">
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">🌐</span>
                <div>
                  <h2 className="text-xl font-bold text-fd-text">Lineage Flow Diagram</h2>
                  <p className="text-sm text-fd-text-muted">
                    {selectedEventId ? (
                      <>Event: <code className="text-fd-green bg-fd-darker px-2 py-0.5 rounded ml-1">{selectedEventId}</code></>
                    ) : (
                      'Interactive visualization'
                    )}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-4 text-sm">
                  <div className="px-3 py-1 bg-fd-green/20 rounded-full">
                    <span className="text-fd-green font-semibold">{lineageGraph.nodes.length} Nodes</span>
                  </div>
                  <div className="px-3 py-1 bg-fd-cyan/20 rounded-full">
                    <span className="text-fd-cyan font-semibold">{lineageGraph.edges.length} Edges</span>
                  </div>
                </div>
                <button
                  onClick={() => setFullscreenMode('none')}
                  className="text-fd-text-muted hover:text-fd-green text-3xl leading-none transition-all px-2"
                  title="Exit Fullscreen"
                >
                  ×
                </button>
              </div>
            </div>
            <div className="bg-white flex-1 overflow-hidden">
              <LineageGraph lineageData={lineageGraph} onNodeClick={handleNodeClick} />
            </div>
          </div>
        )}

        {/* Fullscreen Intelligence Modal */}
        {fullscreenMode === 'intelligence' && lineageEvents.length > 0 && (selectedEventForIntelligence || lineageEvents[0]).outputs && (
          <div className="fixed inset-0 bg-fd-darker z-50 flex flex-col">
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">📜</span>
                <div>
                  <h2 className="text-xl font-bold text-fd-text">Lineage Intelligence</h2>
                  <p className="text-sm text-fd-text-muted">
                    {selectedEventForIntelligence ? (
                      <>
                        Event: <code className="text-fd-green bg-fd-darker px-2 py-0.5 rounded ml-1">{selectedEventForIntelligence.dataset}</code>
                        <span className="text-fd-text-muted mx-2">•</span>
                        <span className="text-fd-cyan">{selectedEventForIntelligence.operation}</span>
                      </>
                    ) : (
                      'Audit trail and transformations'
                    )}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {selectedEventForIntelligence && (
                  <button
                    onClick={() => {
                      setSelectedEventForIntelligence(null);
                      setSelectedEventId(null);
                    }}
                    className="px-3 py-1.5 text-xs bg-fd-dark hover:bg-fd-darker border border-fd-border rounded text-fd-text-muted hover:text-fd-green transition-all"
                  >
                    View Default Event
                  </button>
                )}
                <button
                  onClick={() => setFullscreenMode('none')}
                  className="text-fd-text-muted hover:text-fd-green text-3xl leading-none transition-all px-2"
                  title="Exit Fullscreen"
                >
                  ×
                </button>
              </div>
            </div>

            {/* Intelligence Sub-tabs */}
            <div className="flex border-b border-fd-border bg-fd-dark/50 overflow-x-auto">
              {(() => {
                const currentEvent = selectedEventForIntelligence || lineageEvents[0];
                return [
                  { id: 'path-detail' as const, icon: '🛤️', label: 'Path', condition: currentEvent.outputs.path },
                  { id: 'origin' as const, icon: '📍', label: 'Origin', condition: currentEvent.outputs.origin },
                  { id: 'transformations' as const, icon: '🔄', label: 'Transform', condition: currentEvent.outputs.transformations },
                  { id: 'consumers' as const, icon: '📊', label: 'Consumers', condition: currentEvent.outputs.consumers },
                  { id: 'metadata' as const, icon: '📋', label: 'Metadata', condition: currentEvent.outputs.metadata }
                ].filter(tab => tab.condition);
              })().map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setIntelligenceSubTab(tab.id)}
                  className={`px-4 py-3 font-medium transition-all duration-200 whitespace-nowrap ${
                    intelligenceSubTab === tab.id
                      ? 'text-fd-green border-b-2 border-fd-green bg-fd-darker/50'
                      : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-darker/30'
                  }`}
                >
                  <span className="mr-1">{tab.icon}</span>
                  {tab.label}
                </button>
              ))}
            </div>

            {/* Intelligence Content - Scrollable */}
            <div className="flex-1 overflow-y-auto p-6">
              {(() => {
                const currentEvent = selectedEventForIntelligence || lineageEvents[0];
                return (
                  <>
                    {/* Path Detail Tab */}
                    {intelligenceSubTab === 'path-detail' && currentEvent.outputs.path && (
                      <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-fd-green mb-4">📊 Data Path</h3>
                        {Array.isArray(currentEvent.outputs.path) ? (
                          <div className="space-y-4">
                            {currentEvent.outputs.path.map((stage: any, idx: number) => (
                              <div key={idx} className="flex items-start gap-4">
                                <div className="flex flex-col items-center">
                                  <div className="w-10 h-10 rounded-full bg-fd-green text-fd-darker flex items-center justify-center font-bold shadow-lg">
                                    {idx + 1}
                                  </div>
                                  {idx < currentEvent.outputs.path.length - 1 && (
                                    <div className="w-0.5 h-16 bg-fd-green my-2"></div>
                                  )}
                                </div>
                                <div className="flex-1 bg-fd-dark border border-fd-border rounded-lg p-4 hover:border-fd-green/50 transition-all">
                                  <div className="flex items-center justify-between mb-3">
                                    <div className="flex items-center gap-3">
                                      <span className="text-fd-text font-bold text-lg">{stage.stage}</span>
                                      <span className="px-3 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full font-medium">
                                        {stage.layer}
                                      </span>
                                    </div>
                                    {stage.timestamp && (
                                      <span className="text-xs text-fd-text-muted font-mono">
                                        {new Date(Number.parseInt(stage.timestamp)).toLocaleTimeString()}
                                      </span>
                                    )}
                                  </div>
                                  <div className="space-y-2 text-sm">
                                    {stage.endpoint && (
                                      <div className="flex items-center gap-2">
                                        <span className="text-fd-text-muted">→</span>
                                        <code className="text-fd-green bg-fd-darker/50 px-2 py-1 rounded">{stage.method} {stage.endpoint}</code>
                                      </div>
                                    )}
                                    {stage.class && (
                                      <div className="flex items-center gap-2">
                                        <span className="text-fd-text-muted">→</span>
                                        <code className="text-fd-cyan bg-fd-darker/50 px-2 py-1 rounded">{stage.class}.{stage.method}()</code>
                                      </div>
                                    )}
                                    {stage.interface && (
                                      <div className="flex items-center gap-2">
                                        <span className="text-fd-text-muted">→</span>
                                        <code className="text-fd-green-light bg-fd-darker/50 px-2 py-1 rounded">{stage.interface}.{stage.method}()</code>
                                        {stage.type && <span className="text-fd-text-muted text-xs">({stage.type})</span>}
                                      </div>
                                    )}
                                    {stage.dataset && (
                                      <div className="flex items-center gap-2">
                                        <span className="text-fd-text-muted">→</span>
                                        <code className="text-fd-green bg-fd-darker/50 px-2 py-1 rounded font-bold">{stage.dataset}</code>
                                        {stage.operation && <span className="text-fd-text-muted text-xs">({stage.operation})</span>}
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                            <pre className="text-fd-text text-sm whitespace-pre-wrap overflow-x-auto">
                              {JSON.stringify(currentEvent.outputs.path, null, 2)}
                            </pre>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Origin Tab */}
                    {intelligenceSubTab === 'origin' && currentEvent.outputs.origin && (
                      <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-fd-green mb-4">📍 Data Origin</h3>
                        <div className="space-y-6">
                          <div className="grid grid-cols-2 gap-4">
                            {currentEvent.outputs.origin.primary_dataset && (
                              <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                                <div className="text-fd-text-muted text-sm mb-2">Primary Dataset</div>
                                <code className="text-fd-green text-lg font-bold">{currentEvent.outputs.origin.primary_dataset}</code>
                              </div>
                            )}
                            {currentEvent.outputs.origin.source_type && (
                              <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                                <div className="text-fd-text-muted text-sm mb-2">Source Type</div>
                                <span className="text-fd-text font-medium">{currentEvent.outputs.origin.source_type}</span>
                              </div>
                            )}
                          </div>
                          {currentEvent.outputs.origin.input_sources && currentEvent.outputs.origin.input_sources.length > 0 && (
                            <div>
                              <h4 className="text-fd-green font-semibold mb-3">Input Sources</h4>
                              <div className="space-y-3">
                                {currentEvent.outputs.origin.input_sources.map((source: any, idx: number) => (
                                  <div key={idx} className="bg-fd-dark border border-fd-border rounded-lg p-4 hover:border-fd-green/50 transition-all">
                                    <div className="flex items-center justify-between mb-2">
                                      <code className="text-fd-cyan font-semibold">{source.input_name}</code>
                                      {source.source_system && (
                                        <span className="px-2 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full">
                                          {source.source_system}
                                        </span>
                                      )}
                                    </div>
                                    {source.dataset && (
                                      <div className="text-sm text-fd-text-muted">
                                        Dataset: <code className="text-fd-green">{source.dataset}</code>
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Transformations Tab */}
                    {intelligenceSubTab === 'transformations' && currentEvent.outputs.transformations && (
                      <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-fd-green mb-4">🔄 Transformations</h3>
                        {Array.isArray(currentEvent.outputs.transformations) ? (
                          <div className="space-y-3">
                            {currentEvent.outputs.transformations.map((transform: any, idx: number) => (
                              <div key={idx} className="bg-fd-dark border border-fd-border rounded-lg p-4">
                                <div className="flex items-center gap-3 mb-2">
                                  {transform.type && (
                                    <span className="px-3 py-1 text-xs bg-fd-cyan/20 text-fd-cyan rounded-full font-medium">
                                      {transform.type}
                                    </span>
                                  )}
                                  {transform.operation && (
                                    <code className="text-fd-green font-semibold">{transform.operation}</code>
                                  )}
                                  {transform.name && (
                                    <code className="text-fd-green font-semibold">{transform.name}</code>
                                  )}
                                  {transform.table && (
                                    <code className="text-fd-green font-semibold bg-fd-darker/50 px-2 py-1 rounded">{transform.table}</code>
                                  )}
                                </div>
                                {transform.description && (
                                  <p className="text-fd-text-muted text-sm mb-2">{transform.description}</p>
                                )}
                                {transform.purpose && (
                                  <p className="text-fd-text text-sm flex items-start gap-2">
                                    <span className="text-fd-text-muted">Purpose:</span>
                                    <span>{transform.purpose}</span>
                                  </p>
                                )}
                                {transform.event_context && (
                                  <p className="text-fd-text text-sm flex items-start gap-2 mt-1">
                                    <span className="text-fd-text-muted">Context:</span>
                                    <code className="text-fd-cyan text-xs">{transform.event_context}</code>
                                  </p>
                                )}
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                            <pre className="text-fd-text text-sm whitespace-pre-wrap overflow-x-auto">
                              {JSON.stringify(currentEvent.outputs.transformations, null, 2)}
                            </pre>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Consumers Tab */}
                    {intelligenceSubTab === 'consumers' && currentEvent.outputs.consumers && (
                      <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-fd-green mb-4">📊 Data Consumers</h3>
                        {Array.isArray(currentEvent.outputs.consumers) ? (
                          <div className="space-y-3">
                            {currentEvent.outputs.consumers.map((consumer: any, idx: number) => (
                              <div key={idx} className="bg-fd-dark border border-fd-border rounded-lg p-4 hover:border-fd-green/50 transition-all">
                                <div className="flex items-center gap-3 mb-2">
                                  <span className="px-3 py-1 text-xs bg-fd-green/20 text-fd-green rounded-full font-medium">
                                    {consumer.type}
                                  </span>
                                  <code className="text-fd-cyan font-semibold">{consumer.name}</code>
                                </div>
                                {consumer.description && (
                                  <p className="text-fd-text-muted text-sm">{consumer.description}</p>
                                )}
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                            <pre className="text-fd-text text-sm whitespace-pre-wrap overflow-x-auto">
                              {JSON.stringify(currentEvent.outputs.consumers, null, 2)}
                            </pre>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Metadata Tab */}
                    {intelligenceSubTab === 'metadata' && currentEvent.outputs.metadata && (
                      <div className="space-y-4">
                        <h3 className="text-lg font-semibold text-fd-green mb-4">📋 Event Metadata</h3>
                        <div className="space-y-6">
                          <div className="grid grid-cols-2 gap-6">
                            <div className="space-y-3">
                              <h4 className="text-fd-green font-semibold mb-3">Compliance</h4>
                              {currentEvent.outputs.metadata.recorded_at && (
                                <div className="flex justify-between text-sm bg-fd-dark border border-fd-border rounded-lg p-3">
                                  <span className="text-fd-text-muted">Recorded</span>
                                  <span className="text-fd-text font-mono">{new Date(currentEvent.outputs.metadata.recorded_at).toLocaleString()}</span>
                                </div>
                              )}
                              {currentEvent.outputs.metadata.user && (
                                <div className="flex justify-between text-sm bg-fd-dark border border-fd-border rounded-lg p-3">
                                  <span className="text-fd-text-muted">User</span>
                                  <span className="text-fd-text">{currentEvent.outputs.metadata.user}</span>
                                </div>
                              )}
                              {currentEvent.outputs.metadata.run_id && (
                                <div className="flex justify-between text-sm bg-fd-dark border border-fd-border rounded-lg p-3">
                                  <span className="text-fd-text-muted">Run ID</span>
                                  <code className="text-fd-green text-xs">{currentEvent.outputs.metadata.run_id}</code>
                                </div>
                              )}
                            </div>
                            <div className="space-y-3">
                              <h4 className="text-fd-green font-semibold mb-3">Performance</h4>
                              {currentEvent.outputs.metadata.duration_ms !== undefined && (
                                <div className="flex justify-between text-sm bg-fd-dark border border-fd-border rounded-lg p-3">
                                  <span className="text-fd-text-muted">Duration</span>
                                  <span className="text-fd-green font-bold">{currentEvent.outputs.metadata.duration_ms}ms</span>
                                </div>
                              )}
                              {currentEvent.outputs.metadata.audit_ip_address && (
                                <div className="flex justify-between text-sm bg-fd-dark border border-fd-border rounded-lg p-3">
                                  <span className="text-fd-text-muted">IP Address</span>
                                  <code className="text-fd-text text-xs">{currentEvent.outputs.metadata.audit_ip_address}</code>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    )}
                  </>
                );
              })()}
            </div>
          </div>
        )}

        {/* Fullscreen Events Modal */}
        {fullscreenMode === 'events' && lineageEvents.length > 0 && (
          <div className="fixed inset-0 bg-fd-darker z-50 flex flex-col">
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">📄</span>
                <div>
                  <h2 className="text-xl font-bold text-fd-text">Event History</h2>
                  <p className="text-sm text-fd-text-muted">{filteredEvents.length} of {lineageEvents.length} events</p>
                </div>
              </div>
              <button
                onClick={() => setFullscreenMode('none')}
                className="text-fd-text-muted hover:text-fd-green text-3xl leading-none transition-all px-2"
                title="Exit Fullscreen"
              >
                ×
              </button>
            </div>

            {/* Filter Controls */}
            <div className="bg-fd-dark px-6 py-4 border-b border-fd-border">
              <div className="space-y-3">
                <input
                  type="text"
                  placeholder="🔍 Search events..."
                  value={eventFilter}
                  onChange={(e) => setEventFilter(e.target.value)}
                  className="w-full px-4 py-2 bg-fd-darker border border-fd-border rounded text-fd-text placeholder-fd-text-muted focus:outline-none focus:ring-2 focus:ring-fd-green"
                />
                <div className="grid grid-cols-2 gap-3">
                  <select
                    value={operationFilter}
                    onChange={(e) => setOperationFilter(e.target.value)}
                    className="px-3 py-2 bg-fd-darker border border-fd-border rounded text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
                  >
                    <option value="all">All Operations</option>
                    {uniqueOperations.map(op => (
                      <option key={op} value={op}>{op}</option>
                    ))}
                  </select>
                  <select
                    value={datasetFilter}
                    onChange={(e) => setDatasetFilter(e.target.value)}
                    className="px-3 py-2 bg-fd-darker border border-fd-border rounded text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
                  >
                    <option value="all">All Datasets</option>
                    {uniqueDatasets.map(ds => (
                      <option key={ds} value={ds}>{ds}</option>
                    ))}
                  </select>
                </div>
                {(eventFilter || operationFilter !== 'all' || datasetFilter !== 'all') && (
                  <button
                    onClick={() => {
                      setEventFilter('');
                      setOperationFilter('all');
                      setDatasetFilter('all');
                    }}
                    className="w-full px-3 py-2 text-sm text-fd-text-muted hover:text-fd-green bg-fd-darker hover:bg-fd-dark border border-fd-border rounded transition-all"
                  >
                    Clear Filters
                  </button>
                )}
              </div>
            </div>

            {/* Event Table */}
            <div className="flex-1 overflow-y-auto">
              <table className="w-full">
                <thead className="sticky top-0 bg-fd-dark">
                  <tr className="border-b border-fd-border">
                    <th className="text-left py-3 px-6 text-fd-text-muted font-semibold">Timestamp</th>
                    <th className="text-left py-3 px-6 text-fd-text-muted font-semibold">Dataset</th>
                    <th className="text-left py-3 px-6 text-fd-text-muted font-semibold">Operation</th>
                    <th className="text-left py-3 px-6 text-fd-text-muted font-semibold">Run ID</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredEvents.map((event) => (
                    <tr
                      key={event.id}
                      className={`border-b border-fd-border/10 hover:bg-fd-dark/60 cursor-pointer transition-all ${
                        selectedEventId === event.id ? 'bg-fd-green/10 border-l-4 border-l-fd-green' : ''
                      }`}
                      onClick={() => {
                        loadEventLineage(event);
                        setFullscreenMode('none');
                      }}
                      title="Click to view lineage"
                    >
                      <td className="py-3 px-6 text-fd-text">
                        {new Date(event.createdAt).toLocaleString()}
                      </td>
                      <td className="py-3 px-6">
                        <code className="text-fd-green bg-fd-dark/30 px-2 py-1 rounded">{event.dataset}</code>
                      </td>
                      <td className="py-3 px-6">
                        <span className="inline-block px-3 py-1 bg-fd-cyan/20 text-fd-cyan font-semibold rounded-full">
                          {event.operation}
                        </span>
                      </td>
                      <td className="py-3 px-6">
                        <code className="text-fd-text-muted text-sm">{event.runId}</code>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredEvents.length === 0 && (
                <div className="p-12 text-center text-fd-text-muted">
                  <span className="text-6xl mb-4 block">🔍</span>
                  <p className="text-lg">No events match your filters</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default LineagePage;
