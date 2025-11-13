import {
  formatCurrency,
  formatNumber,
  formatPercentage,
  formatCompactNumber,
  formatDate,
  formatDateTime,
  formatFileSize,
  formatBasisPoints,
  truncateText
} from '../formatters';

describe('formatters utility', () => {
  describe('formatCurrency', () => {
    it('returns $0.00 for null/undefined/empty', () => {
      expect(formatCurrency(null)).toBe('$0.00');
      expect(formatCurrency(undefined)).toBe('$0.00');
      expect(formatCurrency('')).toBe('$0.00');
    });
    it('formats numeric values', () => {
      expect(formatCurrency(1234)).toMatch(/1,234\.00/);
      expect(formatCurrency('56.7')).toMatch(/56\.70/);
    });
    it('returns $0.00 for invalid numeric string', () => {
      expect(formatCurrency('abc')).toBe('$0.00');
    });
  });

  describe('formatNumber', () => {
    it('returns 0 for nullish', () => {
      expect(formatNumber(null)).toBe('0');
      expect(formatNumber(undefined)).toBe('0');
      expect(formatNumber('')).toBe('0');
    });
    it('formats with given decimals', () => {
      expect(formatNumber(12.3456, 3)).toBe('12.346');
    });
    it('handles string input', () => {
      expect(formatNumber('99.9', 1)).toBe('99.9');
    });
  });

  describe('formatPercentage', () => {
    it('returns 0.00% for nullish', () => {
      expect(formatPercentage(null)).toBe('0.00%');
      expect(formatPercentage(undefined)).toBe('0.00%');
    });
    it('formats percentage', () => {
      expect(formatPercentage(0.5)).toBe('0.50%');
    });
  });

  describe('formatCompactNumber', () => {
    it('returns 0 for nullish', () => {
      expect(formatCompactNumber(null)).toBe('0');
    });
    it('formats thousands, millions, billions', () => {
      expect(formatCompactNumber(999)).toMatch(/999\.0/);
      expect(formatCompactNumber(1000)).toMatch(/1\.0K/);
      expect(formatCompactNumber(1_500_000)).toMatch(/1\.5M/);
      expect(formatCompactNumber(2_000_000_000)).toMatch(/2\.0B/);
    });
    it('preserves sign', () => {
      expect(formatCompactNumber(-1200)).toMatch(/-1\.2K/);
    });
  });

  describe('formatDate', () => {
    it('returns dash for invalid', () => {
      expect(formatDate(null)).toBe('-');
      expect(formatDate('not-a-date')).toBe('-');
    });
    it('formats medium by default', () => {
      const out = formatDate('2024-01-15');
      expect(out).toMatch(/2024/);
    });
    it('formats long with weekday', () => {
      const out = formatDate('2024-01-15', 'long');
      expect(out).toMatch(/2024/);
    });
  });

  describe('formatDateTime', () => {
    it('returns dash for invalid', () => {
      expect(formatDateTime(null)).toBe('-');
    });
    it('includes seconds when requested', () => {
      const out = formatDateTime('2024-01-15T12:34:56Z', true);
      expect(out).toMatch(/12:34/);
    });
  });

  describe('formatFileSize', () => {
    it('handles zero/undefined', () => {
      expect(formatFileSize(undefined)).toBe('0 Bytes');
      expect(formatFileSize(0)).toBe('0 Bytes');
    });
    it('formats KB and MB', () => {
      expect(formatFileSize(1024)).toBe('1 KB');
      expect(formatFileSize(1048576)).toBe('1 MB');
    });
  });

  describe('formatBasisPoints', () => {
    it('returns 0.00% for nullish', () => {
      expect(formatBasisPoints(null)).toBe('0.00%');
    });
    it('converts bps to percentage', () => {
      expect(formatBasisPoints(25)).toBe('0.25%');
      expect(formatBasisPoints('100')).toBe('1.00%');
    });
  });

  describe('truncateText', () => {
    it('returns empty for nullish', () => {
      expect(truncateText(null as any, 5)).toBe('');
    });
    it('returns original if shorter than max', () => {
      expect(truncateText('short', 10)).toBe('short');
    });
    it('truncates and adds ellipsis', () => {
      expect(truncateText('abcdefghijklmnopqrstuvwxyz', 5)).toBe('abcde...');
    });
  });
});
