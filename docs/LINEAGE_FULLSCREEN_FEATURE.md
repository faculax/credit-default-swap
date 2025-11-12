# Lineage Fullscreen Feature

## Overview
Added fullscreen capability to all three main sections of the Lineage page for enhanced data exploration and better visibility of complex lineage graphs and intelligence data.

## Implementation Details

### State Management
```typescript
const [fullscreenMode, setFullscreenMode] = useState<'none' | 'graph' | 'intelligence' | 'events'>('none');
```

### Features

#### 1. **Flow Diagram Fullscreen** (`fullscreenMode === 'graph'`)
- **Trigger**: Click expand button (⛶) in Flow Diagram tab header
- **Display**: Full-screen lineage graph with node/edge statistics
- **Exit**: Click × button or press ESC key

#### 2. **Lineage Intelligence Fullscreen** (`fullscreenMode === 'intelligence'`)
- **Trigger**: Click expand button (⛶) in Lineage Intelligence tab header
- **Display**: Full-screen intelligence view with all sub-tabs:
  - 🛤️ Path Detail
  - 📍 Origin
  - 🔄 Transformations (with table details, purpose, event context)
  - 📊 Consumers
  - 📋 Metadata
- **Features**: 
  - View Default Event button (if specific event selected)
  - All transformation details with business context
- **Exit**: Click × button or press ESC key

#### 3. **Event History Fullscreen** (`fullscreenMode === 'events'`)
- **Trigger**: Click expand button (⛶) in Event History sidebar header
- **Display**: Full-screen event table with:
  - Timestamp (full locale format)
  - Dataset (as code block)
  - Operation (badge style)
  - Run ID
- **Features**:
  - Full filter controls (search, operation dropdown, dataset dropdown)
  - Clear filters button
  - Click event to load lineage and exit fullscreen
- **Exit**: Click × button or press ESC key

### User Experience

#### Keyboard Shortcuts
- **ESC**: Exit any fullscreen mode and return to normal view

#### Click Behavior
- **Graph Fullscreen**: Clicking nodes opens node details modal (separate z-index layer)
- **Intelligence Fullscreen**: Can switch between sub-tabs while in fullscreen
- **Events Fullscreen**: Clicking an event loads its lineage and exits fullscreen automatically

### Technical Architecture

#### Fullscreen Overlay Structure
```tsx
<div className="fixed inset-0 bg-fd-darker z-50 flex flex-col">
  {/* Header with title, stats, and close button */}
  <div className="bg-fd-dark px-6 py-4 border-b border-fd-border">
    {/* ... */}
  </div>
  
  {/* Content area (scrollable if needed) */}
  <div className="flex-1 overflow-y-auto">
    {/* Cloned content from regular view */}
  </div>
</div>
```

#### Z-Index Layers
- **Normal view**: z-auto (default)
- **Fullscreen mode**: z-50
- **Node details modal**: z-50 (can overlay on graph fullscreen)

### Visual Indicators

#### Expand Buttons
- **Icon**: ⛶ (fullscreen symbol)
- **Location**: Top-right of each section header
- **Styling**: 
  - Default: Gray with border
  - Hover: Green text with transition
  - Small compact design to avoid clutter

#### Close Buttons
- **Icon**: × (large close symbol)
- **Location**: Top-right of fullscreen overlay
- **Styling**: 
  - Default: Muted text
  - Hover: Green with transition
  - Large (text-3xl) for easy clicking

### Code Locations

#### State & Hooks
- Line 25: `fullscreenMode` state declaration
- Lines 43-49: ESC key event listener

#### Expand Buttons
- Line 458: Graph tab fullscreen button
- Line 509: Intelligence tab fullscreen button  
- Line 307: Event history sidebar fullscreen button

#### Fullscreen Modals
- Lines 791-838: Graph fullscreen modal
- Lines 840-1044: Intelligence fullscreen modal
- Lines 1046-1126: Events fullscreen modal

### Color Scheme
All fullscreen views use consistent FD-themed colors:
- **Background**: `bg-fd-darker` (main overlay)
- **Headers**: `bg-fd-dark` with `border-fd-border`
- **Text**: `text-fd-text` (primary), `text-fd-text-muted` (secondary)
- **Accents**: `text-fd-green`, `text-fd-cyan`
- **Hover**: `hover:text-fd-green`, `hover:bg-fd-darker`

### Testing Checklist

- [x] Graph fullscreen opens and displays correctly
- [x] Intelligence fullscreen shows all sub-tabs
- [x] Events fullscreen displays full table with filters
- [x] ESC key exits all fullscreen modes
- [x] Close buttons work in all modals
- [x] Clicking events in fullscreen loads lineage
- [x] Node click in graph fullscreen opens details
- [x] Intelligence sub-tabs switch correctly in fullscreen
- [x] Filters work in events fullscreen
- [x] No layout shift when entering/exiting fullscreen
- [x] Consistent styling across all fullscreen views

## Benefits

1. **Enhanced Visibility**: Large graphs and complex transformations easier to analyze
2. **Better Focus**: Fullscreen eliminates distractions from other UI elements
3. **Improved Workflow**: 
   - Explore graph in fullscreen
   - Check intelligence for specific transformations
   - Review full event history without scrolling constraints
4. **Keyboard Navigation**: ESC key provides quick exit for power users
5. **Consistent UX**: All three sections have identical fullscreen interaction patterns

## Future Enhancements

- Add fullscreen zoom controls for graph
- Enable screenshot/export from fullscreen mode
- Add fullscreen indicator/badge
- Support multiple monitors (open fullscreen in separate window)
- Add pan/zoom lock in graph fullscreen
- Enable fullscreen state persistence in URL params
