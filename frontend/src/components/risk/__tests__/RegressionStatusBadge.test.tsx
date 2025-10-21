import React from 'react';
import { render } from '@testing-library/react';
import RegressionStatusBadge from '../../RegressionStatusBadge';

test('renders regression status badge', () => {
  // eslint-disable-next-line testing-library/prefer-screen-queries
  const { getByText } = render(<RegressionStatusBadge status="PASS" />);
  expect(getByText(/Regression: PASS/)).toBeInTheDocument();
});