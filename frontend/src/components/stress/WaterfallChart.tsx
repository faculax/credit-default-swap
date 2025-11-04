import React, { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
  ReferenceLine,
} from 'recharts';
import { ScenarioResult } from '../../services/stressTestService';

interface WaterfallChartProps {
  baseNpv: number;
  scenarios: ScenarioResult[];
  currency: string;
  buySellProtection: string;
  fullscreen?: boolean;
}

interface WaterfallDataPoint {
  name: string;
  fullName?: string;
  value: number;
  start: number;
  end: number;
  delta: number;
  isBase: boolean;
  isFinal: boolean;
  color: string;
}

const WaterfallChart: React.FC<WaterfallChartProps> = ({ baseNpv, scenarios, currency, buySellProtection, fullscreen = false }) => {
  const chartData = useMemo(() => {
    const data: WaterfallDataPoint[] = [];
    
    // For waterfall chart, we show the ACTUAL stressed NPV for each scenario
    // No sign flipping needed - we're showing absolute NPV levels, not P&L impact
    // Each bar represents: baseNpv + deltaNpv = stressed NPV under that scenario
    
    // Determine if this is a protection buyer or seller
    const isBuyer = buySellProtection === 'BUY';
    
    // Sort scenarios by their stressed NPV (lowest to highest for visual flow)
    const sortedScenarios = [...scenarios].sort((a, b) => 
      (baseNpv + a.deltaNpv) - (baseNpv + b.deltaNpv)
    );

    // For many scenarios, intelligently select diverse, impactful scenarios
    const MAX_SCENARIOS_TO_SHOW = 10;
    let scenariosToShow = sortedScenarios;

    if (sortedScenarios.length > MAX_SCENARIOS_TO_SHOW) {
      // INTELLIGENT SCENARIO SELECTION:
      // 1. Prioritize "pure" single-factor scenarios (Spread, Recovery, Yield only)
      // 2. Deduplicate scenarios with identical impacts
      // 3. Add diverse combined scenarios
      // Goal: Show actionable insights, not repetitive variations
      
      const pureScenarios: ScenarioResult[] = [];
      const combinedScenarios: ScenarioResult[] = [];
      
      scenarios.forEach(s => {
        const isSimple = !s.scenarioName.includes('Combined');
        if (isSimple) {
          pureScenarios.push(s);
        } else {
          combinedScenarios.push(s);
        }
      });
      
      // Sort by absolute impact
      pureScenarios.sort((a, b) => Math.abs(b.deltaNpv) - Math.abs(a.deltaNpv));
      combinedScenarios.sort((a, b) => Math.abs(b.deltaNpv) - Math.abs(a.deltaNpv));
      
      // DE-DUPLICATION: Remove scenarios with nearly identical deltaNpv
      // Keep only the first scenario at each impact level (tolerance: 0.1% of baseNpv)
      const deduplicateByImpact = (scenarios: ScenarioResult[]): ScenarioResult[] => {
        const unique: ScenarioResult[] = [];
        const tolerance = Math.abs(baseNpv) * 0.001; // 0.1% tolerance
        
        for (const scenario of scenarios) {
          const isDuplicate = unique.some(existing => 
            Math.abs(existing.deltaNpv - scenario.deltaNpv) < tolerance
          );
          
          if (!isDuplicate) {
            unique.push(scenario);
          }
        }
        
        return unique;
      };
      
      const uniquePure = deduplicateByImpact(pureScenarios);
      const uniqueCombined = deduplicateByImpact(combinedScenarios);
      
      // Take top 7 pure scenarios (or all if less than 7)
      const selectedPure = uniquePure.slice(0, Math.min(7, uniquePure.length));
      
      // Fill remaining slots with combined scenarios
      const remainingSlots = MAX_SCENARIOS_TO_SHOW - selectedPure.length;
      const selectedCombined = uniqueCombined.slice(0, Math.min(remainingSlots, uniqueCombined.length));
      
      // Merge and sort by stressed NPV for waterfall visual flow
      scenariosToShow = [...selectedPure, ...selectedCombined];
      scenariosToShow.sort((a, b) => 
        (baseNpv + a.deltaNpv) - (baseNpv + b.deltaNpv)
      );
    }

    // Base case - add first
    data.push({
      name: 'Base Case',
      fullName: 'Base Case',
      value: baseNpv,
      start: 0,
      end: baseNpv,
      delta: 0,
      isBase: true,
      isFinal: false,
      color: 'rgb(0, 240, 0)', // fd-green
    });

    // Add each scenario - show as individual bars from 0 to stressed NPV
    scenariosToShow.forEach((scenario) => {
      const stressedNpv = baseNpv + scenario.deltaNpv;
      
      // Shorten scenario name for readability on X-axis
      const shortName = scenario.scenarioName
        .replace('Recovery ', 'R')
        .replace('Spread ', 'S')
        .replace('Yield ', 'Y')
        .replace(' + ', '+');
      
      // Each bar shows the stressed NPV value
      data.push({
        name: shortName,
        fullName: scenario.scenarioName,
        value: stressedNpv,
        start: 0,
        end: stressedNpv,
        delta: scenario.deltaNpv,
        isBase: false,
        isFinal: false,
        // Color logic: Red for losses (negative delta), Teal for gains (positive delta)
        // Delta is already correctly signed after backend adjustment for SELL positions
        color: scenario.deltaNpv < 0 ? 'rgb(239, 68, 68)' : 'rgb(0, 255, 195)', // negative = red (bad), positive = teal (good)
      });
    });

    // Final total - show the worst case scenario (lowest stressed NPV)
    const worstScenario = scenariosToShow.reduce((worst, s) => 
      (baseNpv + s.deltaNpv) < (baseNpv + worst.deltaNpv) ? s : worst
    , scenariosToShow[0]);
    
    const worstCaseNpv = baseNpv + worstScenario.deltaNpv;
    
    // Shorten worst case scenario name for display
    const worstCaseShortName = worstScenario.scenarioName
      .replace('Combined: ', '')
      .replace('Recovery ', 'R')
      .replace('Spread ', 'S')
      .replace('Yield ', 'Y')
      .replace(' + ', '+');

    data.push({
      name: `Worst: ${worstCaseShortName}`,
      fullName: `Worst Case: ${worstScenario.scenarioName}`,
      value: worstCaseNpv,
      start: 0,
      end: worstCaseNpv,
      delta: worstScenario.deltaNpv,
      isBase: false,
      isFinal: true,
      color: worstScenario.deltaNpv < 0 ? 'rgb(220, 38, 38)' : 'rgb(0, 232, 247)', // negative = dark red (bad), positive = cyan (good)
    });

    console.log('🔍 WaterfallChart - Scenario Selection:', {
      position: isBuyer ? 'Protection Buyer (BUY)' : 'Protection Seller (SELL)',
      baseNpv: baseNpv.toFixed(2),
      totalScenariosReceived: scenarios.length,
      scenariosDisplayed: scenariosToShow.length,
      selectedScenarios: scenariosToShow.map(s => ({
        name: s.scenarioName,
        deltaNpv: s.deltaNpv.toFixed(2),
        stressedNpv: (baseNpv + s.deltaNpv).toFixed(2)
      })),
      chartBars: data.map(d => ({
        name: d.name,
        value: d.value.toFixed(0),
        delta: d.delta.toFixed(0),
        color: d.color.includes('68') || d.color.includes('38') ? 'RED' : d.color.includes('240') ? 'GREEN' : 'TEAL'
      }))
    });

    return data;
  }, [baseNpv, scenarios, buySellProtection]);

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
      notation: 'compact',
      compactDisplay: 'short',
    }).format(value);
  };

  const formatFullCurrency = (value: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const CustomTooltip = ({ active, payload }: any) => {
    if (!active || !payload || !payload.length) return null;

    const data = payload[0].payload as WaterfallDataPoint;
    const displayName = data.fullName || data.name;

    return (
      <div className="bg-fd-dark border border-fd-border rounded-lg p-3 shadow-lg">
        <p className="text-sm font-semibold text-fd-text mb-2">{displayName}</p>
        
        {data.isBase && (
          <div className="text-xs text-fd-text-muted">
            <p>Starting NPV: <span className="text-fd-green font-semibold">{formatFullCurrency(data.value)}</span></p>
          </div>
        )}
        
        {data.isFinal && (
          <div className="text-xs text-fd-text-muted space-y-1">
            <p>Worst Case NPV: <span className="text-fd-text font-semibold">{formatFullCurrency(data.end)}</span></p>
            <p>Impact from Base: <span className={`font-semibold ${data.delta < 0 ? 'text-red-400' : 'text-teal-400'}`}>
              {formatFullCurrency(data.delta)}
            </span></p>
          </div>
        )}
        
        {!data.isBase && !data.isFinal && (
          <div className="text-xs text-fd-text-muted space-y-1">
            <p>Impact: <span className={`font-semibold ${data.delta < 0 ? 'text-red-400' : 'text-teal-400'}`}>
              {formatFullCurrency(data.delta)}
            </span></p>
            <p>Base NPV: <span className="text-fd-text">{formatFullCurrency(baseNpv)}</span></p>
            <p>Stressed NPV: <span className="text-fd-text">{formatFullCurrency(data.end)}</span></p>
          </div>
        )}
      </div>
    );
  };

  const CustomizedLabel = (props: any) => {
    const { x, y, width, height, index } = props;
    const data = chartData[index];
    
    // Only show labels for significant impacts or base/final
    if (!data.isBase && !data.isFinal && Math.abs(data.delta) < Math.abs(baseNpv * 0.02)) {
      return null;
    }

    const labelY = data.delta < 0 ? y + height + 15 : y - 5;
    
    return (
      <text 
        x={x + width / 2} 
        y={labelY} 
        fill="rgb(156, 163, 175)" 
        textAnchor="middle" 
        fontSize={10}
        fontWeight="500"
      >
        {!data.isBase && !data.isFinal ? formatCurrency(data.delta) : formatCurrency(data.value)}
      </text>
    );
  };

  // Calculate Y-axis domain with some padding
  const allValues = chartData.map(d => d.value);
  const minValue = Math.min(...allValues);
  const maxValue = Math.max(...allValues);
  const range = maxValue - minValue;
  const padding = range > 0 ? range * 0.1 : maxValue * 0.1;

  const chartHeight = fullscreen ? 700 : 400;

  if (fullscreen) {
    // Fullscreen mode: no wrapper, just the chart content
    return (
      <div className="w-full h-full">
        <ResponsiveContainer width="100%" height={chartHeight}>
          <BarChart
            data={chartData}
            margin={{ top: 30, right: 30, left: 20, bottom: 80 }}
          >
            <CartesianGrid strokeDasharray="3 3" stroke="rgb(60, 75, 97)" opacity={0.3} />
            <XAxis
              dataKey="name"
              angle={-45}
              textAnchor="end"
              height={120}
              interval={0}
              tick={{ fill: 'rgb(156, 163, 175)', fontSize: 10 }}
              stroke="rgb(60, 75, 97)"
            />
            <YAxis
              domain={[minValue - padding, maxValue + padding]}
              tickFormatter={formatCurrency}
              tick={{ fill: 'rgb(156, 163, 175)', fontSize: 11 }}
              stroke="rgb(60, 75, 97)"
            />
            <Tooltip content={<CustomTooltip />} />
            
            {/* Simple bars showing stressed NPV for each scenario */}
            <Bar 
              dataKey="value" 
              label={<CustomizedLabel />}
              radius={[4, 4, 0, 0]}
              isAnimationActive={false}
            >
              {chartData.map((entry, index) => (
                <Cell 
                  key={`cell-${index}`} 
                  fill={entry.color} 
                  stroke={entry.color}
                  opacity={entry.isBase || entry.isFinal ? 1 : 0.85} 
                />
              ))}
            </Bar>
            
            {/* Base NPV reference line - rendered AFTER bars so it appears on top */}
            <ReferenceLine 
              y={baseNpv} 
              stroke="rgb(0, 240, 0)" 
              strokeWidth={2}
              strokeDasharray="5 5" 
              label={{ 
                value: 'Base NPV', 
                fill: 'rgb(0, 240, 0)', 
                fontSize: 11,
                fontWeight: 600,
                position: 'insideTopRight'
              }} 
            />
            
            {/* Zero reference line */}
            <ReferenceLine 
              y={0} 
              stroke="rgb(156, 163, 175)" 
              strokeWidth={1}
              strokeDasharray="3 3" 
              label={{ 
                value: '0', 
                fill: 'rgb(156, 163, 175)', 
                fontSize: 10,
                position: 'insideTopLeft'
              }} 
            />
          </BarChart>
        </ResponsiveContainer>
        
        {/* Legend */}
        <div className="mt-6 flex flex-wrap gap-4 justify-center text-xs">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 240, 0)' }}></div>
            <span className="text-fd-text-muted">Base Case</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded bg-red-500"></div>
            <span className="text-fd-text-muted">Negative Impact</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 255, 195)' }}></div>
            <span className="text-fd-text-muted">Positive Impact</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 232, 247)' }}></div>
            <span className="text-fd-text-muted">Worst Case</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <ResponsiveContainer width="100%" height={chartHeight}>
        <BarChart
          data={chartData}
          margin={{ top: 30, right: 30, left: 20, bottom: 80 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="rgb(60, 75, 97)" opacity={0.3} />
          <XAxis
            dataKey="name"
            angle={-45}
            textAnchor="end"
            height={120}
            interval={0}
            tick={{ fill: 'rgb(156, 163, 175)', fontSize: 10 }}
            stroke="rgb(60, 75, 97)"
          />
          <YAxis
            domain={[minValue - padding, maxValue + padding]}
            tickFormatter={formatCurrency}
            tick={{ fill: 'rgb(156, 163, 175)', fontSize: 11 }}
            stroke="rgb(60, 75, 97)"
          />
          <Tooltip content={<CustomTooltip />} />
          
          {/* Simple bars showing stressed NPV for each scenario */}
          <Bar 
            dataKey="value" 
            label={<CustomizedLabel />}
            radius={[4, 4, 0, 0]}
            isAnimationActive={false}
          >
            {chartData.map((entry, index) => (
              <Cell 
                key={`cell-${index}`} 
                fill={entry.color} 
                stroke={entry.color}
                opacity={entry.isBase || entry.isFinal ? 1 : 0.85} 
              />
            ))}
          </Bar>
          
          {/* Base NPV reference line - rendered AFTER bars so it appears on top */}
          <ReferenceLine 
            y={baseNpv} 
            stroke="rgb(0, 240, 0)" 
            strokeWidth={2}
            strokeDasharray="5 5" 
            label={{ 
              value: 'Base NPV', 
              fill: 'rgb(0, 240, 0)', 
              fontSize: 11,
              fontWeight: 600,
              position: 'insideTopRight'
            }} 
          />
          
          {/* Zero reference line */}
          <ReferenceLine 
            y={0} 
            stroke="rgb(156, 163, 175)" 
            strokeWidth={1}
            strokeDasharray="3 3" 
            label={{ 
              value: '0', 
              fill: 'rgb(156, 163, 175)', 
              fontSize: 10,
              position: 'insideTopLeft'
            }} 
          />
        </BarChart>
      </ResponsiveContainer>
      
      {/* Legend */}
      <div className="mt-6 flex flex-wrap gap-4 justify-center text-xs">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 240, 0)' }}></div>
          <span className="text-fd-text-muted">Base Case</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded bg-red-500"></div>
          <span className="text-fd-text-muted">Negative Impact</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 255, 195)' }}></div>
          <span className="text-fd-text-muted">Positive Impact</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded" style={{ backgroundColor: 'rgb(0, 232, 247)' }}></div>
          <span className="text-fd-text-muted">Worst Case</span>
        </div>
      </div>
    </div>
  );
};

export default WaterfallChart;
