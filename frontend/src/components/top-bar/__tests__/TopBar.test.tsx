import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import TopBar from '../TopBar';

// Mock the ServicesStatusModal component
jest.mock('../../services-status-modal/ServicesStatusModal', () => {
  return function MockServicesStatusModal({ isOpen, onClose }: any) {
    return isOpen ? (
      <div data-testid="services-modal">
        <button onClick={onClose}>Close Modal</button>
      </div>
    ) : null;
  };
});

describe('TopBar', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2025-11-12T10:30:45'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('renders platform title', () => {
    render(<TopBar />);
    expect(screen.getByText('CREDIT DEFAULT SWAP PLATFORM')).toBeInTheDocument();
  });

  test('displays current date', () => {
    render(<TopBar />);
    expect(screen.getByText('Wed, Nov 12, 2025')).toBeInTheDocument();
  });

  test('displays current time', () => {
    render(<TopBar />);
    expect(screen.getByText('10:30:45')).toBeInTheDocument();
  });

  test('opens services modal when platform title is clicked', () => {
    render(<TopBar />);
    
    const platformTitle = screen.getByText('CREDIT DEFAULT SWAP PLATFORM');
    fireEvent.click(platformTitle);
    
    expect(screen.getByTestId('services-modal')).toBeInTheDocument();
  });

  test('closes services modal when close is triggered', () => {
    render(<TopBar />);
    
    // Open modal
    const platformTitle = screen.getByText('CREDIT DEFAULT SWAP PLATFORM');
    fireEvent.click(platformTitle);
    
    // Close modal
    const closeButton = screen.getByText('Close Modal');
    fireEvent.click(closeButton);
    
    expect(screen.queryByTestId('services-modal')).not.toBeInTheDocument();
  });

  test('updates time every second', () => {
    render(<TopBar />);
    
    expect(screen.getByText('10:30:45')).toBeInTheDocument();
    
    // Advance time by 1 second
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    
    expect(screen.getByText('10:30:46')).toBeInTheDocument();
  });

  test('renders status indicator', () => {
    const { container } = render(<TopBar />);
    const statusIndicator = container.querySelector('.animate-pulse');
    expect(statusIndicator).toBeInTheDocument();
    expect(statusIndicator).toHaveClass('bg-fd-green');
  });
});
