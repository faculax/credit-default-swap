import React, { useState, useEffect, useCallback } from 'react';
import { CDSTradeResponse, cdsTradeService } from '../../services/cdsTradeService';
import { CreditEvent, creditEventService } from '../../services/creditEventService';
import RiskMeasuresPanel from '../risk/RiskMeasuresPanel';
import MarketDataPanel from '../risk/MarketDataPanel';
import ScenarioRunModal from '../risk/ScenarioRunModal';
import RegressionStatusBadge from '../risk/RegressionStatusBadge';
import { fetchRiskMeasures } from '../../services/risk/riskService';
import { RiskMeasures } from '../../services/risk/riskTypes';
import { bondService, Bond } from '../../services/bondService';
import CreditEventModal, { CreateCreditEventRequest } from '../credit-event-modal/CreditEventModal';
import LifecycleTimeline from '../lifecycle/LifecycleTimeline';
import CashflowPanel from '../lifecycle/CashflowPanel';

interface TradeDetailModalProps {
  isOpen: boolean;
  trade: CDSTradeResponse | null;
  onClose: () => void;
  onTradeUpdated?: (updatedTrade: CDSTradeResponse) => void;
  onTradesUpdated?: (affectedTradeIds?: number[]) => void;
}

const TradeDetailModal: React.FC<TradeDetailModalProps> = ({ isOpen, trade, onClose, onTradeUpdated, onTradesUpdated }) => {
  const [currentTrade, setCurrentTrade] = useState<CDSTradeResponse | null>(trade);
  const [creditEvents, setCreditEvents] = useState<CreditEvent[]>([]);
  const [loadingEvents, setLoadingEvents] = useState(false);
  const [activeTab, setActiveTab] = useState<'details' | 'lifecycle' | 'cashflow' | 'events' | 'risk' | 'marketdata'>('details');
  const [showScenarioModal, setShowScenarioModal] = useState(false);
  const [riskMeasures, setRiskMeasures] = useState<RiskMeasures | null>(null);
  const [loadingRisk, setLoadingRisk] = useState(false);
  const [showCreditEventModal, setShowCreditEventModal] = useState(false);
  const [recordingEvent, setRecordingEvent] = useState(false);
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string>('Credit event recorded successfully');

  // Update currentTrade when trade prop changes
  useEffect(() => {
    setCurrentTrade(trade);
  }, [trade]);

  const loadCreditEvents = useCallback(async () => {
    if (!currentTrade) return;
    
    setLoadingEvents(true);
    try {
      const events = await creditEventService.getCreditEventsForTrade(currentTrade.id);
      setCreditEvents(events);
    } catch (error) {
      console.error('Failed to load credit events:', error);
      setCreditEvents([]);
    } finally {
      setLoadingEvents(false);
    }
  }, [currentTrade]);

  const loadRiskMeasures = useCallback(async () => {
    if (!currentTrade) return;
    
    setLoadingRisk(true);
    try {
      const measures = await fetchRiskMeasures(currentTrade.id);
      setRiskMeasures(measures);
    } catch (error) {
      console.error('Failed to load risk measures:', error);
      setRiskMeasures(null);
    } finally {
      setLoadingRisk(false);
    }
  }, [currentTrade]);

  const handleRecordCreditEvent = async (request: CreateCreditEventRequest) => {
    if (!currentTrade) return;
    
    setRecordingEvent(true);
    try {
      const response = await creditEventService.recordCreditEvent(currentTrade.id, request);
      
      // Reload the trade to get updated status
      const updatedTrade = await cdsTradeService.getTradeById(currentTrade.id);
      setCurrentTrade(updatedTrade);
      
      // Notify parent component if callback provided
      if (onTradeUpdated) {
        onTradeUpdated(updatedTrade);
      }
      
      // Notify parent about all affected trades (for UI refresh)
      if (onTradesUpdated && response.affectedTradeIds && response.affectedTradeIds.length > 1) {
        onTradesUpdated(response.affectedTradeIds);
      }
      
      // Reload credit events after successful recording
      await loadCreditEvents();
      setShowCreditEventModal(false);
      
      // Show success notification with propagation info
      const propagatedCount = response.affectedTradeIds.length - 1;
      if (propagatedCount > 0) {
        setSuccessMessage(
          `This ${response.creditEvent.eventType} event has been propagated to ${propagatedCount} other active CDS contract(s) for the same reference entity. All affected trades have been settled.`
        );
      } else {
        setSuccessMessage('Credit event recorded successfully');
      }
      
      setShowSuccessNotification(true);
      setTimeout(() => setShowSuccessNotification(false), 5000);
    } catch (error: any) {
      console.error('Failed to record credit event:', error);
      alert('Failed to record credit event: ' + (error.message || 'Unknown error'));
    } finally {
      setRecordingEvent(false);
    }
  };

  const handleTradeUpdated = async () => {
    if (!currentTrade) return;
    
    try {
      // Reload the trade to get updated status
      const updatedTrade = await cdsTradeService.getTradeById(currentTrade.id);
      setCurrentTrade(updatedTrade);
      
      // Notify parent component if callback provided
      if (onTradeUpdated) {
        onTradeUpdated(updatedTrade);
      }
      
      // Notify parent to refresh the blotter with this trade ID
      if (onTradesUpdated) {
        onTradesUpdated([currentTrade.id]);
      }
    } catch (error) {
      console.error('Failed to reload trade:', error);
    }
  };

  useEffect(() => {
    if (isOpen && currentTrade) {
      loadCreditEvents();
      // Load risk measures when opening modal or switching to risk/marketdata tabs
      if (activeTab === 'risk' || activeTab === 'marketdata') {
        loadRiskMeasures();
      }
    }
  }, [isOpen, currentTrade, activeTab, loadCreditEvents, loadRiskMeasures]);

  if (!isOpen || !currentTrade) return null;

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6 max-w-5xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header with tabs */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-fd-green rounded-full flex items-center justify-center">
              <svg className="w-5 h-5 text-fd-dark" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-fd-text">CDS Trade Details</h2>
          </div>
          <button onClick={onClose} className="text-fd-text-muted hover:text-fd-text transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>
        {/* Tab selector */}
        <div className="flex space-x-4 border-b border-fd-border mb-6">
          {[
            { key: 'details', label: 'Details' },
            { key: 'lifecycle', label: 'Lifecycle' },
            { key: 'cashflow', label: 'Cashflow' },
            { key: 'events', label: 'Credit Events' },
            { key: 'risk', label: 'Risk' },
            { key: 'marketdata', label: 'Market Data' }
          ].map(t => (
            <button
              key={t.key}
              onClick={() => {
                console.log('🔄 Tab switch:', { from: activeTab, to: t.key });
                setActiveTab(t.key as any);
              }}
              className={`pb-2 px-1 text-sm font-medium border-b-2 -mb-px transition-colors ${
                activeTab === t.key ? 'border-fd-green text-fd-text' : 'border-transparent text-fd-text-muted hover:text-fd-text'
              }`}
            >{t.label}</button>
          ))}
        </div>
        {activeTab === 'details' && (
          <>
            <div className="bg-fd-dark rounded-lg p-4 mb-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="flex items-center justify-between">
                  <span className="text-fd-text-muted">Trade ID:</span>
                  <span className="text-fd-green font-mono font-semibold">CDS-{currentTrade.id}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-fd-text-muted">Created:</span>
                  <span className="text-fd-text">{formatDateTime(currentTrade.createdAt)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-fd-text-muted">Last Updated:</span>
                  <span className="text-fd-text">
                    {currentTrade.updatedAt ? formatDateTime(currentTrade.updatedAt) : 'Never'}
                  </span>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Trade Information */}
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">
                  Trade Information
                </h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Reference Entity:</span>
                    <span className="text-fd-text font-medium">{currentTrade.referenceEntity}</span>
                  </div>
                  {currentTrade.obligation && (
                    <div className="flex justify-between">
                      <span className="text-fd-text-muted">Obligation:</span>
                      <span className="text-fd-text font-medium">
                        <span className="text-fd-green">
                          {currentTrade.obligation.isin ? `${currentTrade.obligation.isin} - ` : ''}
                          {currentTrade.obligation.issuer} {currentTrade.obligation.seniority} 
                          ({(currentTrade.obligation.couponRate * 100).toFixed(2)}%)
                        </span>
                      </span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Counterparty:</span>
                    <span className="text-fd-text font-medium">{currentTrade.counterparty}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Direction:</span>
                    <span className={`font-medium ${currentTrade.buySellProtection === 'BUY' ? 'text-blue-400' : 'text-orange-400'}`}>
                      {currentTrade.buySellProtection === 'BUY' ? 'Buy Protection' : 'Sell Protection'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Settlement Type:</span>
                    <span className={`font-medium ${currentTrade.settlementType === 'CASH' ? 'text-fd-cyan' : 'text-fd-teal'}`}>
                      {currentTrade.settlementType === 'CASH' ? 'Cash Settlement' : 'Physical Settlement'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Notional Amount:</span>
                    <span className="text-fd-text font-semibold text-lg">
                      {formatCurrency(currentTrade.notionalAmount, currentTrade.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Spread:</span>
                    <span className="text-fd-text font-medium">{currentTrade.spread} bps</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Currency:</span>
                    <span className="text-fd-text font-medium">{currentTrade.currency}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Status:</span>
                    <span className="text-fd-green font-medium">{currentTrade.tradeStatus.replace(/_/g, ' ')}</span>
                  </div>
                </div>
              </div>

              {/* Date Information */}
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">
                  Date Information
                </h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Trade Date:</span>
                    <span className="text-fd-text">{formatDate(currentTrade.tradeDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Effective Date:</span>
                    <span className="text-fd-text">{formatDate(currentTrade.effectiveDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Maturity Date:</span>
                    <span className="text-fd-text">{formatDate(currentTrade.maturityDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Accrual Start Date:</span>
                    <span className="text-fd-text">{formatDate(currentTrade.accrualStartDate)}</span>
                  </div>
                </div>
              </div>

              {/* Contract Terms */}
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">
                  Contract Terms
                </h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Premium Frequency:</span>
                    <span className="text-fd-text">{currentTrade.premiumFrequency.replace('_', ' ')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Day Count Convention:</span>
                    <span className="text-fd-text">{currentTrade.dayCountConvention.replace('_', '/')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Payment Calendar:</span>
                    <span className="text-fd-text">{currentTrade.paymentCalendar}</span>
                  </div>
                  {currentTrade.restructuringClause && (
                    <div className="flex justify-between">
                      <span className="text-fd-text-muted">Restructuring Clause:</span>
                      <span className="text-fd-text">{currentTrade.restructuringClause.replace(/_/g, ' ')}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Additional Information */}
              <div className="space-y-6">
                <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">
                  Additional Information
                </h3>
                
                <div className="space-y-4">
                  <div className="bg-fd-dark rounded-lg p-4">
                    <div className="text-sm text-fd-text-muted mb-2">Trade Identifier</div>
                    <div className="font-mono text-fd-green text-lg">CDS-{currentTrade.id}</div>
                  </div>
                  
                  <div className="bg-fd-dark rounded-lg p-4">
                    <div className="text-sm text-fd-text-muted mb-2">Risk Metrics</div>
                    <div className="space-y-1">
                      <div className="text-sm">
                        <span className="text-fd-text-muted">Mark-to-Market:</span>
                        <span className="text-fd-text ml-2">TBD</span>
                      </div>
                      <div className="text-sm">
                        <span className="text-fd-text-muted">Credit Duration:</span>
                        <span className="text-fd-text ml-2">TBD</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}
        {activeTab === 'lifecycle' && currentTrade && (
          <LifecycleTimeline trade={currentTrade} onTradeUpdated={handleTradeUpdated} />
        )}
        {activeTab === 'cashflow' && currentTrade && (
          <CashflowPanel trade={currentTrade} />
        )}
        {activeTab === 'events' && (
          <div className="mt-4">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">Credit Events</h3>
              {currentTrade?.tradeStatus === 'ACTIVE' && (
                <button
                  onClick={() => setShowCreditEventModal(true)}
                  className="px-4 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                  </svg>
                  Record Credit Event
                </button>
              )}
            </div>
            {loadingEvents ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-fd-green"></div>
                <span className="ml-3 text-fd-text-muted">Loading credit events...</span>
              </div>
            ) : creditEvents.length === 0 ? (
              <div className="bg-fd-dark rounded-lg p-6 text-center">
                <div className="w-12 h-12 bg-fd-border rounded-full flex items-center justify-center mx-auto mb-3">
                  <svg className="w-6 h-6 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                </div>
                <p className="text-fd-text-muted">No credit events recorded for this trade</p>
              </div>
            ) : (
              <div className="space-y-4">
                {creditEvents.map((event) => (
                  <div key={event.id} className={`bg-fd-dark rounded-lg p-4 border ${
                    event.eventType === 'PAYOUT' ? 'border-fd-green' : 'border-fd-border'
                  }`}>
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                          event.eventType === 'PAYOUT' 
                            ? 'bg-fd-green/20' 
                            : 'bg-red-500/20'
                        }`}>
                          {event.eventType === 'PAYOUT' ? (
                            <svg className="w-5 h-5 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                            </svg>
                          ) : (
                            <svg className="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
                            </svg>
                          )}
                        </div>
                        <div>
                          <h4 className={`font-medium ${
                            event.eventType === 'PAYOUT' ? 'text-fd-green' : 'text-fd-text'
                          }`}>{event.eventType.replace(/_/g, ' ')}</h4>
                          <p className="text-fd-text-muted text-sm">Event ID: {event.id}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-fd-text text-sm">{formatDate(event.eventDate)}</div>
                        <div className="text-fd-text-muted text-xs">{formatDateTime(event.createdAt)}</div>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                      <div>
                        <span className="text-fd-text-muted text-sm">Notice Date:</span>
                        <div className="text-fd-text">{formatDate(event.noticeDate)}</div>
                      </div>
                      <div>
                        <span className="text-fd-text-muted text-sm">Settlement Method:</span>
                        <div className="text-fd-text font-medium">
                          <span className={`inline-flex px-2 py-1 rounded text-xs font-medium ${
                            event.settlementMethod === 'CASH' 
                              ? 'bg-green-500/20 text-green-400' 
                              : 'bg-blue-500/20 text-blue-400'
                          }`}>
                            {event.settlementMethod}
                          </span>
                        </div>
                      </div>
                    </div>

                    {event.comments && (
                      <div className="bg-fd-darker rounded p-3">
                        <span className="text-fd-text-muted text-sm">Comments:</span>
                        <p className="text-fd-text text-sm mt-1">{event.comments}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
        {activeTab === 'risk' && currentTrade && (
          <div className="mt-4 space-y-6" aria-labelledby="risk-panel-heading">
            <div className="flex items-center justify-between">
              <h3 id="risk-panel-heading" className="text-lg font-semibold text-fd-text">Risk Analytics</h3>
              <div className="flex items-center gap-3">
                <RegressionStatusBadge status="UNKNOWN" />
                <button
                  onClick={() => setShowScenarioModal(true)}
                  className="px-3 py-1.5 bg-fd-green text-fd-dark rounded text-sm font-medium hover:bg-fd-green-hover"
                >Run Scenarios</button>
              </div>
            </div>
            <RiskMeasuresPanel tradeId={currentTrade.id} trade={currentTrade} />
          </div>
        )}
        {activeTab === 'marketdata' && currentTrade && (() => {
          console.log('📊 Market Data tab active:', { 
            tradeId: currentTrade.id, 
            loadingRisk, 
            hasRiskMeasures: !!riskMeasures,
            hasMarketSnapshot: !!riskMeasures?.marketDataSnapshot 
          });
          return (
            <div className="mt-4 space-y-6">
              {loadingRisk ? (
                <div className="flex items-center justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-fd-green"></div>
                  <span className="ml-3 text-fd-text-muted">Loading market data snapshot...</span>
                </div>
              ) : (
                <MarketDataPanel marketDataSnapshot={riskMeasures?.marketDataSnapshot} />
              )}
            </div>
          );
        })()}
        <div className="flex justify-end space-x-4 mt-8 pt-4 border-t border-fd-border">
          <button onClick={onClose} className="px-6 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors">Close</button>
        </div>
      </div>
      {currentTrade && (
        <ScenarioRunModal
          tradeId={currentTrade.id}
          isOpen={showScenarioModal}
          onClose={() => setShowScenarioModal(false)}
        />
      )}
      {currentTrade && (
        <CreditEventModal
          isOpen={showCreditEventModal}
          onClose={() => setShowCreditEventModal(false)}
          onSubmit={handleRecordCreditEvent}
          tradeId={currentTrade.id}
          referenceEntity={currentTrade.referenceEntity}
          isLoading={recordingEvent}
        />
      )}
      
      {/* Success Notification */}
      {showSuccessNotification && (
        <div className="fixed top-4 right-4 z-[60] animate-fade-in">
          <div className="bg-fd-dark border-2 border-fd-green rounded-lg shadow-lg p-4 flex items-start gap-3 min-w-[320px] max-w-[480px]">
            <div className="flex-shrink-0 mt-0.5">
              <svg className="w-6 h-6 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
            <div className="flex-1">
              <h4 className="text-fd-text font-semibold mb-1">Success!</h4>
              <p className="text-fd-text-muted text-sm leading-relaxed">{successMessage}</p>
            </div>
            <button
              onClick={() => setShowSuccessNotification(false)}
              className="flex-shrink-0 text-fd-text-muted hover:text-fd-text transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default TradeDetailModal;