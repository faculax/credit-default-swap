import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import ServicesStatusModal from '../ServicesStatusModal';

describe('ServicesStatusModal', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    (globalThis as any).fetch = jest.fn(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        status: 'UP',
        backendStatus: { status: 'UP', responseTime: 12 }
      })
    }));
  });
  afterEach(() => {
    jest.clearAllTimers();
    jest.resetAllMocks();
  });

  it('renders when open and shows overview stats', async () => {
    render(<ServicesStatusModal isOpen={true} onClose={() => {}} />);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Services Status/i)).toBeInTheDocument();
    // Wait for fetch resolution
    await screen.findByText(/System Health/i);
  });

  it('toggles auto refresh off', async () => {
    render(<ServicesStatusModal isOpen={true} onClose={() => {}} />);
    const toggle = screen.getByRole('checkbox');
    expect(toggle).toBeChecked();
    fireEvent.click(toggle);
    expect(toggle).not.toBeChecked();
  });

  it('invokes manual refresh (button may show Refreshing... initially)', async () => {
    render(<ServicesStatusModal isOpen={true} onClose={() => {}} />);
    // Button may temporarily show Refreshing...; match both possible states
    const btn = screen.getByRole('button', { name: /Refresh/ });
    fireEvent.click(btn);
    expect((globalThis.fetch as jest.Mock)).toHaveBeenCalled();
  });

  it('auto refresh interval triggers subsequent fetches', async () => {
    render(<ServicesStatusModal isOpen={true} onClose={() => {}} />);
    await screen.findByText(/System Health/i);
  const initialCalls = (globalThis.fetch as jest.Mock).mock.calls.length;
    act(() => {
      jest.advanceTimersByTime(6000); // > 5s interval
    });
  const afterCalls = (globalThis.fetch as jest.Mock).mock.calls.length;
    expect(afterCalls).toBeGreaterThan(initialCalls);
  });
});
