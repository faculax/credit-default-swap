import React, { useState } from 'react';
import { portfolioService } from '../../services/portfolioService';

interface CreatePortfolioModalProps {
  onClose: () => void;
  onSuccess: () => void;
}

const CreatePortfolioModal: React.FC<CreatePortfolioModalProps> = ({ onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setError('Portfolio name is required');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      await portfolioService.createPortfolio(name.trim(), description.trim() || undefined);
      onSuccess();
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Failed to create portfolio';
      setError(errorMessage);
      console.error('Error creating portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-fd-darker rounded-lg shadow-xl border border-fd-border max-w-md w-full mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 py-4 border-b border-fd-border">
          <h3 className="text-lg font-semibold text-fd-text">Create New Portfolio</h3>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded text-sm">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="portfolio-name" className="block text-sm font-medium text-fd-text mb-1">
              Portfolio Name *
            </label>
            <input
              id="portfolio-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted"
              placeholder="e.g., Tech Portfolio 2025"
              maxLength={60}
              disabled={loading}
              autoFocus
            />
          </div>

          <div>
            <label
              htmlFor="portfolio-description"
              className="block text-sm font-medium text-fd-text mb-1"
            >
              Description
            </label>
            <textarea
              id="portfolio-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 bg-fd-dark border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent placeholder-fd-text-muted resize-none"
              placeholder="Optional description"
              rows={3}
              disabled={loading}
            />
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-darker focus:outline-none focus:ring-2 focus:ring-fd-green"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-fd-dark bg-fd-green border border-transparent rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green disabled:bg-fd-green/50 disabled:cursor-not-allowed"
              disabled={loading}
            >
              {loading ? 'Creating...' : 'Create Portfolio'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreatePortfolioModal;
