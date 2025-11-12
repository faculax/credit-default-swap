import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import RegressionStatusBadge from '../RegressionStatusBadge';

describe('RegressionStatusBadge', () => {
  test('renders PASS status with green background', () => {
    render(<RegressionStatusBadge status="PASS" />);
    const badge = screen.getByText(/Regression: PASS/);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-green-600');
    expect(badge).toHaveClass('text-white');
  });

  test('renders FAIL status with red background', () => {
    render(<RegressionStatusBadge status="FAIL" />);
    const badge = screen.getByText(/Regression: FAIL/);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-red-600');
    expect(badge).toHaveClass('text-white');
  });

  test('renders UNKNOWN status with gray background', () => {
    render(<RegressionStatusBadge status="UNKNOWN" />);
    const badge = screen.getByText(/Regression: UNKNOWN/);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-gray-600');
    expect(badge).toHaveClass('text-white');
  });

  test('shows default title when lastRun is not provided', () => {
    render(<RegressionStatusBadge status="PASS" />);
    const badge = screen.getByText(/Regression: PASS/);
    expect(badge).toHaveAttribute('title', 'No regression run yet');
  });

  test('shows formatted lastRun date in title when provided', () => {
    const lastRunDate = '2025-11-12T10:30:00Z';
    render(<RegressionStatusBadge status="PASS" lastRun={lastRunDate} />);
    const badge = screen.getByText(/Regression: PASS/);
    const title = badge.getAttribute('title');
    expect(title).toContain('Last regression:');
    expect(title).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/); // Match date format
  });

  test('applies correct styling classes', () => {
    render(<RegressionStatusBadge status="PASS" />);
    const badge = screen.getByText(/Regression: PASS/);
    expect(badge).toHaveClass('inline-flex');
    expect(badge).toHaveClass('items-center');
    expect(badge).toHaveClass('px-2');
    expect(badge).toHaveClass('py-0.5');
    expect(badge).toHaveClass('rounded');
    expect(badge).toHaveClass('text-xs');
    expect(badge).toHaveClass('font-semibold');
  });
});
