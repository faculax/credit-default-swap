import React from 'react';

interface MetricsCardProps {
  label: string;
  value: string;
  description: string;
}

const MetricsCard: React.FC<MetricsCardProps> = ({ label, value, description }) => {
  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border p-4 hover:border-fd-green transition-colors">
      <h4 className="text-sm font-medium text-fd-text-muted mb-2">{label}</h4>
      <p className="text-2xl font-bold text-fd-text mb-2">{value}</p>
      <p className="text-xs text-fd-text-muted leading-relaxed">{description}</p>
    </div>
  );
};

export default MetricsCard;
