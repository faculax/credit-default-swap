import React, { useState, useEffect, useCallback } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';
import { CreditEvent, creditEventService } from '../../services/creditEventService';
import { CouponSchedulePanel } from '../lifecycle/CouponSchedulePanel';
import { AccrualHistoryPanel } from '../lifecycle/AccrualHistoryPanel';
import { NotionalAdjustmentModal } from '../lifecycle/NotionalAdjustmentModal';
import { AmendTradeModal } from '../lifecycle/AmendTradeModal';
import RiskMeasuresPanel from '../risk/RiskMeasuresPanel';
import ScenarioRunModal from '../risk/ScenarioRunModal';
import RegressionStatusBadge from '../risk/RegressionStatusBadge';

interface TradeDetailModalProps {
  isOpen: boolean;
  trade: CDSTradeResponse | null;
  onClose: () => void;
}

const TradeDetailModal: React.FC<TradeDetailModalProps> = ({ isOpen, trade, onClose }) => {
  const [creditEvents, setCreditEvents] = useState<CreditEvent[]>([]);
  const [loadingEvents, setLoadingEvents] = useState(false);
  // Lifecycle UI state additions
  const [activeTab, setActiveTab] = useState<'details' | 'events' | 'lifecycle' | 'risk'>('details');
  const [showNotionalModal, setShowNotionalModal] = useState(false);
  const [showAmendModal, setShowAmendModal] = useState(false);
  const [showScenarioModal, setShowScenarioModal] = useState(false);
  const [notionalAdjustmentsVersion, setNotionalAdjustmentsVersion] = useState(0); // trigger re-renders if needed

  const loadCreditEvents = useCallback(async () => {
    if (!trade) return;
    
    setLoadingEvents(true);
    try {
      const events = await creditEventService.getCreditEventsForTrade(trade.id);
      setCreditEvents(events);
    } catch (error) {
      console.error('Failed to load credit events:', error);
      setCreditEvents([]);
    } finally {
      setLoadingEvents(false);
    }
  }, [trade]);

  useEffect(() => {
    if (isOpen && trade) {
      loadCreditEvents();
    }
  }, [isOpen, trade, loadCreditEvents]);

  if (!isOpen || !trade) return null;

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
            { key: 'events', label: 'Credit Events' },
            { key: 'lifecycle', label: 'Lifecycle' },
            { key: 'risk', label: 'Risk' }
          ].map(t => (
            <button
              key={t.key}
              onClick={() => setActiveTab(t.key as any)}
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
                  <span className="text-fd-green font-mono font-semibold">CDS-{trade.id}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-fd-text-muted">Created:</span>
                  <span className="text-fd-text">{formatDateTime(trade.createdAt)}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-fd-text-muted">Last Updated:</span>
                  <span className="text-fd-text">
                    {trade.updatedAt ? formatDateTime(trade.updatedAt) : 'Never'}
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
                    <span className="text-fd-text font-medium">{trade.referenceEntity}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Counterparty:</span>
                    <span className="text-fd-text font-medium">{trade.counterparty}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Direction:</span>
                    <span className={`font-medium ${trade.buySellProtection === 'BUY' ? 'text-blue-400' : 'text-orange-400'}`}>
                      {trade.buySellProtection === 'BUY' ? 'Buy Protection' : 'Sell Protection'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Notional Amount:</span>
                    <span className="text-fd-text font-semibold text-lg">
                      {formatCurrency(trade.notionalAmount, trade.currency)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Spread:</span>
                    <span className="text-fd-text font-medium">{trade.spread} bps</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Currency:</span>
                    <span className="text-fd-text font-medium">{trade.currency}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Status:</span>
                    <span className="text-fd-green font-medium">{trade.tradeStatus.replace(/_/g, ' ')}</span>
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
                    <span className="text-fd-text">{formatDate(trade.tradeDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Effective Date:</span>
                    <span className="text-fd-text">{formatDate(trade.effectiveDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Maturity Date:</span>
                    <span className="text-fd-text">{formatDate(trade.maturityDate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Accrual Start Date:</span>
                    <span className="text-fd-text">{formatDate(trade.accrualStartDate)}</span>
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
                    <span className="text-fd-text">{trade.premiumFrequency.replace('_', ' ')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Day Count Convention:</span>
                    <span className="text-fd-text">{trade.dayCountConvention.replace('_', '/')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-fd-text-muted">Payment Calendar:</span>
                    <span className="text-fd-text">{trade.paymentCalendar}</span>
                  </div>
                  {trade.restructuringClause && (
                    <div className="flex justify-between">
                      <span className="text-fd-text-muted">Restructuring Clause:</span>
                      <span className="text-fd-text">{trade.restructuringClause.replace(/_/g, ' ')}</span>
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
                    <div className="font-mono text-fd-green text-lg">CDS-{trade.id}</div>
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
        {activeTab === 'events' && (
          <div className="mt-4">
            <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2 mb-4">Credit Events</h3>
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
                  <div key={event.id} className="bg-fd-dark rounded-lg p-4 border border-fd-border">
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-red-500/20 rounded-full flex items-center justify-center">
                          <svg className="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
                          </svg>
                        </div>
                        <div>
                          <h4 className="text-fd-text font-medium">{event.eventType.replace(/_/g, ' ')}</h4>
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
  {activeTab === 'lifecycle' && trade && (
          <div className="mt-2 space-y-8">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-fd-text">Lifecycle Management</h3>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setShowAmendModal(true)}
                  className="px-3 py-1.5 text-xs bg-fd-dark border border-fd-border rounded text-fd-text-muted hover:text-fd-text"
                >Amend Trade</button>
                <button
                  onClick={() => setShowNotionalModal(true)}
                  className="px-3 py-1.5 text-xs bg-fd-green text-fd-dark rounded font-medium hover:bg-fd-green-hover"
                >Adjust Notional</button>
              </div>
            </div>
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
              <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
                <CouponSchedulePanel tradeId={trade.id} />
              </div>
              <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
                <AccrualHistoryPanel tradeId={trade.id} />
              </div>
            </div>
            {/* Placeholders for future: Amendments list, Notional adjustments history */}
            <div className="bg-fd-darker border border-dashed border-fd-border rounded p-4 text-fd-text-muted text-xs">
              Amendments & Notional history panels can be added here next.
            </div>
          </div>
        )}
        {activeTab === 'risk' && trade && (
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
            <RiskMeasuresPanel tradeId={trade.id} />
          </div>
        )}
        <div className="flex justify-end space-x-4 mt-8 pt-4 border-t border-fd-border">
          <button onClick={onClose} className="px-6 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors">Close</button>
        </div>
      </div>
      {trade && (
        <>
          <NotionalAdjustmentModal
            tradeId={trade.id}
            isOpen={showNotionalModal}
            onClose={() => setShowNotionalModal(false)}
            onCreated={() => setNotionalAdjustmentsVersion(v => v + 1)}
          />
          <AmendTradeModal
            tradeId={trade.id}
            isOpen={showAmendModal}
            onClose={() => setShowAmendModal(false)}
            onCreated={() => {/* future refresh logic */}}
          />
          <ScenarioRunModal
            tradeId={trade.id}
            isOpen={showScenarioModal}
            onClose={() => setShowScenarioModal(false)}
          />
        </>
      )}
    </div>
  );
};

export default TradeDetailModal;