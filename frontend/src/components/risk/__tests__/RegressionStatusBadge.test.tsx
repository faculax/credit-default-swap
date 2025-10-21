import React from 'react';
import { render } from '@testing-library/react';
import RegressionStatusBadge from '../../RegressionStatusBadge';

test('renders regression status badge', () => {
  const { getByText } = render(<RegressionStatusBadge status="PASS" />);
  expect(getByText(/Regression: PASS/)).toBeInTheDocument();
});