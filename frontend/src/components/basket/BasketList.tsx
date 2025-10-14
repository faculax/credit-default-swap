import React, { useState, useEffect } from 'react';
import { basketService } from '../../services/basketService';
import { Basket } from '../../types/basket';

interface BasketListProps {
  onSelectBasket?: (basket: Basket) => void;
  onCreateClick?: () => void;
  refreshTrigger?: number;
}

const BasketList: React.FC<BasketListProps> = ({ onSelectBasket, onCreateClick, refreshTrigger }) => {
  const [baskets, setBaskets] = useState<Basket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadBaskets();
  }, [refreshTrigger]);

  const loadBaskets = async () => {
    try {
      setLoading(true);
      const data = await basketService.getAllBaskets();
      setBaskets(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load baskets');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-fd-text-muted">Loading baskets...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-500/30 text-red-400 px-4 py-3 rounded-md">
        Error: {error}
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-fd-text">Basket Credit Derivatives</h2>
          <p className="text-sm text-fd-text-muted mt-1">
            First-to-Default, N-th-to-Default, and Tranchette instruments
          </p>
        </div>
        {onCreateClick && (
          <button
            onClick={onCreateClick}
            className="px-4 py-2 bg-fd-green text-fd-dark rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green font-medium"
          >
            + Create Basket
          </button>
        )}
      </div>

      {baskets.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-fd-text-muted">No baskets found. Create your first basket to get started.</p>
        </div>
      ) : (
        <div className="bg-fd-darker border border-fd-border rounded-lg overflow-hidden">
          <table className="min-w-full">
            <thead className="bg-fd-dark">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Name
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Constituents
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Currency
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Notional
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Maturity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-fd-border">
              {baskets.map((basket) => (
                <tr
                  key={basket.id}
                  className="hover:bg-fd-dark transition-colors"
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                    {basket.name}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                    <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-fd-green/20 text-fd-green">
                      {basket.type.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                    {basket.constituentCount || basket.constituents?.length || 0}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                    {basket.currency}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                    {basket.notional.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                    {basket.maturityDate}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={() => onSelectBasket && onSelectBasket(basket)}
                      className="text-fd-green hover:text-fd-green-hover font-medium"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BasketList;
