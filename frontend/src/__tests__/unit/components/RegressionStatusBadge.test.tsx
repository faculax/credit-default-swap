// Example unit test demonstrating proper structure and naming conventions in the new test folder hierarchy
import React from 'react';
import { render, screen } from '@testing-library/react';
import { describeStory, withStoryId } from '../../../utils/testHelpers';
import RegressionStatusBadge from '../../../components/risk/RegressionStatusBadge';

describeStory({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' }, 'RegressionStatusBadge Unit Tests', () => {
  withStoryId({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' })('should render PASS status with green styling', () => {
    // Arrange
    render(<RegressionStatusBadge status="PASS" />);
    
    // Act
    const badge = screen.getByText(/Regression: PASS/i);
    
    // Assert
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-green-600');
    expect(badge).toHaveClass('text-white');
  });

  withStoryId({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' })('should render FAIL status with red styling', () => {
    // Arrange
    render(<RegressionStatusBadge status="FAIL" />);
    
    // Act
    const badge = screen.getByText(/Regression: FAIL/i);
    
    // Assert
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-red-600');
    expect(badge).toHaveClass('text-white');
  });

  withStoryId({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' })('should render UNKNOWN status with gray styling', () => {
    // Arrange
    render(<RegressionStatusBadge status="UNKNOWN" />);
    
    // Act
    const badge = screen.getByText(/Regression: UNKNOWN/i);
    
    // Assert
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-gray-600');
    expect(badge).toHaveClass('text-white');
  });

  withStoryId({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' })('should display lastRun date in title attribute', () => {
    // Arrange
    const lastRun = '2024-11-14T12:00:00Z';
    render(<RegressionStatusBadge status="PASS" lastRun={lastRun} />);
    
    // Act
    const badge = screen.getByText(/Regression: PASS/i);
    
    // Assert
    expect(badge).toHaveAttribute('title');
    expect(badge.getAttribute('title')).toContain('Last regression:');
  });

  withStoryId({ storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' })('should display fallback title when lastRun is not provided', () => {
    // Arrange
    render(<RegressionStatusBadge status="PASS" />);
    
    // Act
    const badge = screen.getByText(/Regression: PASS/i);
    
    // Assert
    expect(badge).toHaveAttribute('title', 'No regression run yet');
  });
});


