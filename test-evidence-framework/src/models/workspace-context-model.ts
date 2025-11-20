/**
 * Workspace Context Model
 * 
 * Defines the structure for capturing actual workspace metadata:
 * - Backend Java classes (Services, Repositories, Controllers, Entities)
 * - Frontend React components and hooks
 * - API endpoints and routes
 * - Database entities and schemas
 */

export interface BackendClass {
  /** Fully qualified class name (e.g., CDSTradeService) */
  className: string;
  
  /** Full package name (e.g., com.creditdefaultswap.platform.service) */
  packageName: string;
  
  /** Fully qualified name (package + class) */
  fullyQualifiedName: string;
  
  /** Type of class */
  type: 'Service' | 'Repository' | 'Controller' | 'Entity' | 'Config' | 'DTO' | 'Util';
  
  /** Absolute file path */
  filePath: string;
  
  /** Detected methods (signatures only) */
  methods?: string[];
  
  /** Detected fields */
  fields?: string[];
  
  /** Spring annotations */
  annotations?: string[];
}

export interface FrontendComponent {
  /** Component name (e.g., TradeForm) */
  componentName: string;
  
  /** Absolute file path */
  filePath: string;
  
  /** Relative path from frontend/src */
  relativePath: string;
  
  /** File extension (.tsx or .jsx) */
  extension: '.tsx' | '.jsx';
  
  /** Exported items (components, hooks, types) */
  exports: string[];
  
  /** Detected props (if parseable) */
  props?: string[];
  
  /** Detected hooks used */
  hooks?: string[];
  
  /** Is it a page/route component? */
  isPage?: boolean;
}

export interface APIEndpoint {
  /** HTTP method */
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  
  /** Path pattern (e.g., /api/trades/{id}) */
  path: string;
  
  /** Controller class that handles it */
  controllerClass?: string;
  
  /** Method name in controller */
  methodName?: string;
  
  /** Request DTO type */
  requestType?: string;
  
  /** Response DTO type */
  responseType?: string;
}

export interface DatabaseEntity {
  /** Entity class name */
  entityName: string;
  
  /** Table name */
  tableName?: string;
  
  /** Package name */
  packageName: string;
  
  /** Detected fields with full metadata */
  fields?: EntityFieldMetadata[];
}

/**
 * Detailed field metadata for entities (for realistic test data generation)
 */
export interface EntityFieldMetadata {
  /** Field name */
  name: string;
  
  /** Java type (e.g., String, BigDecimal, LocalDate) */
  javaType: string;
  
  /** JSON type for test payloads (string, number, boolean, date, enum) */
  jsonType: string;
  
  /** Can field be null? */
  nullable: boolean;
  
  /** Maximum string length (from @Column) */
  maxLength?: number;
  
  /** Numeric precision (from @Column) */
  precision?: number;
  
  /** Numeric scale (from @Column) */
  scale?: number;
  
  /** Enum values if field is enum type */
  enumValues?: string[];
}

/**
 * Complete workspace context
 */
export interface WorkspaceContext {
  /** Absolute path to workspace root */
  workspaceRoot: string;
  
  /** Absolute path to backend module */
  backendRoot: string;
  
  /** Absolute path to frontend module */
  frontendRoot: string;
  
  /** All backend classes discovered */
  backendClasses: BackendClass[];
  
  /** All frontend components discovered */
  frontendComponents: FrontendComponent[];
  
  /** API endpoints extracted from controllers */
  apiEndpoints: APIEndpoint[];
  
  /** Database entities */
  entities: DatabaseEntity[];
  
  /** Services grouped by name */
  servicesByName: Map<string, BackendClass[]>;
  
  /** Repositories grouped by name */
  repositoriesByName: Map<string, BackendClass[]>;
  
  /** Controllers grouped by name */
  controllersByName: Map<string, BackendClass[]>;
  
  /** Components grouped by feature/domain */
  componentsByDomain: Map<string, FrontendComponent[]>;
  
  /** Fully qualified name of Spring Boot application class */
  springBootApplicationClass?: string;
  
  /** Timestamp of scan */
  scannedAt: Date;
}

/**
 * Scan configuration
 */
export interface WorkspaceScanConfig {
  /** Workspace root path */
  workspaceRoot: string;
  
  /** Backend module path (relative to workspace root, default: 'backend') */
  backendPath?: string;
  
  /** Frontend module path (relative to workspace root, default: 'frontend') */
  frontendPath?: string;
  
  /** Whether to scan backend */
  scanBackend?: boolean;
  
  /** Whether to scan frontend */
  scanFrontend?: boolean;
  
  /** Whether to extract method signatures */
  extractMethods?: boolean;
  
  /** Whether to extract API endpoints */
  extractEndpoints?: boolean;
  
  /** File patterns to exclude */
  excludePatterns?: string[];
}

/**
 * Scan result metadata
 */
export interface WorkspaceScanResult {
  success: boolean;
  context?: WorkspaceContext;
  errors: string[];
  warnings: string[];
  scanDuration: number; // milliseconds
  stats: {
    backendClassesFound: number;
    frontendComponentsFound: number;
    endpointsFound: number;
    entitiesFound: number;
  };
}
