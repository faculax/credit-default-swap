import React, { useState } from 'react';
import { Contributor } from '../../../services/simulationService';

interface ContributorsTableProps {
  contributors: Contributor[];
}

type SortField = 'entity' | 'marginalElPct' | 'beta';
type SortDirection = 'asc' | 'desc';

const ContributorsTable: React.FC<ContributorsTableProps> = ({ contributors }) => {
  const [sortField, setSortField] = useState<SortField>('marginalElPct');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection(field === 'entity' ? 'asc' : 'desc');
    }
  };

  const sortedContributors = [...contributors].sort((a, b) => {
    let aVal = a[sortField];
    let bVal = b[sortField];

    if (typeof aVal === 'string') {
      aVal = aVal.toLowerCase();
      bVal = (bVal as string).toLowerCase();
    }

    if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
    return 0;
  });

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return (
        <svg className="w-4 h-4 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
        </svg>
      );
    }

    return sortDirection === 'asc' ? (
      <svg className="w-4 h-4 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
      </svg>
    ) : (
      <svg className="w-4 h-4 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
      </svg>
    );
  };

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-fd-dark border-b border-fd-border">
            <tr>
              <th
                className="px-4 py-3 text-left text-sm font-medium text-fd-text cursor-pointer hover:bg-fd-border transition-colors"
                onClick={() => handleSort('entity')}
              >
                <div className="flex items-center gap-2">
                  Entity Name
                  <SortIcon field="entity" />
                </div>
              </th>
              <th
                className="px-4 py-3 text-right text-sm font-medium text-fd-text cursor-pointer hover:bg-fd-border transition-colors"
                onClick={() => handleSort('marginalElPct')}
              >
                <div className="flex items-center justify-end gap-2">
                  Marginal EL %
                  <SortIcon field="marginalElPct" />
                </div>
              </th>
              <th
                className="px-4 py-3 text-right text-sm font-medium text-fd-text cursor-pointer hover:bg-fd-border transition-colors"
                onClick={() => handleSort('beta')}
              >
                <div className="flex items-center justify-end gap-2">
                  Beta (β)
                  <SortIcon field="beta" />
                </div>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-fd-border">
            {sortedContributors.map((contributor, index) => (
              <tr key={index} className="hover:bg-fd-dark/50 transition-colors">
                <td className="px-4 py-3 text-sm text-fd-text">{contributor.entity}</td>
                <td className="px-4 py-3 text-sm text-fd-text text-right font-medium">
                  {contributor.marginalElPct.toFixed(2)}%
                </td>
                <td className="px-4 py-3 text-sm text-fd-text-muted text-right">
                  {contributor.beta.toFixed(3)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {sortedContributors.length === 0 && (
        <div className="p-8 text-center text-fd-text-muted">
          No contributors data available
        </div>
      )}
    </div>
  );
};

export default ContributorsTable;
