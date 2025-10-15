import React, { useState } from 'react';
import BasketList from './BasketList';
import BasketCreationModal from './BasketCreationModal';
import BasketDetailView from './BasketDetailView';
import { Basket } from '../../types/basket';

const BasketPage: React.FC = () => {
  const [selectedBasket, setSelectedBasket] = useState<Basket | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleSelectBasket = (basket: Basket) => {
    setSelectedBasket(basket);
  };

  const handleCreateClick = () => {
    setShowCreateModal(true);
  };

  const handleCreateSuccess = (basket: Basket) => {
    setRefreshTrigger(prev => prev + 1);
    // Optionally open the newly created basket
    // setSelectedBasket(basket);
  };

  return (
    <div className="max-w-7xl mx-auto">
      <BasketList 
        onSelectBasket={handleSelectBasket}
        onCreateClick={handleCreateClick}
        refreshTrigger={refreshTrigger}
      />
      
      {/* Basket Creation Modal */}
      <BasketCreationModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={handleCreateSuccess}
      />
      
      {/* Basket Detail View */}
      {selectedBasket && (
        <BasketDetailView
          basket={selectedBasket}
          onClose={() => setSelectedBasket(null)}
        />
      )}
    </div>
  );
};

export default BasketPage;
