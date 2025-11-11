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
  base: number; // Invisible bar to position the visible bar
  value: number; // The visible bar (delta)
  end: number;
  delta: number;
  isBase: boolean;
  isFinal: boolean;
  color: string;
}

const WaterfallChart: React.FC<WaterfallChartProps> = ({ baseNpv, scenarios, currency, buySellProtection, fullscreen = false }) => {
  const chartData = useMemo(() => {
    const data: WaterfallDataPoint[] = [];
    
    // For waterfall chart showing impact from base NPV:
    // - Base case: show as marker at base NPV level (zero height bar)
    // - Scenarios: show delta from base NPV (positive = up, negative = down from base)
    // - This makes negative impacts VISIBLE as large downward bars
    
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

    // Base case - show as a full bar from 0 to base NPV
    data.push({
      name: 'Base Case',
      fullName: 'Base Case',
      base: 0, // Start from zero
      value: baseNpv, // Full height to base NPV
      end: baseNpv,
      delta: 0,
      isBase: true,
      isFinal: false,
      color: 'rgb(0, 240, 0)', // fd-green
    });

    // Add each scenario - TRUE WATERFALL from base NPV
    scenariosToShow.forEach((scenario) => {
      const stressedNpv = baseNpv + scenario.deltaNpv;
      
      // Clean up scenario name (fix "+-" issue)
      const cleanName = scenario.scenarioName.replace('+-', '-');
      
      // Shorten scenario name for readability on X-axis
      const shortName = cleanName
        .replace('Recovery ', 'R')
        .replace('Spread ', 'S')
        .replace('Yield ', 'Y')
        .replace(' + ', '+');
      
      // For waterfall effect with stacked bars:
      // - base: invisible bar from 0 to baseNpv (or from 0 to stressedNpv if negative delta)
      // - value: visible bar showing the delta (height = abs(delta))
      const isNegative = scenario.deltaNpv < 0;
      
      data.push({
        name: shortName,
        fullName: cleanName,
        base: isNegative ? stressedNpv : baseNpv, // Start position
        value: Math.abs(scenario.deltaNpv), // Height of the bar
        end: stressedNpv,
        delta: scenario.deltaNpv,
        isBase: false,
        isFinal: false,
        // Color logic: Red for losses (negative delta), Teal for gains (positive delta)
        color: scenario.deltaNpv < 0 ? 'rgb(239, 68, 68)' : 'rgb(0, 255, 195)',
      });
    });

    // Final total - show the worst case scenario
    const worstScenario = scenariosToShow.reduce((worst, s) => 
      (baseNpv + s.deltaNpv) < (baseNpv + worst.deltaNpv) ? s : worst
    , scenariosToShow[0]);
    
    const worstCaseNpv = baseNpv + worstScenario.deltaNpv;
    
    // Clean up worst case scenario name (fix "+-" issue)
    const worstCaseCleanName = worstScenario.scenarioName.replace('+-', '-');
    
    // Shorten worst case scenario name for display
    const worstCaseShortName = worstCaseCleanName
      .replace('Combined: ', '')
      .replace('Recovery ', 'R')
      .replace('Spread ', 'S')
      .replace('Yield ', 'Y')
      .replace(' + ', '+');

    const isWorstNegative = worstScenario.deltaNpv < 0;

    data.push({
      name: `Worst: ${worstCaseShortName}`,
      fullName: `Worst Case: ${worstCaseCleanName}`,
      base: isWorstNegative ? worstCaseNpv : baseNpv, // Start position
      value: Math.abs(worstScenario.deltaNpv), // Height of the bar
      end: worstCaseNpv,
      delta: worstScenario.deltaNpv,
      isBase: false,
      isFinal: true,
      color: worstScenario.deltaNpv < 0 ? 'rgb(220, 38, 38)' : 'rgb(0, 232, 247)',
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
    
    // For base case, show the NPV value at the top
    if (data.isBase) {
      return (
        <text 
          x={x + width / 2} 
          y={y - 5} 
          fill="rgb(0, 240, 0)" 
          textAnchor="middle" 
          fontSize={11}
          fontWeight="600"
        >
          {formatCurrency(baseNpv)}
        </text>
      );
    }
    
    // Only show labels for significant impacts or final
    if (!data.isFinal && Math.abs(data.delta) < Math.abs(baseNpv * 0.02)) {
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
        {formatCurrency(data.delta)}
      </text>
    );
  };

  // Calculate Y-axis domain with some padding
  // For stacked bars, we need to consider base + value
  const allValues = chartData.flatMap(d => [d.base, d.base + d.value, d.end]);
  const minVal = Math.min(...allValues);
  const maxVal = Math.max(...allValues);
  const range = maxVal - minVal;
  const padding = range > 0 ? range * 0.1 : Math.abs(maxVal) * 0.1;

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
              domain={[minVal - padding, maxVal + padding]}
              tickFormatter={formatCurrency}
              tick={{ fill: 'rgb(156, 163, 175)', fontSize: 11 }}
              stroke="rgb(60, 75, 97)"
            />
            <Tooltip content={<CustomTooltip />} />
            
            {/* Waterfall bars using stacked approach */}
            {/* Invisible base bar to position the visible bar */}
            <Bar 
              dataKey="base" 
              stackId="waterfall"
              fill="transparent"
              isAnimationActive={false}
            />
            {/* Visible bar showing the delta */}
            <Bar 
              dataKey="value" 
              stackId="waterfall"
              label={<CustomizedLabel />}
              radius={[4, 4, 4, 4]}
              isAnimationActive={false}
            >
              {chartData.map((entry, index) => (
                <Cell 
                  key={`cell-${index}`} 
                  fill={entry.color} 
                  stroke={entry.color}
                  opacity={entry.isBase ? 1 : (entry.isFinal ? 1 : 0.85)}
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
            domain={[minVal - padding, maxVal + padding]}
            tickFormatter={formatCurrency}
            tick={{ fill: 'rgb(156, 163, 175)', fontSize: 11 }}
            stroke="rgb(60, 75, 97)"
          />
          <Tooltip content={<CustomTooltip />} />
          
          {/* Waterfall bars using stacked approach */}
          {/* Invisible base bar to position the visible bar */}
          <Bar 
            dataKey="base" 
            stackId="waterfall"
            fill="transparent"
            isAnimationActive={false}
          />
          {/* Visible bar showing the delta */}
          <Bar 
            dataKey="value" 
            stackId="waterfall"
            label={<CustomizedLabel />}
            radius={[4, 4, 4, 4]}
            isAnimationActive={false}
          >
            {chartData.map((entry, index) => (
              <Cell 
                key={`cell-${index}`} 
                fill={entry.color} 
                stroke={entry.color}
                opacity={entry.isBase ? 1 : (entry.isFinal ? 1 : 0.85)}
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
