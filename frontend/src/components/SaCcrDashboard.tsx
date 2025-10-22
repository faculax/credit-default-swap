import React, { useState, useEffect } from 'react';
import { apiUrl } from '../config/api';

interface NettingSet {
    id: number;
    nettingSetId: string;
    counterpartyId: string;
    legalAgreementType: string;
    agreementDate: string;
    governingLaw: string;
    nettingEligible: boolean;
    collateralAgreement: boolean;
    createdAt: string;
}

interface SaCcrCalculation {
    id: number;
    calculationId: string;
    nettingSetId: string;
    calculationDate: string;
    jurisdiction: string;
    alphaFactor: number;
    grossMtm: number;
    vmReceived: number;
    vmPosted: number;
    imReceived: number;
    imPosted: number;
    replacementCost: number;
    effectiveNotional: number;
    supervisoryAddon: number;
    multiplier: number;
    potentialFutureExposure: number;
    exposureAtDefault: number;
    calculationStatus: string;
    nettingSet: NettingSet;
}

interface CalculationRequest {
    valuationDate: string;
    jurisdiction: string;
}

const SaCcrDashboard: React.FC = () => {
    const [nettingSets, setNettingSets] = useState<NettingSet[]>([]);
    const [calculations, setCalculations] = useState<SaCcrCalculation[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [selectedNettingSet, setSelectedNettingSet] = useState<number | null>(null);
    const [calculationRequest, setCalculationRequest] = useState<CalculationRequest>({
        valuationDate: new Date().toISOString().split('T')[0],
        jurisdiction: 'US'
    });

    // Load netting sets on component mount
    useEffect(() => {
        loadNettingSets();
    }, []);

    // Load calculations when jurisdiction or valuation date changes
    useEffect(() => {
        if (calculationRequest.valuationDate) {
            loadCalculations();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [calculationRequest.jurisdiction, calculationRequest.valuationDate]);

    const loadNettingSets = async () => {
        console.log('Loading netting sets...');
        setError(null);
        try {
            const url = apiUrl('/v1/sa-ccr/netting-sets');
            console.log('API URL:', url);
            
            const response = await fetch(url);
            console.log('Response status:', response.status);
            
            const data = await response.json();
            console.log('API Response:', data);
            
            if (data.status === 'SUCCESS') {
                console.log('Setting netting sets:', data.nettingSets?.length);
                setNettingSets(data.nettingSets || []);
            } else {
                const errorMsg = data.message || 'Failed to load netting sets';
                console.error('API error:', errorMsg);
                setError(errorMsg);
            }
        } catch (err) {
            console.error('Fetch error:', err);
            setError('Error connecting to SA-CCR service');
        }
    };

    const loadCalculations = async () => {
        if (!calculationRequest.valuationDate || !calculationRequest.jurisdiction) {
            return;
        }
        
        console.log('Loading calculations for:', calculationRequest);
        try {
            const url = apiUrl(`/v1/sa-ccr/exposures?valuationDate=${calculationRequest.valuationDate}&jurisdiction=${calculationRequest.jurisdiction}`);
            
            const response = await fetch(url);
            const data = await response.json();
            
            if (data.status === 'SUCCESS') {
                console.log('Loaded calculations:', data.calculations?.length);
                setCalculations(data.calculations || []);
            } else {
                // No error shown if no calculations exist - just empty table
                console.log('No calculations found:', data.message);
                setCalculations([]);
            }
        } catch (err) {
            console.error('Error loading calculations:', err);
            // Don't show error for missing calculations, just empty state
            setCalculations([]);
        }
    };

    const calculateExposures = async () => {
        setIsLoading(true);
        setError(null);
        
        try {
            const url = apiUrl(`/v1/sa-ccr/calculate?valuationDate=${calculationRequest.valuationDate}&jurisdiction=${calculationRequest.jurisdiction}`);
            
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            
            const data = await response.json();
            
            if (data.status === 'SUCCESS') {
                setCalculations(data.calculations);
            } else {
                setError(data.message || 'Failed to calculate exposures');
            }
        } catch (err) {
            setError('Error calculating SA-CCR exposures');
            console.error('Error calculating exposures:', err);
        } finally {
            setIsLoading(false);
        }
    };

    const formatCurrency = (amount: number) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const formatNumber = (num: number, decimals: number = 2) => {
        return num.toFixed(decimals);
    };

    // Calculations are already filtered by jurisdiction from API
    const getFilteredCalculations = () => {
        return calculations;
    };

    const getTotalExposure = () => {
        return getFilteredCalculations().reduce((sum, calc) => sum + calc.exposureAtDefault, 0);
    };

    const getMaxExposure = () => {
        const filtered = getFilteredCalculations();
        return filtered.length > 0 ? Math.max(...filtered.map(calc => calc.exposureAtDefault)) : 0;
    };

    const getActiveNettingSetsCount = () => {
        return nettingSets.length;
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold text-fd-text">SA-CCR Exposure Dashboard</h1>
                    <p className="text-fd-text-secondary mt-1">
                        Basel III Standardized Approach for Counterparty Credit Risk
                    </p>
                </div>
            </div>

            {/* Error Display */}
            {error && (
                <div className="bg-red-900/50 border border-red-600 rounded-lg p-4 flex items-start space-x-3">
                    <span className="text-red-400 flex-shrink-0 mt-0.5">⚠️</span>
                    <div>
                        <h3 className="text-red-400 font-medium">Error</h3>
                        <p className="text-red-300 text-sm mt-1">{error}</p>
                    </div>
                </div>
            )}

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <span className="text-2xl text-fd-green">💰</span>
                        </div>
                        <div className="ml-5 w-0 flex-1">
                            <dl>
                                <dt className="text-sm font-medium text-fd-text-secondary truncate">
                                    Total Exposure (EAD)
                                </dt>
                                <dd className="text-lg font-medium text-fd-text">
                                    {formatCurrency(getTotalExposure())}
                                </dd>
                            </dl>
                        </div>
                    </div>
                </div>

                <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <span className="text-2xl text-fd-accent">📊</span>
                        </div>
                        <div className="ml-5 w-0 flex-1">
                            <dl>
                                <dt className="text-sm font-medium text-fd-text-secondary truncate">
                                    Max Single Exposure
                                </dt>
                                <dd className="text-lg font-medium text-fd-text">
                                    {formatCurrency(getMaxExposure())}
                                </dd>
                            </dl>
                        </div>
                    </div>
                </div>

                <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <span className="text-2xl text-fd-blue">🔢</span>
                        </div>
                        <div className="ml-5 w-0 flex-1">
                            <dl>
                                <dt className="text-sm font-medium text-fd-text-secondary truncate">
                                    Active Netting Sets
                                </dt>
                                <dd className="text-lg font-medium text-fd-text">
                                    {getActiveNettingSetsCount()}
                                </dd>
                            </dl>
                        </div>
                    </div>
                </div>

                <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <span className="text-2xl text-fd-yellow">📈</span>
                        </div>
                        <div className="ml-5 w-0 flex-1">
                            <dl>
                                <dt className="text-sm font-medium text-fd-text-secondary truncate">
                                    Calculations Count ({calculationRequest.jurisdiction})
                                </dt>
                                <dd className="text-lg font-medium text-fd-text">
                                    {getFilteredCalculations().length}
                                </dd>
                            </dl>
                        </div>
                    </div>
                </div>
            </div>

            {/* Calculation Control Panel */}
            <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                <h2 className="text-xl font-semibold text-fd-text mb-4">Calculate SA-CCR Exposures</h2>
                <div className="flex items-end space-x-4">
                    <div className="flex-1">
                        <label htmlFor="valuationDate" className="block text-sm font-medium text-fd-text mb-2">
                            Valuation Date
                        </label>
                        <input
                            type="date"
                            id="valuationDate"
                            value={calculationRequest.valuationDate}
                            onChange={(e) => setCalculationRequest(prev => ({
                                ...prev,
                                valuationDate: e.target.value
                            }))}
                            className="block w-full px-3 py-2 border border-fd-border rounded-md shadow-sm bg-fd-dark text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-fd-green sm:text-sm"
                        />
                    </div>
                    <div className="flex-1">
                        <label htmlFor="jurisdiction" className="block text-sm font-medium text-fd-text mb-2">
                            Jurisdiction
                        </label>
                        <select
                            id="jurisdiction"
                            value={calculationRequest.jurisdiction}
                            onChange={(e) => setCalculationRequest(prev => ({
                                ...prev,
                                jurisdiction: e.target.value
                            }))}
                            className="block w-full px-3 py-2 border border-fd-border rounded-md shadow-sm bg-fd-dark text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-fd-green sm:text-sm"
                        >
                            <option value="US">United States</option>
                            <option value="EU">European Union</option>
                            <option value="UK">United Kingdom</option>
                            <option value="CA">Canada</option>
                            <option value="JP">Japan</option>
                            <option value="AU">Australia</option>
                        </select>
                    </div>
                    <div>
                        <button
                            onClick={calculateExposures}
                            disabled={isLoading}
                            className="inline-flex items-center px-6 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-fd-dark bg-fd-green hover:bg-fd-green-dark focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-fd-green disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isLoading ? (
                                <>
                                    <div className="animate-spin -ml-1 mr-3 h-4 w-4 border-2 border-fd-dark border-t-transparent rounded-full"></div>
                                    Calculating...
                                </>
                            ) : (
                                <>
                                    <span className="mr-2">🔢</span>
                                    Calculate Exposures
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </div>

            {/* Calculations Results Table */}
            {calculations.length > 0 && (
                <div className="bg-fd-darker border border-fd-border rounded-lg">
                    <div className="px-6 py-4 border-b border-fd-border">
                        <h2 className="text-xl font-semibold text-fd-text">
                            SA-CCR Calculation Results - {calculationRequest.jurisdiction}
                        </h2>
                        <p className="text-sm text-fd-text-secondary mt-1">
                            Exposure at Default (EAD) = α × (Replacement Cost + Potential Future Exposure)
                            {getFilteredCalculations().length === 0 && (
                                <span className="text-yellow-400 ml-2">
                                    ⚠️ No calculations for selected jurisdiction. Change jurisdiction or run calculations.
                                </span>
                            )}
                        </p>
                    </div>
                    {getFilteredCalculations().length > 0 ? (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-fd-border">
                            <thead>
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Counterparty
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Agreement Type
                                    </th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Replacement Cost
                                    </th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Potential Future Exposure
                                    </th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Alpha Factor
                                    </th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Exposure at Default
                                    </th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                        Status
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-fd-border">
                                {getFilteredCalculations().map((calculation) => (
                                    <tr
                                        key={calculation.id}
                                        className="hover:bg-fd-dark/50 cursor-pointer"
                                        onClick={() => setSelectedNettingSet(selectedNettingSet === calculation.nettingSet.id ? null : calculation.nettingSet.id)}
                                    >
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <div className="text-sm font-medium text-fd-text">
                                                {calculation.nettingSet.counterpartyId}
                                            </div>
                                            <div className="text-sm text-fd-text-secondary">
                                                {calculation.nettingSet.nettingSetId}
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                                            {calculation.nettingSet.legalAgreementType}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text text-right">
                                            {formatCurrency(calculation.replacementCost)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text text-right">
                                            {formatCurrency(calculation.potentialFutureExposure)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text text-right">
                                            {formatNumber(calculation.alphaFactor, 1)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text text-right">
                                            {formatCurrency(calculation.exposureAtDefault)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                                calculation.calculationStatus === 'COMPLETED' 
                                                    ? 'bg-green-900/50 text-green-400' 
                                                    : 'bg-yellow-900/50 text-yellow-400'
                                            }`}>
                                                {calculation.calculationStatus}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    ) : (
                        <div className="px-6 py-8 text-center text-fd-text-muted">
                            <p>No calculations found for jurisdiction: {calculationRequest.jurisdiction}</p>
                            <p className="text-sm mt-2">Try selecting a different jurisdiction or running calculations.</p>
                        </div>
                    )}
                </div>
            )}

            {/* Netting Sets Overview */}
            <div className="bg-fd-darker border border-fd-border rounded-lg">
                <div className="px-6 py-4 border-b border-fd-border">
                    <h2 className="text-xl font-semibold text-fd-text">Active Netting Sets</h2>
                    <p className="text-sm text-fd-text-secondary mt-1">
                        Legal netting agreements for trade aggregation in SA-CCR calculations
                    </p>
                </div>
                <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-fd-border">
                        <thead>
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Netting Set ID
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Counterparty
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Agreement Type
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Governing Law
                                </th>
                                <th className="px-6 py-3 text-center text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Netting Eligible
                                </th>
                                <th className="px-6 py-3 text-center text-xs font-medium text-fd-text-secondary uppercase tracking-wider">
                                    Collateral Agreement
                                </th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-fd-border">
                            {nettingSets.map(nettingSet => (
                                <tr key={nettingSet.id} className="hover:bg-fd-dark/50">
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                                        {nettingSet.nettingSetId}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                                        {nettingSet.counterpartyId}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                                        {nettingSet.legalAgreementType}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                                        {nettingSet.governingLaw}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-center">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                            nettingSet.nettingEligible 
                                                ? 'bg-green-900/50 text-green-400' 
                                                : 'bg-red-900/50 text-red-400'
                                        }`}>
                                            {nettingSet.nettingEligible ? 'Yes' : 'No'}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-center">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                            nettingSet.collateralAgreement 
                                                ? 'bg-green-900/50 text-green-400' 
                                                : 'bg-gray-900/50 text-gray-400'
                                        }`}>
                                            {nettingSet.collateralAgreement ? 'Yes' : 'No'}
                                        </span>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default SaCcrDashboard;