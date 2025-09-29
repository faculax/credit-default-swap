import React, { useState, useEffect } from 'react';

const TopBar: React.FC = () => {
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  return (
    <nav className="bg-fd-darker border-b border-fd-border py-3 px-6">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 bg-fd-green rounded-full animate-pulse"></div>
            <span className="text-fd-green font-medium">CREDIT DEFAULT SWAP PLATFORM</span>
          </div>
        </div>
        
        <div className="flex items-center space-x-4">
          <span className="text-fd-text-muted text-sm">
            {currentTime.toLocaleDateString('en-US', { 
              weekday: 'short', 
              year: 'numeric', 
              month: 'short', 
              day: 'numeric' 
            })}
          </span>
          <span className="text-fd-green font-mono text-lg">
            {currentTime.toLocaleTimeString('en-US', {
              hour12: false,
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            })}
          </span>
        </div>
      </div>
    </nav>
  );
};

export default TopBar;