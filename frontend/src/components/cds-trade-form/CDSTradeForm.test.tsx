import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describeStory, withStoryId } from '../../utils/testHelpers';
import CDSTradeForm from './CDSTradeForm';

// Story 3.1 - CDS Trade Capture UI
describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'Story 3.1 - CDS Trade Capture UI', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC1: Form displays all required fields', 
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should mark required fields with asterisks',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Count red asterisks (required field markers)
          const asterisks = container.querySelectorAll('.text-red-500');
          
          // Should have at least 13 required fields (excluding Restructuring Clause and Obligation which are optional)
          expect(asterisks.length).toBeGreaterThanOrEqual(13);
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should show Restructuring Clause as optional (no asterisk in its label)',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const restructuringLabel = screen.getByText(/Restructuring Clause/i);
          // The label text should NOT contain an asterisk
          expect(restructuringLabel.textContent).not.toMatch(/\*/);
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC2: Default values set correctly',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should set default values as per acceptance criteria',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Trade Date should default to today
          const tradeDateInput = container.querySelector('input[type="date"]') as HTMLInputElement;
          const today = new Date().toISOString().split('T')[0];
          expect(tradeDateInput?.value).toBe(today);

          // Check select default values by finding selects and checking their values
          const selects = container.querySelectorAll('select');
          const selectsArray = Array.from(selects);

          // Currency defaults to USD
          const currencySelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'USD'));
          expect(currencySelect?.value).toBe('USD');

          // Premium Frequency defaults to QUARTERLY
          const premiumFreqSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'QUARTERLY'));
          expect(premiumFreqSelect?.value).toBe('QUARTERLY');

          // Day Count Convention defaults to ACT_360
          const dayCountSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'ACT_360'));
          expect(dayCountSelect?.value).toBe('ACT_360');

          // Trade Direction defaults to BUY
          const tradeDirectionSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'BUY'));
          expect(tradeDirectionSelect?.value).toBe('BUY');

          // Trade Status defaults to PENDING
          const statusSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'PENDING'));
          expect(statusSelect?.value).toBe('PENDING');
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC3: Dropdowns populated from reference data',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should populate Reference Entity dropdown with known entities',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Find the Reference Entity select by its placeholder option
          const selects = Array.from(container.querySelectorAll('select'));
          const refEntitySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Reference Entity')
          );

          expect(refEntitySelect).toBeInTheDocument();
          expect(refEntitySelect?.options.length).toBeGreaterThan(1);
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should populate Counterparty dropdown',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const selects = Array.from(container.querySelectorAll('select'));
          const counterpartySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Counterparty')
          );

          expect(counterpartySelect).toBeInTheDocument();
          expect(counterpartySelect?.options.length).toBeGreaterThan(1);
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should populate Currency dropdown',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const selects = Array.from(container.querySelectorAll('select'));
          const currencySelect = selects.find(s => 
            Array.from(s.options).some(o => o.value === 'USD')
          );

          expect(currencySelect).toBeInTheDocument();
          // Should have multiple currency options
          expect(currencySelect?.options.length).toBeGreaterThan(1);
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC6: Formatting and UI features',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should accept notional amount input',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);

          fireEvent.change(notionalInput, { target: { value: '1000000' } });

          // Value should be formatted with commas
          expect(notionalInput).toHaveValue('1,000,000');
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should clear form when Clear Form button is clicked',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Fill in a field
          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);
          fireEvent.change(notionalInput, { target: { value: '5000000' } });
          expect(notionalInput).toHaveValue('5,000,000');

          // Click Clear Form button
          const clearButton = screen.getByText(/Clear Form/i);
          fireEvent.click(clearButton);

          // Field should be cleared
          expect(notionalInput).toHaveValue('');
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should fill random data when Fill Random Data button is clicked',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);
          
          // Initially empty
          expect(notionalInput).toHaveValue('');

          // Click Fill Random Data
          const fillButton = screen.getByText(/Fill Random Data/i);
          fireEvent.click(fillButton);

          // Should have a value now
          expect(notionalInput).not.toHaveValue('');
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC7: Form submission',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should have submit button that triggers form validation',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Submit button should exist
          const submitButton = screen.getByText(/Book Trade/i);
          expect(submitButton).toBeInTheDocument();

          // Clicking without filling required fields should not call onSubmit
          fireEvent.click(submitButton);
          
          // onSubmit should not be called due to validation
          expect(mockOnSubmit).not.toHaveBeenCalled();
        }
      );
    }
  );
});
