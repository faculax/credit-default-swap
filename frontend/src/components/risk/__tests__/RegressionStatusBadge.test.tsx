import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import RegressionStatusBadge from '../RegressionStatusBadge';

test('renders regression status badge', () => {
  render(<RegressionStatusBadge status="PASS" />);
  expect(screen.getByText(/Regression: PASS/)).toBeInTheDocument();
});