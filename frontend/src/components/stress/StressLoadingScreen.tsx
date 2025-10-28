import React, { useState, useEffect } from 'react';

interface StressLoadingScreenProps {
  scenarioCount: number;
}

const StressLoadingScreen: React.FC<StressLoadingScreenProps> = ({ scenarioCount }) => {
  const [currentPhase, setCurrentPhase] = useState(0);
  
  const phases = [
    { label: 'Calculating scenarios', emoji: '🧮' },
    { label: 'Interacting with ORE', emoji: '⚙️' },
    { label: 'Aggregating responses', emoji: '📊' },
    { label: 'Finalizing results', emoji: '✨' }
  ];

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentPhase((prev) => (prev + 1) % phases.length);
    }, 3000);

    return () => clearInterval(interval);
  }, [phases.length]);

  return (
    <div className="flex flex-col items-center justify-center py-16 space-y-6">
      {/* Animated spinner */}
      <div className="relative">
        <div className="animate-spin rounded-full h-20 w-20 border-b-4 border-fd-green"></div>
        <div className="absolute inset-0 flex items-center justify-center text-3xl">
          {phases[currentPhase].emoji}
        </div>
      </div>

      {/* Current phase */}
      <div className="text-center space-y-2">
        <h3 className="text-xl font-semibold text-fd-text">
          {phases[currentPhase].label}
        </h3>
        <p className="text-fd-text-muted text-sm">
          Running {scenarioCount} scenario{scenarioCount !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Phase indicators */}
      <div className="flex space-x-2">
        {phases.map((phase, index) => (
          <div
            key={index}
            className={`h-2 w-2 rounded-full transition-all duration-300 ${
              index === currentPhase
                ? 'bg-fd-green w-8'
                : 'bg-fd-border'
            }`}
          />
        ))}
      </div>

      {/* Progress bar */}
      <div className="w-full max-w-md">
        <div className="h-1 bg-fd-border rounded-full overflow-hidden">
          <div
            className="h-full bg-fd-green transition-all duration-1000"
            style={{
              width: `${((currentPhase + 1) / phases.length) * 100}%`,
            }}
          />
        </div>
      </div>

      {/* Tip */}
      <p className="text-xs text-fd-text-muted italic mt-4">
        This may take up to 30 seconds...
      </p>
    </div>
  );
};

export default StressLoadingScreen;
