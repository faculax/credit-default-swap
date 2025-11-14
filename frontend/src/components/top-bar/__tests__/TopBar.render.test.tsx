import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import TopBar from '../TopBar';

jest.useFakeTimers();

describe('TopBar render basics', () => {
  afterEach(() => {
    jest.clearAllTimers();
  });

  it('renders platform title and updates clock', () => {
    render(<TopBar />);
    const titleBtn = screen.getByText(/CREDIT DEFAULT SWAP PLATFORM/i);
    expect(titleBtn).toBeInTheDocument();
    const initialTime = screen.getByText(/:/); // contains time
    act(() => {
      jest.advanceTimersByTime(1100); // advance more than 1s
    });
    const updatedTime = screen.getByText(/:/);
    expect(updatedTime.textContent).not.toBe(initialTime.textContent); // time ticked
  });

  it('opens services modal when title clicked', () => {
    render(<TopBar />);
    const titleBtn = screen.getByText(/CREDIT DEFAULT SWAP PLATFORM/i);
  fireEvent.click(titleBtn);
  const modal = screen.getByRole('dialog');
  expect(modal).toBeInTheDocument();
  });
});
