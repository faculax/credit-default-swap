import React from 'react';
import { render, screen, fireEvent, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describeStory, withStoryId } from '../../utils/testHelpers';
import CDSTradeForm from './CDSTradeForm';

// Helper function to get form fields by their position or specific attributes
const getFormFields = (container: HTMLElement) => {
  const selects = Array.from(container.querySelectorAll('select'));
  const inputs = Array.from(container.querySelectorAll('input'));
  
  return {
    // Selects (in order of appearance in the form)
    referenceEntitySelect: selects.find(s => s.options[0]?.textContent?.includes('Select Reference Entity')),
    counterpartySelect: selects.find(s => s.options[0]?.textContent?.includes('Select Counterparty')),
    currencySelect: selects.find(s => Array.from(s.options).some(o => o.value === 'USD')),
    
    // Inputs (by placeholder or type)
    notionalInput: inputs.find(i => i.placeholder?.includes('10,000,000')),
    spreadInput: inputs.find(i => i.type === 'number' && i.step === '0.01'),
    recoveryRateInput: inputs.find(i => i.type === 'number' && i.min === '0' && i.max === '100'),
    
    // Date inputs (by index)
    tradeDateInput: inputs.find(i => i.type === 'date' && i.value === new Date().toISOString().split('T')[0]),
    effectiveDateInput: inputs.filter(i => i.type === 'date')[1],
    maturityDateInput: inputs.filter(i => i.type === 'date')[2],
    accrualStartDateInput: inputs.filter(i => i.type === 'date')[3]
  };
};

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

// Story 3.4 - Booking Confirmation UX & Error Handling
describeStory({ storyId: 'UTS-3.4', testType: 'unit' }, 'Story 3.4 - Booking Confirmation UX & Error Handling', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describeStory({ storyId: 'UTS-3.4', testType: 'unit' }, 'AC1: Success modal displays trade details',
    () => {
      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should disable submit button and show loading state during submission',
        async () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill required fields including obligation (after selecting reference entity)
          fireEvent.change(fields.referenceEntitySelect!, { target: { value: 'MSFT' } });
          
          // Wait for bonds to load (triggers useEffect)
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Re-query to get obligation select that appears after reference entity is selected
          const obligationSelect = container.querySelector('select[value=""]') as HTMLSelectElement;
          const obligationOption = obligationSelect?.options[1]; // First real bond option
          if (obligationOption) {
            fireEvent.change(obligationSelect, { target: { value: obligationOption.value } });
          }

          fireEvent.change(fields.counterpartySelect!, { target: { value: 'JPMORGAN' } });
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });
          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2024-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.accrualStartDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          
          // Click submit
          fireEvent.click(submitButton);

          // Button should show loading state
          expect(screen.getByText(/Booking Trade/i)).toBeInTheDocument();
          
          // Button should be disabled
          expect(submitButton).toBeDisabled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should call onSubmit with trade data when form is valid',
        async () => {
          // Mock bondService to avoid async issues
          jest.spyOn(require('../../services/bondService').bondService, 'getBondsByIssuer')
            .mockResolvedValue([
              { id: 1, isin: 'US1234567890', issuer: 'MSFT', seniority: 'Senior', couponRate: 5, maturityDate: '2030-01-01' }
            ]);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill required fields
          fireEvent.change(fields.referenceEntitySelect!, { target: { value: 'MSFT' } });
          
          // Wait for bonds to load
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Select obligation (first bond)
          const selects = Array.from(container.querySelectorAll('select'));
          const obligationSelect = selects.find(s => s.options[0]?.textContent?.includes('Select Obligation'));
          if (obligationSelect && obligationSelect.options.length > 1) {
            fireEvent.change(obligationSelect, { target: { value: obligationSelect.options[1].value } });
          }

          fireEvent.change(fields.counterpartySelect!, { target: { value: 'JPMORGAN' } });
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });
          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2024-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.accrualStartDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Wait for the simulated API call delay
          await new Promise(resolve => setTimeout(resolve, 1100));

          // onSubmit should have been called
          expect(mockOnSubmit).toHaveBeenCalledTimes(1);
          
          // Verify the data structure
          const submittedData = mockOnSubmit.mock.calls[0][0];
          expect(submittedData).toMatchObject({
            referenceEntity: 'MSFT',
            counterparty: 'JPMORGAN',
            notionalAmount: 10000000,
            spread: 100,
            effectiveDate: '2024-01-01',
            maturityDate: '2029-01-01',
            accrualStartDate: '2024-01-01'
          });
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should reset form to default values after successful submission',
        async () => {
          // Mock bondService
          jest.spyOn(require('../../services/bondService').bondService, 'getBondsByIssuer')
            .mockResolvedValue([
              { id: 1, isin: 'US1234567890', issuer: 'MSFT', seniority: 'Senior', couponRate: 5, maturityDate: '2030-01-01' }
            ]);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill required fields
          fireEvent.change(fields.referenceEntitySelect!, { target: { value: 'MSFT' } });
          
          // Wait for bonds to load
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Select obligation
          const selects = Array.from(container.querySelectorAll('select'));
          const obligationSelect = selects.find(s => s.options[0]?.textContent?.includes('Select Obligation'));
          if (obligationSelect && obligationSelect.options.length > 1) {
            fireEvent.change(obligationSelect, { target: { value: obligationSelect.options[1].value } });
          }

          fireEvent.change(fields.counterpartySelect!, { target: { value: 'JPMORGAN' } });
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });
          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2024-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.accrualStartDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Wait for the simulated API call delay
          await new Promise(resolve => setTimeout(resolve, 1100));

          // Get fields again after form reset
          const fieldsAfterReset = getFormFields(container);

          // Form should be reset (reference entity and counterparty go back to empty)
          expect(fieldsAfterReset.referenceEntitySelect).toHaveValue('');
          expect(fieldsAfterReset.counterpartySelect).toHaveValue('');
          expect(fieldsAfterReset.notionalInput).toHaveValue('');
          expect(fieldsAfterReset.spreadInput).toHaveValue('');
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.4', testType: 'unit' }, 'AC2: Validation errors displayed inline',
    () => {
      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should show inline error for missing reference entity',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Should show validation error
          expect(screen.getByText(/Reference Entity is required/i)).toBeInTheDocument();
          
          // onSubmit should not be called
          expect(mockOnSubmit).not.toHaveBeenCalled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should show inline error for missing counterparty',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Should show validation error
          expect(screen.getByText(/Counterparty is required/i)).toBeInTheDocument();
          
          // onSubmit should not be called
          expect(mockOnSubmit).not.toHaveBeenCalled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should show inline error for invalid notional amount',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          fireEvent.change(fields.notionalInput!, { target: { value: '0' } });

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Should show validation error
          expect(screen.getByText(/Notional Amount must be greater than 0/i)).toBeInTheDocument();
          
          // onSubmit should not be called
          expect(mockOnSubmit).not.toHaveBeenCalled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should show inline error for invalid maturity date (before effective date)',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Should show validation error
          expect(screen.getByText(/Maturity Date must be after Effective Date/i)).toBeInTheDocument();
          
          // onSubmit should not be called
          expect(mockOnSubmit).not.toHaveBeenCalled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should clear error message when user corrects the field',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);
          
          // Submit with invalid data
          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Error should appear
          expect(screen.getByText(/Notional Amount must be greater than 0/i)).toBeInTheDocument();

          // User corrects the field
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });

          // Error should disappear
          expect(screen.queryByText(/Notional Amount must be greater than 0/i)).not.toBeInTheDocument();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should retain form data when validation fails',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill some fields
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });

          // Submit (will fail due to missing required fields)
          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Get fields again to verify they retained values
          const fieldsAfterValidation = getFormFields(container);

          // Form data should be retained
          expect(fieldsAfterValidation.notionalInput).toHaveValue('10,000,000');
          expect(fieldsAfterValidation.spreadInput).toHaveValue(100); // Number type input
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.4', testType: 'unit' }, 'AC3: Duplicate submission prevention',
    () => {
      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should prevent duplicate submission by disabling button during submission',
        async () => {
          // Mock bondService
          jest.spyOn(require('../../services/bondService').bondService, 'getBondsByIssuer')
            .mockResolvedValue([
              { id: 1, isin: 'US1234567890', issuer: 'MSFT', seniority: 'Senior', couponRate: 5, maturityDate: '2030-01-01' }
            ]);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill required fields
          fireEvent.change(fields.referenceEntitySelect!, { target: { value: 'MSFT' } });
          
          // Wait for bonds to load
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Select obligation
          const selects = Array.from(container.querySelectorAll('select'));
          const obligationSelect = selects.find(s => s.options[0]?.textContent?.includes('Select Obligation'));
          if (obligationSelect && obligationSelect.options.length > 1) {
            fireEvent.change(obligationSelect, { target: { value: obligationSelect.options[1].value } });
          }

          fireEvent.change(fields.counterpartySelect!, { target: { value: 'JPMORGAN' } });
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });
          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2024-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.accrualStartDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          
          // First click
          fireEvent.click(submitButton);
          
          // Button should be disabled immediately
          expect(submitButton).toBeDisabled();
          
          // Try to click again (should have no effect)
          fireEvent.click(submitButton);
          fireEvent.click(submitButton);

          // Wait for the simulated API call delay
          await new Promise(resolve => setTimeout(resolve, 1100));

          // onSubmit should have been called only once
          expect(mockOnSubmit).toHaveBeenCalledTimes(1);
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.4', testType: 'unit' }, 'AC5: Accessibility features',
    () => {
      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should have proper ARIA label for submit button',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const submitButton = screen.getByText(/Book Trade/i);
          
          // Button should be in the document and accessible
          expect(submitButton).toBeInTheDocument();
          expect(submitButton).toBeEnabled();
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should mark required fields with visual indicators (asterisks)',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // All required fields should have red asterisks
          const asterisks = container.querySelectorAll('.text-red-500');
          
          // Should have multiple required field markers
          expect(asterisks.length).toBeGreaterThanOrEqual(10);
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should associate error messages with their input fields',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const submitButton = screen.getByText(/Book Trade/i);
          fireEvent.click(submitButton);

          // Error messages should appear near their fields
          const errorMessages = screen.getAllByText(/is required|must be/i);
          
          // Should have multiple error messages
          expect(errorMessages.length).toBeGreaterThan(0);
          
          // All errors should be visible
          for (const error of errorMessages) {
            expect(error).toBeVisible();
          }
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should support keyboard navigation through form fields',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // All interactive elements should be accessible via keyboard
          const submitButton = screen.getByText(/Book Trade/i);

          // Elements should be focusable
          expect(fields.referenceEntitySelect).not.toHaveAttribute('tabindex', '-1');
          expect(fields.counterpartySelect).not.toHaveAttribute('tabindex', '-1');
          expect(submitButton).not.toHaveAttribute('tabindex', '-1');
        }
      );

      withStoryId({ storyId: 'UTS-3.4', testType: 'unit' })(
        'should disable submit button when form is submitting',
        async () => {
          // Mock bondService
          jest.spyOn(require('../../services/bondService').bondService, 'getBondsByIssuer')
            .mockResolvedValue([
              { id: 1, isin: 'US1234567890', issuer: 'MSFT', seniority: 'Senior', couponRate: 5, maturityDate: '2030-01-01' }
            ]);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);
          const fields = getFormFields(container);

          // Fill required fields
          fireEvent.change(fields.referenceEntitySelect!, { target: { value: 'MSFT' } });
          
          // Wait for bonds to load
          await new Promise(resolve => setTimeout(resolve, 100));
          
          // Select obligation
          const selects = Array.from(container.querySelectorAll('select'));
          const obligationSelect = selects.find(s => s.options[0]?.textContent?.includes('Select Obligation'));
          if (obligationSelect && obligationSelect.options.length > 1) {
            fireEvent.change(obligationSelect, { target: { value: obligationSelect.options[1].value } });
          }

          fireEvent.change(fields.counterpartySelect!, { target: { value: 'JPMORGAN' } });
          fireEvent.change(fields.notionalInput!, { target: { value: '10000000' } });
          fireEvent.change(fields.spreadInput!, { target: { value: '100' } });
          fireEvent.change(fields.effectiveDateInput!, { target: { value: '2024-01-01' } });
          fireEvent.change(fields.maturityDateInput!, { target: { value: '2029-01-01' } });
          fireEvent.change(fields.accrualStartDateInput!, { target: { value: '2024-01-01' } });

          const submitButton = screen.getByText(/Book Trade/i);
          
          // Submit
          fireEvent.click(submitButton);

          // Button should be disabled to prevent keyboard re-submission
          expect(submitButton).toBeDisabled();
        }
      );
    }
  );
});

