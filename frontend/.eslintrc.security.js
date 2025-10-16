module.exports = {
  extends: [
    'react-app',
    'react-app/jest',
  ],
  plugins: ['security'],
  rules: {
    // Security-focused rules for CDS platform
    'security/detect-object-injection': 'warn',
    'security/detect-non-literal-regexp': 'warn',
    'security/detect-non-literal-fs-filename': 'warn',
    'security/detect-eval-with-expression': 'error',
    'security/detect-pseudoRandomBytes': 'error',
    'security/detect-possible-timing-attacks': 'warn',
    'security/detect-unsafe-regex': 'error',
    'security/detect-buffer-noassert': 'error',
    'security/detect-child-process': 'warn',
    'security/detect-disable-mustache-escape': 'error',
    'security/detect-no-csrf-before-method-override': 'error',
    
    // React security rules
    'react/no-danger': 'warn',
    'react/no-danger-with-children': 'error',
    'react/jsx-no-target-blank': ['error', {
      enforceDynamicLinks: 'always'
    }],
    
    // General security best practices
    'no-eval': 'error',
    'no-implied-eval': 'error',
    'no-new-func': 'error',
    'no-script-url': 'error',
  },
};