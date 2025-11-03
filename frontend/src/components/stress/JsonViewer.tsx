import React, { useState } from 'react';

interface JsonViewerProps {
  data: any;
  title?: string;
}

const JsonViewer: React.FC<JsonViewerProps> = ({ data, title = 'Raw Response' }) => {
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  const togglePath = (path: string) => {
    setExpanded((prev) => ({
      ...prev,
      [path]: !prev[path],
    }));
  };

  const renderValue = (value: any, path: string = '', depth: number = 0): React.ReactNode => {
    if (value === null || value === undefined) {
      return <span className="text-fd-text-muted italic">null</span>;
    }

    if (typeof value === 'boolean') {
      return <span className="text-blue-400">{value.toString()}</span>;
    }

    if (typeof value === 'number') {
      return <span className="text-green-400">{value}</span>;
    }

    if (typeof value === 'string') {
      return <span className="text-orange-400">"{value}"</span>;
    }

    if (Array.isArray(value)) {
      const isExpanded = expanded[path] ?? (depth < 1); // Auto-expand first level
      const itemCount = value.length;

      return (
        <div>
          <button
            onClick={() => togglePath(path)}
            className="text-fd-text hover:text-fd-green focus:outline-none inline-flex items-center gap-1"
          >
            <svg
              className={`w-3 h-3 transition-transform ${isExpanded ? 'rotate-90' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
            </svg>
            <span className="text-fd-text-muted">
              Array[{itemCount}]
            </span>
          </button>
          {isExpanded && (
            <div className="ml-4 border-l border-fd-border pl-2 mt-1">
              {value.map((item, index) => (
                <div key={index} className="py-1">
                  <span className="text-fd-text-muted mr-2">[{index}]:</span>
                  {renderValue(item, `${path}.${index}`, depth + 1)}
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }

    if (typeof value === 'object') {
      const isExpanded = expanded[path] ?? (depth < 1); // Auto-expand first level
      const keys = Object.keys(value);

      return (
        <div>
          <button
            onClick={() => togglePath(path)}
            className="text-fd-text hover:text-fd-green focus:outline-none inline-flex items-center gap-1"
          >
            <svg
              className={`w-3 h-3 transition-transform ${isExpanded ? 'rotate-90' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
            </svg>
            <span className="text-fd-text-muted">
              Object{'{' + keys.length + '}'}
            </span>
          </button>
          {isExpanded && (
            <div className="ml-4 border-l border-fd-border pl-2 mt-1">
              {keys.map((key) => (
                <div key={key} className="py-1">
                  <span className="text-purple-400 mr-2">"{key}":</span>
                  {renderValue(value[key], `${path}.${key}`, depth + 1)}
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }

    return <span className="text-fd-text">{String(value)}</span>;
  };

  return (
    <div className="bg-fd-dark rounded-lg border border-fd-border p-4">
      <div className="flex items-center justify-between mb-3 pb-2 border-b border-fd-border">
        <h4 className="text-sm font-semibold text-fd-text">{title}</h4>
        <button
          onClick={() => {
            navigator.clipboard.writeText(JSON.stringify(data, null, 2));
          }}
          className="text-xs text-fd-green hover:text-fd-green-hover flex items-center gap-1"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
          </svg>
          Copy
        </button>
      </div>
      <div className="font-mono text-xs overflow-x-auto max-h-96 overflow-y-auto">
        {renderValue(data, 'root', 0)}
      </div>
    </div>
  );
};

export default JsonViewer;
