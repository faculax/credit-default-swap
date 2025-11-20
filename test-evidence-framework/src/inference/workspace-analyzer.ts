/**
 * Workspace Analyzer
 * 
 * Scans the actual workspace structure to discover:
 * - Backend Java classes (Services, Repositories, Controllers, Entities)
 * - Frontend React components (.tsx/.jsx files)
 * - API endpoints from Spring controllers
 * - Database entities
 * 
 * This provides REAL workspace context to generators, replacing invented
 * class names with actual ones from the codebase.
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import {
  WorkspaceContext,
  WorkspaceScanConfig,
  WorkspaceScanResult,
  BackendClass,
  FrontendComponent,
  APIEndpoint,
  DatabaseEntity,
  EntityFieldMetadata
} from '../models/workspace-context-model.js';

export class WorkspaceAnalyzer {
  private config: WorkspaceScanConfig;
  private enumRegistry: Map<string, string[]> = new Map();
  
  constructor(config: WorkspaceScanConfig) {
    this.config = {
      backendPath: 'backend',
      frontendPath: 'frontend',
      scanBackend: true,
      scanFrontend: true,
      extractMethods: true,
      extractEndpoints: true,
      excludePatterns: ['**/target/**', '**/node_modules/**', '**/build/**', '**/dist/**'],
      ...config
    };
  }
  
  /**
   * Scan the entire workspace
   */
  public async scan(): Promise<WorkspaceScanResult> {
    const startTime = Date.now();
    const errors: string[] = [];
    const warnings: string[] = [];
    
    try {
      const backendRoot = path.join(this.config.workspaceRoot, this.config.backendPath || 'backend');
      const frontendRoot = path.join(this.config.workspaceRoot, this.config.frontendPath || 'frontend');
      
      // Scan backend
      let backendClasses: BackendClass[] = [];
      let apiEndpoints: APIEndpoint[] = [];
      let entities: DatabaseEntity[] = [];
      
      if (this.config.scanBackend) {
        if (fs.existsSync(backendRoot)) {
          backendClasses = await this.scanBackendClasses(backendRoot);
          
          if (this.config.extractEndpoints) {
            apiEndpoints = this.extractAPIEndpoints(backendClasses);
          }
          
          entities = backendClasses
            .filter(c => c.type === 'Entity')
            .map(c => this.convertToEntity(c));
        } else {
          warnings.push(`Backend root not found: ${backendRoot}`);
        }
      }
      
      // Scan frontend
      let frontendComponents: FrontendComponent[] = [];
      
      if (this.config.scanFrontend) {
        if (fs.existsSync(frontendRoot)) {
          frontendComponents = await this.scanFrontendComponents(frontendRoot);
        } else {
          warnings.push(`Frontend root not found: ${frontendRoot}`);
        }
      }
      
      // Build context
      const springBootApplicationClass = this.findSpringBootApplicationClass(backendClasses);
      
      const context: WorkspaceContext = {
        workspaceRoot: this.config.workspaceRoot,
        backendRoot,
        frontendRoot,
        backendClasses,
        frontendComponents,
        apiEndpoints,
        entities,
        servicesByName: this.groupByName(backendClasses, 'Service'),
        repositoriesByName: this.groupByName(backendClasses, 'Repository'),
        controllersByName: this.groupByName(backendClasses, 'Controller'),
        componentsByDomain: this.groupComponentsByDomain(frontendComponents),
        springBootApplicationClass,
        scannedAt: new Date()
      };
      
      const scanDuration = Date.now() - startTime;
      
      return {
        success: true,
        context,
        errors,
        warnings,
        scanDuration,
        stats: {
          backendClassesFound: backendClasses.length,
          frontendComponentsFound: frontendComponents.length,
          endpointsFound: apiEndpoints.length,
          entitiesFound: entities.length
        }
      };
      
    } catch (error) {
      errors.push(error instanceof Error ? error.message : String(error));
      
      return {
        success: false,
        errors,
        warnings,
        scanDuration: Date.now() - startTime,
        stats: {
          backendClassesFound: 0,
          frontendComponentsFound: 0,
          endpointsFound: 0,
          entitiesFound: 0
        }
      };
    }
  }
  
  /**
   * Scan backend Java source files
   */
  private async scanBackendClasses(backendRoot: string): Promise<BackendClass[]> {
    const classes: BackendClass[] = [];
    const srcMain = path.join(backendRoot, 'src', 'main', 'java');
    
    if (!fs.existsSync(srcMain)) {
      return classes;
    }
    
    const scanDir = (dir: string): void => {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        
        if (entry.isDirectory()) {
          // Skip excluded patterns
          if (this.shouldExclude(fullPath)) {
            continue;
          }
          scanDir(fullPath);
        } else if (entry.isFile() && entry.name.endsWith('.java')) {
          try {
            // Check if this is an enum file and populate registry
            this.checkAndRegisterEnum(fullPath);
            
            const classInfo = this.parseJavaClass(fullPath, srcMain);
            if (classInfo) {
              classes.push(classInfo);
            }
          } catch (error) {
            // Skip files that can't be parsed
            console.warn(`Failed to parse ${fullPath}:`, error);
          }
        }
      }
    };
    
    scanDir(srcMain);
    return classes;
  }
  
  /**
   * Check if file is an enum and register its values
   */
  private checkAndRegisterEnum(filePath: string): void {
    try {
      const content = fs.readFileSync(filePath, 'utf-8');
      
      // Check if this is an enum definition
      const enumMatch = /(?:public\s+)?enum\s+(\w+)\s*\{([^}]+)\}/s.exec(content);
      if (enumMatch) {
        const enumName = enumMatch[1];
        const enumBody = enumMatch[2];
        
        // Extract enum values (constants before any methods/fields)
        const values = enumBody
          .split(/[,;]/)
          .map(v => v.trim().split(/[(\s]/)[0])  // Get first word before ( or space
          .filter(v => v && v.match(/^[A-Z_][A-Z0-9_]*$/));  // Valid enum constant names
        
        if (values.length > 0) {
          this.enumRegistry.set(enumName, values);
        }
      }
    } catch (error) {
      // Ignore parse errors
    }
  }
  
  /**
   * Parse a Java class file
   */
  private parseJavaClass(filePath: string, srcRoot: string): BackendClass | null {
    const content = fs.readFileSync(filePath, 'utf-8');
    
    // Extract package name
    const packageMatch = content.match(/package\s+([\w.]+);/);
    if (!packageMatch) {
      return null;
    }
    const packageName = packageMatch[1];
    
    // Extract class name
    const classMatch = content.match(/(?:public\s+)?(?:abstract\s+)?(?:class|interface|enum)\s+(\w+)/);
    if (!classMatch) {
      return null;
    }
    const className = classMatch[1];
    
    // Determine type from annotations and naming
    let type: BackendClass['type'] = 'Util';
    const annotations: string[] = [];
    
    // Extract Spring/JPA annotations
    const annotationMatches = content.matchAll(/@(\w+)(?:\(.*?\))?/g);
    for (const match of annotationMatches) {
      annotations.push(match[1]);
    }
    
    if (annotations.includes('Service') || className.endsWith('Service')) {
      type = 'Service';
    } else if (annotations.includes('Repository') || className.endsWith('Repository')) {
      type = 'Repository';
    } else if (annotations.includes('RestController') || annotations.includes('Controller') || className.endsWith('Controller')) {
      type = 'Controller';
    } else if (annotations.includes('Entity') || className.endsWith('Entity')) {
      type = 'Entity';
    } else if (annotations.includes('Configuration') || className.endsWith('Config')) {
      type = 'Config';
    } else if (className.endsWith('DTO') || className.endsWith('Request') || className.endsWith('Response')) {
      type = 'DTO';
    }
    
    // Extract methods if requested
    let methods: string[] | undefined;
    if (this.config.extractMethods) {
      methods = this.extractMethods(content);
    }
    
    // Extract fields
    const fields = this.extractFields(content);
    
    return {
      className,
      packageName,
      fullyQualifiedName: `${packageName}.${className}`,
      type,
      filePath,
      methods,
      fields,
      annotations
    };
  }
  
  /**
   * Extract method signatures from Java class
   */
  private extractMethods(content: string): string[] {
    const methods: string[] = [];
    
    // Match method signatures (simplified - doesn't handle all cases)
    const methodPattern = /(?:public|protected|private)\s+(?:static\s+)?(?:<[\w,\s]+>\s+)?[\w<>[\]]+\s+(\w+)\s*\([^)]*\)/g;
    let match;
    
    while ((match = methodPattern.exec(content)) !== null) {
      methods.push(match[1]);
    }
    
    return methods;
  }
  
  /**
   * Extract field names from Java class
   */
  private extractFields(content: string): string[] {
    const fields: string[] = [];
    
    // Match field declarations (simplified)
    const fieldPattern = /(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?[\w<>[\]]+\s+(\w+)\s*[;=]/g;
    let match;
    
    while ((match = fieldPattern.exec(content)) !== null) {
      fields.push(match[1]);
    }
    
    return fields;
  }
  
  /**
   * Extract detailed field metadata from JPA entity
   */
  private extractEntityFieldMetadata(content: string): EntityFieldMetadata[] {
    const fields: EntityFieldMetadata[] = [];
    
    // Match field declarations with annotations
    // Pattern: match @Column and field declaration together
    const lines = content.split('\n');
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      
      // Skip non-field lines
      if (!line.match(/^\s*(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?[\w<>[\]]+\s+\w+\s*[;=]/)) {
        continue;
      }
      
      // Extract field declaration
      const fieldMatch = line.match(/(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?([\w<>[\]]+)\s+(\w+)\s*[;=]/);
      if (!fieldMatch) continue;
      
      const javaType = fieldMatch[1];
      const fieldName = fieldMatch[2];
      
      // Skip static/transient fields
      if (line.includes('static') || line.includes('transient')) {
        continue;
      }
      
      // Look backwards for annotations (up to 5 lines)
      // We need to track if we hit an empty line to avoid cross-field contamination
      let columnAnnotation: string | null = null;
      let enumValues: string[] | undefined;
      let isId = false;
      let isComplexObject = false;
      
      for (let j = i - 1; j >= Math.max(0, i - 5); j--) {
        const prevLine = lines[j].trim();
        
        // Stop if we hit an empty line or another field declaration
        // This prevents annotations from previous fields from being associated with this field
        if (prevLine === '' || prevLine.match(/^\s*(?:private|protected|public)\s+/)) {
          break;
        }
        
        if (prevLine.includes('@Column')) {
          columnAnnotation = prevLine;
        }
        if (prevLine.includes('@Id')) {
          isId = true;
        }
        if (prevLine.includes('@Enumerated')) {
          // Look up enum values from registry
          enumValues = this.enumRegistry.get(javaType);
        }
        // Detect complex objects (@OneToOne, @OneToMany, @ManyToOne, @ManyToMany)
        if (prevLine.includes('@OneToOne') || prevLine.includes('@OneToMany') || 
            prevLine.includes('@ManyToOne') || prevLine.includes('@ManyToMany')) {
          isComplexObject = true;
        }
      }
      
      // If no @Enumerated annotation found, also check enum registry by type name
      if (!enumValues && !isComplexObject && javaType.match(/^[A-Z]/)) {
        enumValues = this.enumRegistry.get(javaType);
      }
      
      // Parse @Column constraints
      let nullable = true;
      let maxLength: number | undefined;
      let precision: number | undefined;
      let scale: number | undefined;
      
      if (columnAnnotation) {
        const nullableMatch = columnAnnotation.match(/nullable\s*=\s*(true|false)/);
        if (nullableMatch) {
          nullable = nullableMatch[1] === 'true';
        }
        
        const lengthMatch = columnAnnotation.match(/length\s*=\s*(\d+)/);
        if (lengthMatch) {
          maxLength = parseInt(lengthMatch[1]);
        }
        
        const precisionMatch = columnAnnotation.match(/precision\s*=\s*(\d+)/);
        if (precisionMatch) {
          precision = parseInt(precisionMatch[1]);
        }
        
        const scaleMatch = columnAnnotation.match(/scale\s*=\s*(\d+)/);
        if (scaleMatch) {
          scale = parseInt(scaleMatch[1]);
        }
      }
      
      // Skip ID fields in test data generation
      if (isId) {
        continue;
      }
      
      // Map Java type to JSON type
      const jsonType = this.mapJavaTypeToJsonType(javaType, enumValues, isComplexObject);
      
      fields.push({
        name: fieldName,
        javaType,
        jsonType,
        nullable,
        maxLength,
        precision,
        scale,
        enumValues
      });
    }
    
    return fields;
  }
  
  /**
   * Map Java type to JSON type for test data generation
   */
  /**
   * Find the main Spring Boot application class
   */
  private findSpringBootApplicationClass(classes: BackendClass[]): string | undefined {
    for (const cls of classes) {
      if (cls.annotations?.includes('SpringBootApplication')) {
        return cls.fullyQualifiedName;
      }
    }
    return undefined;
  }
  
  private mapJavaTypeToJsonType(javaType: string, enumValues?: string[], isComplexObject?: boolean): string {
    if (javaType === 'String') return 'string';
    if (javaType === 'BigDecimal') return 'number';
    if (javaType === 'Integer' || javaType === 'Long' || javaType === 'int' || javaType === 'long') return 'number';
    if (javaType === 'Boolean' || javaType === 'boolean') return 'boolean';
    if (javaType === 'LocalDate' || javaType === 'Date') return 'date';
    if (javaType === 'LocalDateTime' || javaType === 'ZonedDateTime' || javaType === 'Instant') return 'datetime';
    
    // Complex object (relationship) - set to null
    if (isComplexObject) return 'object';
    
    // Enum with values
    if (enumValues && enumValues.length > 0) return 'enum';
    
    // Uppercase type without enum values - likely a complex object
    if (javaType.match(/^[A-Z]/)) return 'object';
    
    return 'string';
  }
  
  /**
   * Extract API endpoints from controller classes
   */
  private extractAPIEndpoints(classes: BackendClass[]): APIEndpoint[] {
    const endpoints: APIEndpoint[] = [];
    
    const controllers = classes.filter(c => c.type === 'Controller');
    
    for (const controller of controllers) {
      try {
        const content = fs.readFileSync(controller.filePath, 'utf-8');
        
        // Extract @RequestMapping from class level
        const classPathMatch = content.match(/@RequestMapping\s*\(\s*["']([^"']+)["']/);
        const basePath = classPathMatch ? classPathMatch[1] : '';
        
        // Extract method mappings
        const mappingPattern = /@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/g;
        let match;
        
        while ((match = mappingPattern.exec(content)) !== null) {
          const annotation = match[1];
          const methodPath = match[2];
          const fullPath = basePath + methodPath;
          
          let method: APIEndpoint['method'] = 'GET';
          if (annotation.startsWith('Post')) method = 'POST';
          else if (annotation.startsWith('Put')) method = 'PUT';
          else if (annotation.startsWith('Delete')) method = 'DELETE';
          else if (annotation.startsWith('Patch')) method = 'PATCH';
          
          endpoints.push({
            method,
            path: fullPath,
            controllerClass: controller.className
          });
        }
      } catch (error) {
        console.warn(`Failed to extract endpoints from ${controller.className}:`, error);
      }
    }
    
    return endpoints;
  }
  
  /**
   * Scan frontend React components
   */
  private async scanFrontendComponents(frontendRoot: string): Promise<FrontendComponent[]> {
    const components: FrontendComponent[] = [];
    const srcDir = path.join(frontendRoot, 'src');
    
    if (!fs.existsSync(srcDir)) {
      return components;
    }
    
    const scanDir = (dir: string): void => {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        
        if (entry.isDirectory()) {
          if (this.shouldExclude(fullPath)) {
            continue;
          }
          scanDir(fullPath);
        } else if (entry.isFile() && (entry.name.endsWith('.tsx') || entry.name.endsWith('.jsx'))) {
          try {
            const componentInfo = this.parseReactComponent(fullPath, srcDir);
            if (componentInfo) {
              components.push(componentInfo);
            }
          } catch (error) {
            console.warn(`Failed to parse ${fullPath}:`, error);
          }
        }
      }
    };
    
    scanDir(srcDir);
    return components;
  }
  
  /**
   * Parse a React component file
   */
  private parseReactComponent(filePath: string, srcRoot: string): FrontendComponent | null {
    const content = fs.readFileSync(filePath, 'utf-8');
    const fileName = path.basename(filePath);
    const relativePath = path.relative(srcRoot, filePath);
    
    // Extract component name from filename
    const componentName = fileName.replace(/\.(tsx|jsx)$/, '');
    
    // Extract exports
    const exports = this.extractExports(content);
    
    // Extract hooks usage
    const hooks = this.extractHooks(content);
    
    // Check if it's a page component
    const isPage = relativePath.includes('pages') || relativePath.includes('views') || relativePath.includes('routes');
    
    return {
      componentName,
      filePath,
      relativePath,
      extension: fileName.endsWith('.tsx') ? '.tsx' : '.jsx',
      exports,
      hooks,
      isPage
    };
  }
  
  /**
   * Extract exported items from React file
   */
  private extractExports(content: string): string[] {
    const exports: string[] = [];
    
    // Match named exports
    const namedExportPattern = /export\s+(?:const|function|class)\s+(\w+)/g;
    let match;
    
    while ((match = namedExportPattern.exec(content)) !== null) {
      exports.push(match[1]);
    }
    
    // Match default export
    const defaultExportMatch = content.match(/export\s+default\s+(\w+)/);
    if (defaultExportMatch) {
      exports.push(`default:${defaultExportMatch[1]}`);
    }
    
    return exports;
  }
  
  /**
   * Extract React hooks usage
   */
  private extractHooks(content: string): string[] {
    const hooks: string[] = [];
    const hookPattern = /\b(use[A-Z]\w+)\(/g;
    let match;
    
    while ((match = hookPattern.exec(content)) !== null) {
      if (!hooks.includes(match[1])) {
        hooks.push(match[1]);
      }
    }
    
    return hooks;
  }
  
  /**
   * Check if path should be excluded
   */
  private shouldExclude(filePath: string): boolean {
    const normalizedPath = filePath.replace(/\\/g, '/');
    return this.config.excludePatterns?.some(pattern => {
      const regex = new RegExp(pattern.replace(/\*\*/g, '.*').replace(/\*/g, '[^/]*'));
      return regex.test(normalizedPath);
    }) || false;
  }
  
  /**
   * Group backend classes by type
   */
  private groupByName(classes: BackendClass[], type: BackendClass['type']): Map<string, BackendClass[]> {
    const map = new Map<string, BackendClass[]>();
    
    const filtered = classes.filter(c => c.type === type);
    
    for (const cls of filtered) {
      // Extract domain name from class name (e.g., CDSTrade from CDSTradeService)
      const domainName = cls.className.replace(new RegExp(`${type}$`), '');
      
      if (!map.has(domainName)) {
        map.set(domainName, []);
      }
      map.get(domainName)!.push(cls);
    }
    
    return map;
  }
  
  /**
   * Group frontend components by domain/feature
   */
  private groupComponentsByDomain(components: FrontendComponent[]): Map<string, FrontendComponent[]> {
    const map = new Map<string, FrontendComponent[]>();
    
    for (const component of components) {
      // Extract domain from path (e.g., 'trades' from 'components/trades/TradeForm.tsx')
      const pathParts = component.relativePath.split(/[/\\]/);
      const domain = pathParts.length > 1 ? pathParts[0] : 'common';
      
      if (!map.has(domain)) {
        map.set(domain, []);
      }
      map.get(domain)!.push(component);
    }
    
    return map;
  }
  
  /**
   * Convert BackendClass to DatabaseEntity
   */
  private convertToEntity(backendClass: BackendClass): DatabaseEntity {
    // Extract table name and field metadata from source file
    let tableName: string | undefined;
    let fieldMetadata: EntityFieldMetadata[] = [];
    
    try {
      const content = fs.readFileSync(backendClass.filePath, 'utf-8');
      
      // Extract table name from @Table annotation
      const tableMatch = content.match(/@Table\s*\(\s*name\s*=\s*["']([^"']+)["']/);
      if (tableMatch) {
        tableName = tableMatch[1];
      }
      
      // Extract field metadata for entities
      fieldMetadata = this.extractEntityFieldMetadata(content);
      
    } catch (error) {
      // Ignore errors, return basic entity info
      console.warn(`Failed to extract entity metadata from ${backendClass.className}:`, error);
    }
    
    return {
      entityName: backendClass.className,
      tableName,
      packageName: backendClass.packageName,
      fields: fieldMetadata
    };
  }
  
  /**
   * Find classes relevant to a story
   */
  public findRelevantClasses(context: WorkspaceContext, storyText: string): BackendClass[] {
    const relevant: BackendClass[] = [];
    const storyLower = storyText.toLowerCase();
    
    // Extract potential entity names from story
    const words = storyText.match(/\b[A-Z][a-z]+(?:[A-Z][a-z]+)*\b/g) || [];
    
    for (const cls of context.backendClasses) {
      // Check if class name appears in story
      if (storyLower.includes(cls.className.toLowerCase())) {
        relevant.push(cls);
        continue;
      }
      
      // Check if any word from story matches class name parts
      for (const word of words) {
        if (cls.className.includes(word)) {
          relevant.push(cls);
          break;
        }
      }
    }
    
    return relevant;
  }
  
  /**
   * Find components relevant to a story
   */
  public findRelevantComponents(context: WorkspaceContext, storyText: string): FrontendComponent[] {
    const relevant: FrontendComponent[] = [];
    const storyLower = storyText.toLowerCase();
    
    for (const component of context.frontendComponents) {
      if (storyLower.includes(component.componentName.toLowerCase())) {
        relevant.push(component);
      }
    }
    
    return relevant;
  }
}
