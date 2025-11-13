import React from 'react';
import { render, screen } from '@testing-library/react';
import RegressionStatusBadge from '../RegressionStatusBadge';

describe('RegressionStatusBadge', () => {
  it('renders PASS status with green class', () => {
    render(<RegressionStatusBadge status="PASS" />);
    const badge = screen.getByText(/Regression: PASS/);
    expect(badge).toHaveClass('bg-green-600');
  });
  it('renders FAIL status with red class', () => {
    render(<RegressionStatusBadge status="FAIL" />);
    const badge = screen.getByText(/Regression: FAIL/);
    expect(badge).toHaveClass('bg-red-600');
  });
  it('adds title tooltip from lastRun', () => {
    const ts = new Date('2024-02-01T10:20:30Z').toISOString();
    render(<RegressionStatusBadge status="UNKNOWN" lastRun={ts} />);
    const badge = screen.getByText(/Regression: UNKNOWN/);
    expect(badge.getAttribute('title')).toMatch(/Last regression:/);
  });
});
