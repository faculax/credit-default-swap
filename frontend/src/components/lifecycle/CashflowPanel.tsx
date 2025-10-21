import React, { useState, useEffect } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';
import { lifecycleService } from '../../services/lifecycleService';
import { CouponPeriod } from '../../types/lifecycle';

interface CashflowPanelProps {
  trade: CDSTradeResponse;
}

interface CashflowEvent {
  id: string;
  type: 'schedule_generation' | 'coupon_payment';
  timestamp: string;
  title: string;
  description: string;
  status?: 'completed' | 'pending' | 'upcoming';
  icon: string;
  color: string;
}

const CashflowPanel: React.FC<CashflowPanelProps> = ({ trade }) => {
  const [coupons, setCoupons] = useState<CouponPeriod[]>([]);
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState<CashflowEvent[]>([]);

  const loadCashflowData = async () => {
    setLoading(true);
    try {
      // Load coupon schedule
      const couponSchedule = await lifecycleService.getCouponSchedule(trade.id);
      setCoupons(couponSchedule);
      
      // Build cashflow events
      buildCashflowEvents(couponSchedule);
    } catch (error) {
      console.error('Failed to load cashflow data:', error);
      // Build basic timeline without coupon data
      buildCashflowEvents([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCashflowData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trade.id]);

  const buildCashflowEvents = (couponSchedule: CouponPeriod[]) => {
    const cashflowEvents: CashflowEvent[] = [];
    const now = new Date();
    const isTradeEnded = trade.tradeStatus !== 'ACTIVE' && trade.tradeStatus !== 'PENDING';

    // 1. Coupon Schedule Generation (if exists)
    if (couponSchedule.length > 0) {
      const firstCoupon = couponSchedule[0];
      cashflowEvents.push({
        id: 'schedule_gen',
        type: 'schedule_generation',
        timestamp: firstCoupon.createdAt,
        title: 'Coupon Schedule Generated',
        description: `${couponSchedule.length} coupon periods created`,
        status: 'completed',
        icon: '📅',
        color: 'rgb(0, 232, 247)' // cyan
      });

      // 2. Coupon Payments - Filter out unpaid coupons if trade has ended
      couponSchedule.forEach((coupon, index) => {
        const paymentDate = new Date(coupon.paymentDate);
        let status: 'completed' | 'pending' | 'upcoming' = 'upcoming';
        
        if (coupon.paid && coupon.paidAt) {
          status = 'completed';
        } else if (paymentDate <= now) {
          status = 'pending';
        }

        // Skip unpaid coupons if trade lifecycle has ended
        if (isTradeEnded && !coupon.paid) {
          return;
        }

        cashflowEvents.push({
          id: `coupon-${coupon.id}`,
          type: 'coupon_payment',
          timestamp: coupon.paid && coupon.paidAt ? coupon.paidAt : coupon.paymentDate,
          title: `Coupon Payment ${index + 1}`,
          description: coupon.paid 
            ? `Paid ${formatCurrency(coupon.couponAmount || 0, trade.currency)} on ${formatDate(coupon.paidAt!)}`
            : `Due ${formatDate(coupon.paymentDate)} - ${formatCurrency(coupon.couponAmount || 0, trade.currency)}`,
          status,
          icon: coupon.paid ? '💰' : '⏰',
          color: coupon.paid ? 'rgb(30, 230, 190)' : 'rgb(60, 75, 97)'
        });
      });
    }

    // Sort by timestamp
    cashflowEvents.sort((a, b) => 
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    setEvents(cashflowEvents);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
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

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-fd-green"></div>
        <span className="ml-3 text-fd-text-muted">Loading cashflow data...</span>
      </div>
    );
  }

  return (
    <div className="mt-4">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-fd-text mb-2">Cashflow Timeline</h3>
        <p className="text-fd-text-muted text-sm">
          Coupon schedule generation and payment events for CDS-{trade.id}
        </p>
      </div>

      {/* Stats Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Total Coupons</div>
          <div className="text-fd-text text-2xl font-bold">{coupons.length}</div>
        </div>
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Coupons Paid</div>
          <div className="text-fd-green text-2xl font-bold">
            {coupons.filter(c => c.paid).length}
          </div>
        </div>
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Pending Payments</div>
          <div className="text-yellow-400 text-2xl font-bold">
            {coupons.filter(c => !c.paid && new Date(c.paymentDate) <= new Date()).length}
          </div>
        </div>
        <div className="bg-fd-dark rounded-lg p-4 border border-fd-border">
          <div className="text-fd-text-muted text-xs uppercase mb-1">Upcoming</div>
          <div className="text-fd-text text-2xl font-bold">
            {coupons.filter(c => !c.paid && new Date(c.paymentDate) > new Date()).length}
          </div>
        </div>
      </div>

      {events.length === 0 ? (
        <div className="bg-fd-dark rounded-lg p-6 text-center border border-fd-border">
          <div className="w-12 h-12 bg-fd-border rounded-full flex items-center justify-center mx-auto mb-3">
            <svg className="w-6 h-6 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
            </svg>
          </div>
          <p className="text-fd-text-muted">No coupon schedule has been generated for this trade</p>
        </div>
      ) : (
        <>
          {/* Timeline Visualization */}
          <div className="relative">
            {/* Vertical line */}
            <div className="absolute left-8 top-0 bottom-0 w-0.5 bg-fd-border"></div>

            {/* Cashflow Events */}
            <div className="space-y-6">
              {events.map((event, index) => (
                <div key={event.id} className="relative pl-20">
                  {/* Icon */}
                  <div 
                    className="absolute left-0 w-16 h-16 rounded-full flex items-center justify-center text-3xl border-4 border-fd-darker"
                    style={{ backgroundColor: event.color }}
                  >
                    {event.icon}
                  </div>

                  {/* Arrow connector */}
                  <svg 
                    className="absolute left-16 top-6" 
                    width="16" 
                    height="4" 
                    viewBox="0 0 16 4"
                  >
                    <path 
                      d="M0 2 L14 2 L12 0 M14 2 L12 4" 
                      stroke="rgb(60, 75, 97)" 
                      strokeWidth="1" 
                      fill="none"
                    />
                  </svg>

                  {/* Event Card */}
                  <div 
                    className={`bg-fd-dark rounded-lg p-4 border-l-4 shadow-lg transition-all hover:shadow-xl hover:scale-[1.01]`}
                    style={{ borderLeftColor: event.color }}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <h4 className="text-fd-text font-semibold text-lg">{event.title}</h4>
                        <p className="text-fd-text-muted text-sm mt-1">{event.description}</p>
                      </div>
                      {event.status && (
                        <span 
                          className={`inline-flex items-center px-2.5 py-0.5 rounded text-xs font-medium ${
                            event.status === 'completed' 
                              ? 'bg-green-500/20 text-green-400'
                              : event.status === 'pending'
                              ? 'bg-yellow-500/20 text-yellow-400'
                              : 'bg-blue-500/20 text-blue-400'
                          }`}
                        >
                          {event.status === 'completed' ? '✓ Completed' : 
                           event.status === 'pending' ? '◷ Pending' : '→ Upcoming'}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center text-xs text-fd-text-muted mt-3">
                      <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                      {formatDateTime(event.timestamp)}
                    </div>
                  </div>

                  {/* Connection line to next event */}
                  {index < events.length - 1 && (
                    <div className="absolute left-8 top-20 w-0.5 h-6 bg-fd-border"></div>
                  )}
                </div>
              ))}
            </div>

            {/* End marker */}
            {events.length > 0 && (
              <div className="relative pl-20 mt-6">
                <div className="absolute left-0 w-16 h-16 rounded-full flex items-center justify-center text-2xl border-4 border-fd-darker bg-fd-border">
                  {trade.tradeStatus === 'ACTIVE' ? '▶' : '■'}
                </div>
                
                {/* Arrow connector */}
                <svg 
                  className="absolute left-16 top-6" 
                  width="16" 
                  height="4" 
                  viewBox="0 0 16 4"
                >
                  <path 
                    d="M0 2 L14 2 L12 0 M14 2 L12 4" 
                    stroke="rgb(60, 75, 97)" 
                    strokeWidth="1" 
                    fill="none"
                  />
                </svg>

                {/* End marker card */}
                <div className="bg-fd-dark rounded-lg p-4 border-l-4 border-fd-border shadow-lg">
                  <h4 className="text-fd-text font-semibold text-lg mb-2">
                    {trade.tradeStatus === 'ACTIVE' ? 'Cashflows Ongoing' : 'Cashflows Ended'}
                  </h4>
                  <p className="text-fd-text-muted text-sm">
                    {trade.tradeStatus === 'ACTIVE' 
                      ? 'More coupon payments may occur as trade continues...'
                      : `All cashflows settled - Status: ${trade.tradeStatus.replace(/_/g, ' ')}`}
                  </p>
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default CashflowPanel;
