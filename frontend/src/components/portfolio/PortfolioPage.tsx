import React, { useState } from 'react';
import PortfolioList from './PortfolioList';
import PortfolioDetail from './PortfolioDetail';
import { CdsPortfolio } from '../../services/portfolioService';

const PortfolioPage: React.FC = () => {
  const [selectedPortfolio, setSelectedPortfolio] = useState<CdsPortfolio | null>(null);

  const handlePortfolioSelect = (portfolio: CdsPortfolio) => {
    setSelectedPortfolio(portfolio);
  };

  const handleBack = () => {
    setSelectedPortfolio(null);
  };

  return (
    <div className="px-4 py-6">
      {selectedPortfolio ? (
        <PortfolioDetail portfolioId={selectedPortfolio.id} onBack={handleBack} />
      ) : (
        <PortfolioList onPortfolioSelect={handlePortfolioSelect} />
      )}
    </div>
  );
};

export default PortfolioPage;
