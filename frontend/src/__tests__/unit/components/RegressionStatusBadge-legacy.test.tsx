import React from 'react';
import { render } from '@testing-library/react';
import RegressionStatusBadge from '../../../components/risk/RegressionStatusBadge';
import { withStoryId } from '../../../utils/testHelpers';

withStoryId({
  storyId: 'UTS-501',
  testType: 'unit',
  service: 'frontend',
  microservice: 'risk-ui'
})('renders regression status badge', () => {
  const { getByText } = render(<RegressionStatusBadge status="PASS" />);
  expect(getByText(/Regression: PASS/)).toBeInTheDocument();
});

