import React, { useEffect, useRef } from 'react';
import { simulationGlossary } from '../../../data/simulationGlossary';

interface MetricsGlossaryModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const MetricsGlossaryModal: React.FC<MetricsGlossaryModalProps> = ({ isOpen, onClose }) => {
  const modalRef = useRef<HTMLDivElement>(null);

  // Focus trap and Escape key handler
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }

      // Tab key focus trap
      if (e.key === 'Tab') {
        const focusableElements = modalRef.current?.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );

        if (!focusableElements || focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey && document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        } else if (!e.shiftKey && document.activeElement === lastElement) {
          e.preventDefault();
          firstElement.focus();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Auto-focus modal on open
  useEffect(() => {
    if (isOpen && modalRef.current) {
      modalRef.current.focus();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="glossary-modal-title"
    >
      <div
        ref={modalRef}
        className="bg-fd-darker rounded-lg border border-fd-border shadow-2xl max-w-4xl max-h-[80vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
        tabIndex={-1}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-fd-border">
          <h2 id="glossary-modal-title" className="text-2xl font-medium text-fd-text">
            Simulation Metrics Glossary
          </h2>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors p-2 rounded hover:bg-fd-border"
            aria-label="Close glossary"
          >
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto p-6 space-y-6">
          {simulationGlossary.map((term) => (
            <div key={term.term} className="border-l-4 border-fd-green pl-4">
              <h3 className="text-lg font-medium text-fd-text mb-2">{term.term}</h3>
              <p className="text-fd-text-muted leading-relaxed">{term.definition}</p>
              {term.formula && (
                <div className="mt-2 p-3 bg-fd-dark rounded font-mono text-sm text-fd-green">
                  {term.formula}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="border-t border-fd-border p-4 bg-fd-dark/30">
          <button
            onClick={onClose}
            className="bg-fd-green hover:bg-fd-green-hover text-fd-dark font-medium py-2 px-6 rounded transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default MetricsGlossaryModal;
