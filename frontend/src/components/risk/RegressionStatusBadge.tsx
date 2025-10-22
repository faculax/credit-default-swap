import React from 'react';

interface Props {
  status: 'PASS' | 'FAIL' | 'UNKNOWN';
  lastRun?: string;
}

const colorMap: Record<string, string> = {
  PASS: 'bg-green-600 text-white',
  FAIL: 'bg-red-600 text-white',
  UNKNOWN: 'bg-gray-600 text-white',
};

const RegressionStatusBadge: React.FC<Props> = ({ status, lastRun }) => {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${colorMap[status]}`}
      title={
        lastRun ? `Last regression: ${new Date(lastRun).toLocaleString()}` : 'No regression run yet'
      }
    >
      Regression: {status}
    </span>
  );
};

export default RegressionStatusBadge;
