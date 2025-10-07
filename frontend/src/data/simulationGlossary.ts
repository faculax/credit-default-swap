// Simulation metrics glossary definitions

export interface GlossaryTerm {
  term: string;
  definition: string;
  formula?: string;
}

export const simulationGlossary: GlossaryTerm[] = [
  {
    term: 'EL (Expected Loss)',
    definition: 'Expected portfolio loss up to horizon. The mean loss across all Monte Carlo simulation paths.',
  },
  {
    term: 'VaR (Value at Risk)',
    definition: 'Loss threshold not exceeded with a given probability (confidence level). For example, VaR₉₅ is the loss level that will not be exceeded in 95% of scenarios.',
  },
  {
    term: 'ES (Expected Shortfall)',
    definition: 'Average loss conditional on exceeding VaR. Also known as Conditional VaR or CVaR. ES₉₇.₅ represents the average loss in the worst 2.5% of scenarios.',
  },
  {
    term: 'pAnyDefault',
    definition: 'Probability that at least one reference entity defaults by the horizon date.',
  },
  {
    term: 'Expected Defaults',
    definition: 'Mean count of defaulting names across simulation paths. Represents the average number of entities expected to default.',
  },
  {
    term: 'Diversification Benefit',
    definition: 'Relative EL reduction due to imperfect correlation. Calculated as (Σ standalone EL - portfolio EL) / Σ standalone EL. Higher values indicate better risk diversification.',
  },
  {
    term: 'β (Beta / Loading)',
    definition: 'Sensitivity of a name to the systemic factor. Ranges from 0 (no correlation) to ~0.95 (high correlation). Higher β means the entity is more affected by market-wide credit events.',
  },
  {
    term: 'Marginal EL %',
    definition: 'Percentage contribution of each entity to the total portfolio expected loss. Sum of all marginal contributions equals 100%.',
  },
  {
    term: 'Horizon',
    definition: 'Time period for risk measurement (e.g., 1Y, 3Y, 5Y). Metrics are calculated for each specified horizon.',
  },
  {
    term: 'Monte Carlo Paths',
    definition: 'Number of simulated scenarios. More paths provide more accurate tail risk estimates but take longer to compute.',
  },
  {
    term: 'Seed',
    definition: 'Random number generator seed for reproducibility. Using the same seed with identical inputs produces identical results.',
  },
];
