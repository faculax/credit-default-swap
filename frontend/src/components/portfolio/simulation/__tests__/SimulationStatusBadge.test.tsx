import React from 'react';
import { render, screen } from '@testing-library/react';
import SimulationStatusBadge from '../SimulationStatusBadge';

describe('SimulationStatusBadge', () => {
  const statuses = ['COMPLETE', 'RUNNING', 'QUEUED', 'FAILED', 'CANCELED', 'UNKNOWN'];
  for (const status of statuses) {
    it(`renders badge for status ${status}`, () => {
      render(<SimulationStatusBadge status={status} />);
      expect(screen.getByText(status)).toBeInTheDocument();
    });
  }
});
