/**
 * Utility functions for formatting numbers, currencies, and other data types
 */

/**
 * Format a number as currency (USD by default)
 */
export const formatCurrency = (
  value: number | string | null | undefined,
  currency: string = 'USD',
  locale: string = 'en-US'
): string => {
  if (value === null || value === undefined || value === '') {
    return '$0.00';
  }

  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return '$0.00';
  }

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numValue);
};

/**
 * Format a number with specified decimal places
 */
export const formatNumber = (
  value: number | string | null | undefined,
  decimals: number = 2,
  locale: string = 'en-US'
): string => {
  if (value === null || value === undefined || value === '') {
    return '0';
  }

  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return '0';
  }

  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(numValue);
};

/**
 * Format a number as percentage
 */
export const formatPercentage = (
  value: number | string | null | undefined,
  decimals: number = 2
): string => {
  if (value === null || value === undefined || value === '') {
    return '0.00%';
  }

  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return '0.00%';
  }

  return `${formatNumber(numValue, decimals)}%`;
};

/**
 * Format large numbers with K, M, B suffixes
 */
export const formatCompactNumber = (
  value: number | string | null | undefined,
  decimals: number = 1
): string => {
  if (value === null || value === undefined || value === '') {
    return '0';
  }

  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return '0';
  }

  const abs = Math.abs(numValue);
  const sign = numValue < 0 ? '-' : '';
  
  if (abs >= 1e9) {
    return `${sign}${formatNumber(abs / 1e9, decimals)}B`;
  } else if (abs >= 1e6) {
    return `${sign}${formatNumber(abs / 1e6, decimals)}M`;
  } else if (abs >= 1e3) {
    return `${sign}${formatNumber(abs / 1e3, decimals)}K`;
  } else {
    return `${sign}${formatNumber(abs, decimals)}`;
  }
};

/**
 * Format a date to a readable string
 */
export const formatDate = (
  date: Date | string | null | undefined,
  format: 'short' | 'medium' | 'long' = 'medium'
): string => {
  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const options: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: format === 'short' ? 'numeric' : format === 'medium' ? 'short' : 'long',
    day: 'numeric'
  };

  if (format === 'long') {
    options.weekday = 'long';
  }

  return dateObj.toLocaleDateString('en-US', options);
};

/**
 * Format a date and time to a readable string
 */
export const formatDateTime = (
  date: Date | string | null | undefined,
  includeSeconds: boolean = false
): string => {
  if (!date) {
    return '-';
  }

  const dateObj = typeof date === 'string' ? new Date(date) : date;
  
  if (isNaN(dateObj.getTime())) {
    return '-';
  }

  const dateOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  };

  const timeOptions: Intl.DateTimeFormatOptions = {
    hour: '2-digit',
    minute: '2-digit',
    ...(includeSeconds && { second: '2-digit' })
  };

  const datePart = dateObj.toLocaleDateString('en-US', dateOptions);
  const timePart = dateObj.toLocaleTimeString('en-US', timeOptions);

  return `${datePart} ${timePart}`;
};

/**
 * Format file size in bytes to human readable format
 */
export const formatFileSize = (bytes: number | null | undefined): string => {
  if (!bytes || bytes === 0) {
    return '0 Bytes';
  }

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
};

/**
 * Format basis points to percentage
 */
export const formatBasisPoints = (
  bps: number | string | null | undefined,
  decimals: number = 2
): string => {
  if (bps === null || bps === undefined || bps === '') {
    return '0.00%';
  }

  const numValue = typeof bps === 'string' ? parseFloat(bps) : bps;
  
  if (isNaN(numValue)) {
    return '0.00%';
  }

  const percentage = numValue / 10000; // Convert basis points to percentage
  return formatPercentage(percentage * 100, decimals);
};

/**
 * Truncate text with ellipsis
 */
export const truncateText = (
  text: string | null | undefined,
  maxLength: number
): string => {
  if (!text) {
    return '';
  }

  if (text.length <= maxLength) {
    return text;
  }

  return `${text.substring(0, maxLength)}...`;
};