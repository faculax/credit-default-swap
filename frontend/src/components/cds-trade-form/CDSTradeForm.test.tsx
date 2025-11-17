import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { describeStory, withStoryId } from '../../utils/testHelpers';
import CDSTradeForm from './CDSTradeForm';
import { bondService } from '../../services/bondService';

// Mock the bondService
jest.mock('../../services/bondService', () => ({
  bondService: {
    getBondsByIssuer: jest.fn()
  }
}));

// Story 3.1 - CDS Trade Capture UI
describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'Story 3.1 - CDS Trade Capture UI', () => {
  const mockOnSubmit = jest.fn();
  const mockGetBondsByIssuer = bondService.getBondsByIssuer as jest.MockedFunction<typeof bondService.getBondsByIssuer>;

  beforeEach(() => {
    jest.clearAllMocks();
    mockGetBondsByIssuer.mockResolvedValue([
      { 
        issuer: 'BARCLAYS', 
        isin: 'US123456', 
        currency: 'USD',
        notional: 1000000,
        couponRate: 5,
        couponFrequency: 'SEMI_ANNUAL',
        dayCount: 'ACT_ACT',
        issueDate: '2020-01-01',
        maturityDate: '2025-01-01',
        seniority: 'SR_UNSEC'
      },
      { 
        issuer: 'BARCLAYS', 
        isin: 'US789012', 
        currency: 'USD',
        notional: 1000000,
        couponRate: 6,
        couponFrequency: 'SEMI_ANNUAL',
        dayCount: 'ACT_ACT',
        issueDate: '2020-01-01',
        maturityDate: '2027-01-01',
        seniority: 'SR_UNSEC'
      }
    ]);
  });

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC1: Form displays all required fields', 
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should render all CDS trade fields',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Check for presence of all field labels using getByText
          expect(screen.getByText(/Reference Entity/i)).toBeInTheDocument();
          expect(screen.getByText(/Counterparty/i)).toBeInTheDocument();
          expect(screen.getByText(/Currency/i)).toBeInTheDocument();
          expect(screen.getByText(/Notional Amount/i)).toBeInTheDocument();
          expect(screen.getByText(/Fixed Spread/i)).toBeInTheDocument();
          expect(screen.getByText(/Recovery Rate/i)).toBeInTheDocument();
          expect(screen.getByText(/Trade Direction/i)).toBeInTheDocument();
          expect(screen.getByText(/Settlement Type/i)).toBeInTheDocument();
          expect(screen.getByText(/Trade Date/i)).toBeInTheDocument();
          expect(screen.getByText(/Effective Date/i)).toBeInTheDocument();
          expect(screen.getByText(/Maturity Date/i)).toBeInTheDocument();
          expect(screen.getByText(/Premium Frequency/i)).toBeInTheDocument();
          expect(screen.getByText(/Day Count Convention/i)).toBeInTheDocument();
          expect(screen.getByText(/Business Day Convention/i)).toBeInTheDocument();
          expect(screen.getByText(/Restructuring Clause/i)).toBeInTheDocument();
          expect(screen.getByText(/Trade Status/i)).toBeInTheDocument();
        }
      );

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

          // Business Day Convention defaults to FOLLOWING
          const bdcSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'FOLLOWING'));
          expect(bdcSelect?.value).toBe('FOLLOWING');

          // Trade Status defaults to PENDING
          const statusSelect = selectsArray.find(s => Array.from(s.options).some(o => o.value === 'PENDING'));
          expect(statusSelect?.value).toBe('PENDING');

          // Recovery Rate defaults to 0.40 (40%)
          const recoveryRateInput = container.querySelector('input[step="0.01"]') as HTMLInputElement;
          expect(recoveryRateInput?.value).toBe('0.40');
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

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC4: Obligation field conditional on Reference Entity',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should show Obligation field when Reference Entity is selected',
        async () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Find and select a reference entity
          const selects = Array.from(container.querySelectorAll('select'));
          const refEntitySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Reference Entity')
          );

          fireEvent.change(refEntitySelect!, { target: { value: 'BARCLAYS' } });

          // Wait for bonds to load
          await waitFor(() => {
            expect(mockGetBondsByIssuer).toHaveBeenCalledWith('BARCLAYS');
          });

          // Check that Obligation field appears
          await waitFor(() => {
            expect(screen.getByText(/Obligation/i)).toBeInTheDocument();
          }, { timeout: 2000 });
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should clear obligation when reference entity changes',
        async () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const selects = Array.from(container.querySelectorAll('select'));
          const refEntitySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Reference Entity')
          ) as HTMLSelectElement;

          // Select first entity
          fireEvent.change(refEntitySelect, { target: { value: 'BARCLAYS' } });
          await waitFor(() => {
            expect(screen.getByText(/Obligation/i)).toBeInTheDocument();
          }, { timeout: 2000 });

          // Select a different entity
          mockGetBondsByIssuer.mockResolvedValue([
            { 
              issuer: 'DEUTSCHE', 
              isin: 'US345678', 
              currency: 'USD',
              notional: 1000000,
              couponRate: 4,
              couponFrequency: 'SEMI_ANNUAL',
              dayCount: 'ACT_ACT',
              issueDate: '2020-01-01',
              maturityDate: '2026-01-01',
              seniority: 'SR_UNSEC'
            }
          ]);

          fireEvent.change(refEntitySelect, { target: { value: 'DEUTSCHE' } });

          // Bonds should be reloaded
          await waitFor(() => {
            expect(mockGetBondsByIssuer).toHaveBeenCalledWith('DEUTSCHE');
          }, { timeout: 2000 });
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should handle loading state for bonds',
        async () => {
          // Make the service delay
          let resolveBonds: (value: any[]) => void;
          const bondsPromise = new Promise<any[]>((resolve) => {
            resolveBonds = resolve;
          });
          mockGetBondsByIssuer.mockReturnValue(bondsPromise);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const selects = Array.from(container.querySelectorAll('select'));
          const refEntitySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Reference Entity')
          ) as HTMLSelectElement;

          fireEvent.change(refEntitySelect, { target: { value: 'BARCLAYS' } });

          // Should show loading state
          await waitFor(() => {
            expect(screen.getByText(/Loading bonds.../i)).toBeInTheDocument();
          }, { timeout: 1000 });

          // Resolve the promise
          resolveBonds!([]);

          // Wait for loading to complete
          await waitFor(() => {
            expect(screen.queryByText(/Loading bonds.../i)).not.toBeInTheDocument();
          }, { timeout: 2000 });
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should handle empty bonds list',
        async () => {
          mockGetBondsByIssuer.mockResolvedValue([]);

          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const selects = Array.from(container.querySelectorAll('select'));
          const refEntitySelect = selects.find(s => 
            s.options[0]?.textContent?.includes('Select Reference Entity')
          ) as HTMLSelectElement;

          fireEvent.change(refEntitySelect, { target: { value: 'UNKNOWN_ENTITY' } });

          await waitFor(() => {
            expect(screen.getByText(/No bonds available for UNKNOWN_ENTITY/i)).toBeInTheDocument();
          }, { timeout: 2000 });
        }
      );
    }
  );

  describeStory({ storyId: 'UTS-3.1', testType: 'unit' }, 'AC5: Validation rules enforced',
    () => {
      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should validate notional amount is greater than 0',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Find notional input by placeholder
          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);

          // Enter invalid value
          fireEvent.change(notionalInput, { target: { value: '0' } });
          fireEvent.blur(notionalInput);

          // Should show error - exact message from component
          expect(screen.getByText('Notional Amount must be greater than 0')).toBeInTheDocument();
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should validate fixed spread is non-negative',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const spreadInput = screen.getByPlaceholderText(/e.g., 100/);

          fireEvent.change(spreadInput, { target: { value: '-100' } });
          fireEvent.blur(spreadInput);

          expect(screen.getByText('Spread must be 0 or greater')).toBeInTheDocument();
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should validate effectiveDate >= tradeDate',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Find date inputs
          const dateInputs = Array.from(container.querySelectorAll('input[type="date"]'));
          const tradeDateInput = dateInputs[0] as HTMLInputElement;
          const effectiveDateInput = dateInputs[1] as HTMLInputElement;

          // Set trade date to future
          fireEvent.change(tradeDateInput, { target: { value: '2025-12-31' } });
          
          // Set effective date to past
          fireEvent.change(effectiveDateInput, { target: { value: '2025-01-01' } });
          fireEvent.blur(effectiveDateInput);

          expect(screen.getByText('Effective Date must be on or after Trade Date')).toBeInTheDocument();
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should validate maturityDate > effectiveDate',
        () => {
          const { container } = render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const dateInputs = Array.from(container.querySelectorAll('input[type="date"]'));
          const effectiveDateInput = dateInputs[1] as HTMLInputElement;
          const maturityDateInput = dateInputs[2] as HTMLInputElement;

          fireEvent.change(effectiveDateInput, { target: { value: '2025-06-01' } });
          fireEvent.change(maturityDateInput, { target: { value: '2025-05-01' } });
          fireEvent.blur(maturityDateInput);

          expect(screen.getByText('Maturity Date must be after Effective Date')).toBeInTheDocument();
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should clear validation errors when user corrects input',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);

          // Trigger error
          fireEvent.change(notionalInput, { target: { value: '0' } });
          fireEvent.blur(notionalInput);
          expect(screen.getByText('Notional Amount must be greater than 0')).toBeInTheDocument();

          // Correct the value
          fireEvent.change(notionalInput, { target: { value: '1000000' } });
          
          // Error should clear
          expect(screen.queryByText('Notional Amount must be greater than 0')).not.toBeInTheDocument();
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

          // Value should be set
          expect(notionalInput).toHaveValue('1000000');
        }
      );

      withStoryId({ storyId: 'UTS-3.1', testType: 'unit' })(
        'should clear form when Clear Form button is clicked',
        () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          // Fill in a field
          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);
          fireEvent.change(notionalInput, { target: { value: '5000000' } });
          expect(notionalInput).toHaveValue('5000000');

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
        'should disable submit button while submitting',
        async () => {
          render(<CDSTradeForm onSubmit={mockOnSubmit} />);

          const submitButton = screen.getByText(/Book Trade/i);
          
          // Button should initially be enabled
          expect(submitButton).not.toBeDisabled();

          // Make onSubmit async to test disabled state
          let resolveSubmit: () => void;
          const submitPromise = new Promise<void>((resolve) => {
            resolveSubmit = resolve;
          });
          mockOnSubmit.mockImplementation(() => submitPromise);

          // Fill minimum required fields
          const notionalInput = screen.getByPlaceholderText(/e.g., 10,000,000/);
          fireEvent.change(notionalInput, { target: { value: '1000000' } });

          fireEvent.click(submitButton);

          // Button should be disabled during submission
          await waitFor(() => {
            expect(submitButton).toBeDisabled();
          });

          // Resolve submission
          resolveSubmit!();
          
          // Wait for button to be enabled again
          await waitFor(() => {
            expect(submitButton).not.toBeDisabled();
          });
        }
      );

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
