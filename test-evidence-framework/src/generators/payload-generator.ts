/**
 * Payload Generator
 * 
 * Generates realistic test payloads based on actual entity field metadata.
 * Uses workspace context to build JSON that matches real API contracts.
 * 
 * NO HARDCODED DATA - all payloads derived from entity structure.
 */

import type { EntityFieldMetadata, DatabaseEntity, WorkspaceContext } from '../models/workspace-context-model.js';

export class PayloadGenerator {
  
  /**
   * Generate realistic JSON payload for entity
   */
  public generatePayload(entity: DatabaseEntity, scenario: 'valid' | 'invalid' = 'valid'): string {
    if (!entity.fields || entity.fields.length === 0) {
      throw new Error(`Entity ${entity.entityName} has no field metadata. Cannot generate payload.`);
    }
    
    const payload: Record<string, any> = {};
    
    for (const field of entity.fields) {
      const value = scenario === 'valid' 
        ? this.generateValidValue(field)
        : this.generateInvalidValue(field);
      
      payload[field.name] = value;
    }
    
    return JSON.stringify(payload, null, 2);
  }
  
  /**
   * Generate valid test value for field
   */
  private generateValidValue(field: EntityFieldMetadata): any {
    // Only set null if field is explicitly nullable
    // For test data, always provide values unless nullable=true
    if (field.nullable) {
      return null;
    }
    
    switch (field.jsonType) {
      case 'string':
        return this.generateString(field);
      
      case 'number':
        return this.generateNumber(field);
      
      case 'boolean':
        return Math.random() < 0.5;
      
      case 'date':
        return this.generateDate(field);
      
      case 'datetime':
        return this.generateDateTime(field);
      
      case 'enum':
        return this.generateEnum(field);
      
      default:
        return null;
    }
  }
  
  /**
   * Generate invalid test value for field
   */
  private generateInvalidValue(field: EntityFieldMetadata): any {
    switch (field.jsonType) {
      case 'string':
        // Exceed max length
        if (field.maxLength) {
          return 'X'.repeat(field.maxLength + 10);
        }
        return '';
      
      case 'number':
        // Negative for positive fields, or wrong format
        return 'invalid-number';
      
      case 'date':
        // Invalid date format
        return 'invalid-date';
      
      case 'enum':
        // Invalid enum value
        return 'INVALID_ENUM_VALUE';
      
      default:
        return null;
    }
  }
  
  /**
   * Generate realistic string value
   */
  private generateString(field: EntityFieldMetadata): string {
    const fieldLower = field.name.toLowerCase();
    
    // Domain-specific strings
    if (fieldLower.includes('reference') || fieldLower.includes('entity')) {
      return this.pickRandom(['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA']);
    }
    
    if (fieldLower.includes('counterparty') || fieldLower.includes('party')) {
      return this.pickRandom(['JPMORGAN', 'GOLDMAN_SACHS', 'BARCLAYS', 'CITI', 'HSBC']);
    }
    
    if (fieldLower.includes('currency')) {
      return this.pickRandom(['USD', 'EUR', 'GBP', 'JPY']);
    }
    
    if (fieldLower.includes('frequency')) {
      return this.pickRandom(['QUARTERLY', 'MONTHLY', 'SEMI_ANNUAL', 'ANNUAL']);
    }
    
    if (fieldLower.includes('convention')) {
      return this.pickRandom(['ACT_360', 'ACT_365', 'THIRTY_360']);
    }
    
    if (fieldLower.includes('clause') || fieldLower.includes('restructuring')) {
      return this.pickRandom(['MOD_R', 'MOD_MOD_R', 'CR', 'OLD_R', 'NO_R']);
    }
    
    if (fieldLower.includes('calendar')) {
      return this.pickRandom(['NYC', 'LON', 'TARGET']);
    }
    
    if (fieldLower.includes('description')) {
      return `Test ${field.name} for integration testing`;
    }
    
    // Generic string with length constraint
    const maxLen = field.maxLength || 50;
    const testValue = `TEST_${field.name.toUpperCase()}`;
    return testValue.substring(0, Math.min(testValue.length, maxLen));
  }
  
  /**
   * Generate realistic number value
   */
  private generateNumber(field: EntityFieldMetadata): number {
    const fieldLower = field.name.toLowerCase();
    
    // Domain-specific numbers
    if (fieldLower.includes('notional') || fieldLower.includes('amount')) {
      return 10000000; // 10M
    }
    
    if (fieldLower.includes('spread') || fieldLower.includes('coupon')) {
      return 100; // 100 bps
    }
    
    if (fieldLower.includes('recovery') || fieldLower.includes('rate')) {
      const scale = field.scale || 2;
      if (scale > 0) {
        return 40.0; // 40% recovery rate
      }
      return 40;
    }
    
    if (fieldLower.includes('version')) {
      return 1;
    }
    
    // Generic number based on precision/scale
    if (field.precision && field.scale) {
      // Decimal number
      const intDigits = field.precision - field.scale;
      const maxValue = Math.pow(10, intDigits) - 1;
      return Math.random() * maxValue;
    }
    
    // Integer
    return Math.floor(Math.random() * 1000);
  }
  
  /**
   * Generate realistic date value
   */
  private generateDate(field: EntityFieldMetadata): string {
    const fieldLower = field.name.toLowerCase();
    const now = new Date();
    
    // Domain-specific dates
    if (fieldLower.includes('maturity')) {
      // Future date (5 years out)
      const future = new Date(now);
      future.setFullYear(future.getFullYear() + 5);
      return future.toISOString().split('T')[0];
    }
    
    if (fieldLower.includes('effective') || fieldLower.includes('start') || fieldLower.includes('accrual')) {
      // Recent past (1 month ago)
      const past = new Date(now);
      past.setMonth(past.getMonth() - 1);
      return past.toISOString().split('T')[0];
    }
    
    if (fieldLower.includes('trade') || fieldLower.includes('created')) {
      // Today
      return now.toISOString().split('T')[0];
    }
    
    // Default: yesterday
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    return yesterday.toISOString().split('T')[0];
  }
  
  /**
   * Generate realistic datetime value (ISO 8601)
   */
  private generateDateTime(field: EntityFieldMetadata): string {
    const fieldLower = field.name.toLowerCase();
    const now = new Date();
    
    // Domain-specific datetimes
    if (fieldLower.includes('created') || fieldLower.includes('updated')) {
      // Recent timestamp (within last hour)
      const recent = new Date(now);
      recent.setMinutes(recent.getMinutes() - Math.floor(Math.random() * 60));
      return recent.toISOString();
    }
    
    if (fieldLower.includes('novation') || fieldLower.includes('timestamp')) {
      // Past timestamp (within last 30 days)
      const past = new Date(now);
      past.setDate(past.getDate() - Math.floor(Math.random() * 30));
      return past.toISOString();
    }
    
    // Default: now
    return now.toISOString();
  }
  
  /**
   * Generate enum value
   */
  private generateEnum(field: EntityFieldMetadata): string {
    if (field.enumValues && field.enumValues.length > 0) {
      return this.pickRandom(field.enumValues);
    }
    
    // Fallback enum values based on field name
    const fieldLower = field.name.toLowerCase();
    
    if (fieldLower.includes('status')) {
      return this.pickRandom(['ACTIVE', 'PENDING', 'TERMINATED', 'SETTLED']);
    }
    
    if (fieldLower.includes('direction') || fieldLower.includes('protection')) {
      return this.pickRandom(['BUY', 'SELL']);
    }
    
    if (fieldLower.includes('settlement')) {
      return this.pickRandom(['CASH', 'PHYSICAL']);
    }
    
    return 'UNKNOWN';
  }
  
  /**
   * Pick random value from array
   */
  private pickRandom<T>(options: T[]): T {
    return options[Math.floor(Math.random() * options.length)];
  }
  
  /**
   * Find entity in workspace context by name (fuzzy match)
   */
  public static findEntity(context: WorkspaceContext, entityName: string): DatabaseEntity | null {
    // Exact match
    let entity = context.entities.find(e => e.entityName === entityName);
    if (entity) return entity;
    
    // Partial match (e.g., "Trade" matches "CDSTrade")
    entity = context.entities.find(e => e.entityName.toLowerCase().includes(entityName.toLowerCase()));
    if (entity) return entity;
    
    // Reverse partial match (e.g., "CDSTrade" matches "Trade")
    entity = context.entities.find(e => entityName.toLowerCase().includes(e.entityName.toLowerCase()));
    if (entity) return entity;
    
    return null;
  }
  
  /**
   * Generate Java object instantiation code
   */
  public generateJavaObjectCode(entity: DatabaseEntity, varName: string): string {
    if (!entity.fields || entity.fields.length === 0) {
      throw new Error(`Entity ${entity.entityName} has no field metadata. Cannot generate Java code.`);
    }
    
    const lines: string[] = [];
    lines.push(`${entity.entityName} ${varName} = new ${entity.entityName}();`);
    
    for (const field of entity.fields) {
      const value = this.generateJavaValue(field);
      const setter = `set${this.capitalize(field.name)}`;
      lines.push(`${varName}.${setter}(${value});`);
    }
    
    return lines.join('\n');
  }
  
  /**
   * Generate Java value expression
   */
  private generateJavaValue(field: EntityFieldMetadata): string {
    switch (field.javaType) {
      case 'String':
        return `"${this.generateString(field)}"`;
      
      case 'BigDecimal':
        return `new BigDecimal("${this.generateNumber(field)}")`;
      
      case 'Integer':
      case 'Long':
        return `${Math.floor(this.generateNumber(field))}`;
      
      case 'Boolean':
        return Math.random() < 0.5 ? 'true' : 'false';
      
      case 'LocalDate':
        const date = this.generateDate(field);
        const parts = date.split('-');
        return `LocalDate.of(${parts[0]}, ${Number.parseInt(parts[1])}, ${Number.parseInt(parts[2])})`;
      
      default:
        // Enum or custom type
        if (field.enumValues && field.enumValues.length > 0) {
          return `${field.javaType}.${this.pickRandom(field.enumValues)}`;
        }
        return 'null';
    }
  }
  
  /**
   * Capitalize first letter
   */
  private capitalize(str: string): string {
    return str.charAt(0).toUpperCase() + str.slice(1);
  }
}
