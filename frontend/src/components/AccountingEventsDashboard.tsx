import React, { useState, useEffect } from 'react';
import accountingService from '../services/accountingService';
import {
  AccountingEvent,
  AccountingSummary,
  EventStatus,
  EventType,
} from '../types/accounting';

const AccountingEventsDashboard: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [summary, setSummary] = useState<AccountingSummary | null>(null);
  const [events, setEvents] = useState<AccountingEvent[]>([]);
  const [filteredEvents, setFilteredEvents] = useState<AccountingEvent[]>([]);
  const [activeFilter, setActiveFilter] = useState<'all' | 'pending' | 'posted'>('all');
  const [selectedEventType, setSelectedEventType] = useState<string>('all');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedEvents, setSelectedEvents] = useState<Set<number>>(new Set());
  const [isGenerating, setIsGenerating] = useState(false);
  const [isPosting, setIsPosting] = useState(false);

  const fetchData = React.useCallback(async () => {
    if (!selectedDate) return;

    setLoading(true);
    setError(null);

    try {
      // Fetch summary
      const summaryData = await accountingService.getSummary(selectedDate);
      setSummary(summaryData);

      // Fetch events
      const eventsData = await accountingService.getEventsByDate(selectedDate);
      setEvents(eventsData);
    } catch (err) {
      console.error('Error fetching accounting data:', err);
      setError('Failed to fetch accounting data. Events may not be generated yet.');
      setSummary(null);
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

  const applyFilters = React.useCallback(() => {
    let filtered = events;

    // Filter by status
    if (activeFilter === 'pending') {
      filtered = filtered.filter((e) => e.status === EventStatus.PENDING);
    } else if (activeFilter === 'posted') {
      filtered = filtered.filter((e) => e.status === EventStatus.POSTED);
    }

    // Filter by event type
    if (selectedEventType !== 'all') {
      filtered = filtered.filter((e) => e.eventType === selectedEventType);
    }

    setFilteredEvents(filtered);
  }, [events, activeFilter, selectedEventType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const handleGenerateEvents = async () => {
    setIsGenerating(true);
    setError(null);

    try {
      const result = await accountingService.generateEvents(selectedDate);
      alert(`Success! Generated ${result.eventCount} accounting events.`);
      await fetchData();
    } catch (err) {
      console.error('Error generating events:', err);
      setError('Failed to generate accounting events. Check if P&L data exists.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleToggleEvent = (eventId: number) => {
    const newSelection = new Set(selectedEvents);
    if (newSelection.has(eventId)) {
      newSelection.delete(eventId);
    } else {
      newSelection.add(eventId);
    }
    setSelectedEvents(newSelection);
  };

  const handleToggleAll = () => {
    if (selectedEvents.size === filteredEvents.length) {
      setSelectedEvents(new Set());
    } else {
      const allIds = new Set(filteredEvents.map((e) => e.id));
      setSelectedEvents(allIds);
    }
  };

  const handleBulkPost = async () => {
    if (selectedEvents.size === 0) {
      alert('Please select events to post');
      return;
    }

    const glBatchId = prompt('Enter GL Batch ID:');
    if (!glBatchId) return;

    const postedBy = prompt('Posted by (username):', 'system');
    if (!postedBy) return;

    setIsPosting(true);
    setError(null);

    try {
      const result = await accountingService.markEventsAsPostedBatch({
        glBatchId,
        postedBy,
        eventIds: Array.from(selectedEvents),
      });

      alert(`Success! Posted ${result.successCount} events. Failed: ${result.failureCount}`);
      setSelectedEvents(new Set());
      await fetchData();
    } catch (err) {
      console.error('Error posting events:', err);
      setError('Failed to post events to GL.');
    } finally {
      setIsPosting(false);
    }
  };

  const formatCurrency = (value: number | undefined | null) => {
    if (value === undefined || value === null) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatDateTime = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  };

  const getStatusBadge = (status: EventStatus) => {
    const colors = {
      PENDING: 'bg-yellow-400/20 text-yellow-400',
      POSTED: 'bg-green-400/20 text-green-400',
      FAILED: 'bg-red-400/20 text-red-400',
      CANCELLED: 'bg-gray-400/20 text-gray-400',
    };

    return (
      <span className={`px-2 py-1 text-xs rounded font-medium ${colors[status]}`}>
        {status}
      </span>
    );
  };

  const getEventTypeLabel = (eventType: EventType) => {
    const labels: Record<EventType, string> = {
      [EventType.MTM_VALUATION]: 'MTM Valuation',
      [EventType.MTM_PNL_UNREALIZED]: 'MTM P&L',
      [EventType.ACCRUED_INTEREST]: 'Accrued Interest',
      [EventType.REALIZED_PNL]: 'Realized P&L',
      [EventType.CASH_SETTLEMENT]: 'Cash Settlement',
      [EventType.TERMINATION]: 'Termination',
      [EventType.NOVATION]: 'Novation',
      [EventType.CREDIT_EVENT]: 'Credit Event',
    };
    return labels[eventType] || eventType;
  };

  return (
    <div className="p-6 max-w-screen-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-fd-text mb-2">
          Accounting Events Dashboard
        </h1>
        <p className="text-fd-muted">
          View and manage accounting journal entries generated from valuations
        </p>
      </div>

      {/* Date Selector and Actions */}
      <div className="bg-fd-card border border-fd-border rounded-lg p-6 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex-1 min-w-[200px]">
            <label htmlFor="event-date" className="block text-sm font-medium text-fd-text mb-2">
              Event Date
            </label>
            <input
              type="date"
              id="event-date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full px-4 py-2 border border-fd-border rounded-md bg-fd-card text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-primary"
            />
          </div>

          <div className="flex gap-2 items-end">
            <button
              onClick={handleGenerateEvents}
              disabled={isGenerating || loading}
              className="px-4 py-2 bg-fd-primary text-white rounded-md hover:bg-fd-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isGenerating ? 'Generating...' : 'Generate Events'}
            </button>

            <button
              onClick={handleBulkPost}
              disabled={selectedEvents.size === 0 || isPosting || loading}
              className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isPosting ? 'Posting...' : `Post Selected (${selectedEvents.size})`}
            </button>

            <button
              onClick={fetchData}
              disabled={loading}
              className="px-4 py-2 bg-fd-card border border-fd-border text-fd-text rounded-md hover:bg-fd-hover disabled:opacity-50 transition-colors"
            >
              Refresh
            </button>
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <div className="bg-fd-card border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-muted mb-1">Total Debits</div>
            <div className="text-2xl font-bold text-fd-text">
              {formatCurrency(summary.totalDebits)}
            </div>
          </div>

          <div className="bg-fd-card border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-muted mb-1">Total Credits</div>
            <div className="text-2xl font-bold text-fd-text">
              {formatCurrency(summary.totalCredits)}
            </div>
          </div>

          <div className="bg-fd-card border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-muted mb-1">Balance Status</div>
            <div className={`text-2xl font-bold ${summary.balanced ? 'text-green-400' : 'text-red-400'}`}>
              {summary.balanced ? 'BALANCED ✓' : 'UNBALANCED ✗'}
            </div>
            {!summary.balanced && (
              <div className="text-xs text-red-400 mt-1">
                Diff: {formatCurrency(Math.abs(summary.totalDebits - summary.totalCredits))}
              </div>
            )}
          </div>

          <div className="bg-fd-card border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-muted mb-1">Events</div>
            <div className="text-2xl font-bold text-fd-text">{summary.totalEvents}</div>
            <div className="text-xs text-fd-muted mt-1">
              <span className="text-yellow-400">{summary.pendingEvents} pending</span>
              {' • '}
              <span className="text-green-400">{summary.postedEvents} posted</span>
            </div>
          </div>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-red-900/20 border border-red-500 text-red-400 rounded-lg p-4 mb-6">
          {error}
        </div>
      )}

      {/* Filters and Table */}
      <div className="bg-fd-card border border-fd-border rounded-lg">
        {/* Filter Tabs */}
        <div className="flex flex-wrap items-center justify-between border-b border-fd-border p-4">
          <div className="flex gap-2">
            {['all', 'pending', 'posted'].map((filter) => (
              <button
                key={filter}
                onClick={() => setActiveFilter(filter as any)}
                className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                  activeFilter === filter
                    ? 'bg-fd-primary text-white'
                    : 'text-fd-muted hover:text-fd-text hover:bg-fd-hover'
                }`}
              >
                {filter.charAt(0).toUpperCase() + filter.slice(1)}
              </button>
            ))}
          </div>

          <div>
            <select
              value={selectedEventType}
              onChange={(e) => setSelectedEventType(e.target.value)}
              className="px-4 py-2 border border-fd-border rounded-md bg-fd-card text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-primary"
            >
              <option value="all">All Event Types</option>
              {Object.values(EventType).map((type) => (
                <option key={type} value={type}>
                  {getEventTypeLabel(type)}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Events Table */}
        <div className="p-6">
          {loading ? (
            <div className="text-center py-12 text-fd-muted">Loading accounting events...</div>
          ) : filteredEvents.length === 0 ? (
            <div className="text-center py-12 text-fd-muted">
              No accounting events found. Generate events first.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-sm text-fd-muted border-b border-fd-border">
                    <th className="pb-3 pr-4">
                      <input
                        type="checkbox"
                        checked={selectedEvents.size === filteredEvents.length && filteredEvents.length > 0}
                        onChange={handleToggleAll}
                        className="rounded"
                      />
                    </th>
                    <th className="pb-3 pr-4">ID</th>
                    <th className="pb-3 pr-4">Type</th>
                    <th className="pb-3 pr-4">Trade</th>
                    <th className="pb-3 pr-4">Account</th>
                    <th className="pb-3 pr-4 text-right">Debit</th>
                    <th className="pb-3 pr-4 text-right">Credit</th>
                    <th className="pb-3 pr-4">Status</th>
                    <th className="pb-3 pr-4">GL Batch</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredEvents.map((event) => (
                    <tr key={event.id} className="border-b border-fd-border hover:bg-fd-hover">
                      <td className="py-3 pr-4">
                        <input
                          type="checkbox"
                          checked={selectedEvents.has(event.id)}
                          onChange={() => handleToggleEvent(event.id)}
                          disabled={event.status !== EventStatus.PENDING}
                          className="rounded"
                        />
                      </td>
                      <td className="py-3 pr-4">
                        <span className="font-mono text-sm">{event.id}</span>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="text-sm">{getEventTypeLabel(event.eventType)}</div>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="text-sm">{event.tradeId}</div>
                        <div className="text-xs text-fd-muted">{event.referenceEntityName}</div>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="text-sm font-mono">{event.accountCode}</div>
                        <div className="text-xs text-fd-muted">{event.accountName}</div>
                      </td>
                      <td className="py-3 pr-4 text-right font-mono text-sm">
                        {event.debitAmount > 0 ? formatCurrency(event.debitAmount) : '-'}
                      </td>
                      <td className="py-3 pr-4 text-right font-mono text-sm">
                        {event.creditAmount > 0 ? formatCurrency(event.creditAmount) : '-'}
                      </td>
                      <td className="py-3 pr-4">{getStatusBadge(event.status)}</td>
                      <td className="py-3 pr-4">
                        <div className="text-sm font-mono">{event.glBatchId || '-'}</div>
                        {event.postedAt && (
                          <div className="text-xs text-fd-muted">{formatDateTime(event.postedAt)}</div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AccountingEventsDashboard;
