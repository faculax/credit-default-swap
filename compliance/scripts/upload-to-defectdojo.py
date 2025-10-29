#!/usr/bin/env python3
"""
DefectDojo Security Scan Uploader for CI/CD
Uploads security scan results to DefectDojo via REST API
"""

import argparse
import json
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

try:
    import requests
    from requests.adapters import HTTPAdapter
    from urllib3.util.retry import Retry
except ImportError:
    print("❌ ERROR: Required dependencies not installed")
    print("Install with: pip install requests")
    sys.exit(1)


class DefectDojoUploader:
    """Upload security scan results to DefectDojo"""
    
    # Scanner type mappings for DefectDojo
    SCAN_TYPE_MAP = {
        'dependency-check-report.json': 'Dependency Check Scan',
        'spotbugsXml.xml': 'SpotBugs Scan',
        'checkstyle-result.xml': 'Checkstyle Scan',
        'audit-npm.json': 'NPM Audit Scan',
        'eslint-security.json': 'ESLint Scan',
        'retire-report.json': 'Retire.js Scan',
        'gitleaks-report.json': 'Gitleaks Scan',
    }
    
    def __init__(self, url: str, username: str = None, password: str = None, token: str = None):
        self.base_url = url.rstrip('/')
        self.username = username
        self.password = password
        self.token = token
        self.session = self._create_session()
        
        # If token provided directly, set it up
        if self.token:
            self.session.headers.update({
                'Authorization': f'Token {self.token}',
                'Content-Type': 'application/json'
            })
    
    def _create_session(self) -> requests.Session:
        """Create a requests session with retry logic"""
        session = requests.Session()
        
        retry_strategy = Retry(
            total=3,
            backoff_factor=1,
            status_forcelist=[429, 500, 502, 503, 504],
        )
        
        adapter = HTTPAdapter(max_retries=retry_strategy)
        session.mount("http://", adapter)
        session.mount("https://", adapter)
        
        return session
    
    def authenticate(self) -> bool:
        """Authenticate and get API token"""
        # If token already provided, skip authentication
        if self.token:
            print(f"✅ Using provided API token (token: {self.token[:10]}...)")
            return True
            
        print("🔐 Authenticating with DefectDojo...")
        
        try:
            response = self.session.post(
                f"{self.base_url}/api/v2/api-token-auth/",
                json={"username": self.username, "password": self.password},
                timeout=30
            )
            
            if response.status_code == 200:
                self.token = response.json()['token']
                self.session.headers.update({
                    'Authorization': f'Token {self.token}',
                    'Content-Type': 'application/json'
                })
                print(f"✅ Authentication successful (token: {self.token[:10]}...)")
                return True
            else:
                print(f"❌ Authentication failed: {response.status_code}")
                print(f"Response: {response.text}")
                return False
                
        except Exception as e:
            print(f"❌ Authentication error: {e}")
            return False
    
    def get_or_create_product(self, product_name: str) -> Optional[int]:
        """Get or create a product"""
        print(f"📦 Finding/creating product: {product_name}")
        
        try:
            # Search for existing product
            response = self.session.get(
                f"{self.base_url}/api/v2/products/",
                params={"name": product_name},
                timeout=30
            )
            
            if response.status_code == 200:
                results = response.json()['results']
                if results:
                    product_id = results[0]['id']
                    print(f"✅ Found existing product (ID: {product_id})")
                    return product_id
            
            # Get or create product type
            prod_type_id = self._get_or_create_product_type("Web Application")
            if not prod_type_id:
                print("❌ Failed to get/create product type")
                return None
            
            # Create new product
            response = self.session.post(
                f"{self.base_url}/api/v2/products/",
                json={
                    "name": product_name,
                    "description": "Credit Default Swap Trading Platform",
                    "prod_type": prod_type_id
                },
                timeout=30
            )
            
            if response.status_code == 201:
                product_id = response.json()['id']
                print(f"✅ Created new product (ID: {product_id})")
                return product_id
            else:
                print(f"❌ Failed to create product: {response.text}")
                return None
                
        except Exception as e:
            print(f"❌ Product error: {e}")
            return None
    
    def _get_or_create_product_type(self, type_name: str) -> Optional[int]:
        """Get or create a product type"""
        try:
            # Search for existing product type
            response = self.session.get(
                f"{self.base_url}/api/v2/product_types/",
                params={"name": type_name},
                timeout=30
            )
            
            if response.status_code == 200:
                results = response.json()['results']
                if results:
                    return results[0]['id']
            
            # Create new product type
            response = self.session.post(
                f"{self.base_url}/api/v2/product_types/",
                json={"name": type_name},
                timeout=30
            )
            
            if response.status_code == 201:
                return response.json()['id']
            
            # If creation failed, try to get any existing product type
            response = self.session.get(
                f"{self.base_url}/api/v2/product_types/",
                timeout=30
            )
            if response.status_code == 200:
                results = response.json()['results']
                if results:
                    print(f"⚠️  Using existing product type: {results[0]['name']}")
                    return results[0]['id']
            
            return None
            
        except Exception as e:
            print(f"❌ Product type error: {e}")
            return None
    
    def get_or_create_engagement(
        self, 
        product_id: int, 
        engagement_name: str,
        reuse_today: bool = True
    ) -> Optional[int]:
        """Get or create an engagement"""
        print(f"📋 Finding/creating engagement: {engagement_name}")
        
        try:
            # If reuse_today, search for today's engagement
            if reuse_today:
                today = datetime.now().date().isoformat()
                response = self.session.get(
                    f"{self.base_url}/api/v2/engagements/",
                    params={
                        "product": product_id,
                        "target_start": today
                    },
                    timeout=30
                )
                
                if response.status_code == 200:
                    results = response.json()['results']
                    if results:
                        engagement_id = results[0]['id']
                        print(f"✅ Reusing today's engagement (ID: {engagement_id})")
                        return engagement_id
            
            # Create new engagement
            today = datetime.now().date().isoformat()
            response = self.session.post(
                f"{self.base_url}/api/v2/engagements/",
                json={
                    "name": engagement_name,
                    "product": product_id,
                    "target_start": today,
                    "target_end": today,
                    "status": "In Progress",
                    "engagement_type": "CI/CD"
                },
                timeout=30
            )
            
            if response.status_code == 201:
                engagement_id = response.json()['id']
                print(f"✅ Created new engagement (ID: {engagement_id})")
                return engagement_id
            else:
                print(f"❌ Failed to create engagement: {response.text}")
                return None
                
        except Exception as e:
            print(f"❌ Engagement error: {e}")
            return None
    
    def upload_scan(
        self,
        engagement_id: int,
        scan_type: str,
        file_path: Path,
        tags: List[str] = None
    ) -> Optional[bool]:
        """Upload a scan result"""
        print(f"📤 Uploading {scan_type}: {file_path.name}")
        
        try:
            # Read file content first
            with open(file_path, 'rb') as f:
                file_content = f.read()
            
            # Prepare files for upload
            files = {'file': (file_path.name, file_content)}
            
            data = {
                'engagement': str(engagement_id),
                'scan_type': scan_type,
                'active': 'true',
                'verified': 'true',
                'close_old_findings': 'true',
                'push_to_jira': 'false',
                'minimum_severity': 'Info',
                'scan_date': datetime.now().date().isoformat(),
            }
            
            if tags:
                data['tags'] = ','.join(tags)
            
            # Use requests.post directly with only Authorization header
            # This prevents any session-level headers from interfering
            headers = {'Authorization': f'Token {self.token}'}
            
            response = requests.post(
                f"{self.base_url}/api/v2/import-scan/",
                files=files,
                data=data,
                headers=headers,
                timeout=120
            )
            
            if response.status_code == 201:
                result = response.json()
                print(f"✅ Upload successful - Test ID: {result.get('test')}")
                return True
            else:
                error_msg = response.text
                # Handle known npm audit limitation
                if 'npm7 with auditReportVersion' in error_msg:
                    print(f"⚠️  Upload skipped: npm audit v7+ format not supported by DefectDojo")
                    print(f"    (Frontend security is still covered by ESLint and Retire.js)")
                    return None  # Return None to indicate "skip" rather than "fail"
                else:
                    print(f"❌ Upload failed ({response.status_code}): {error_msg}")
                    return False
                    
        except Exception as e:
            print(f"❌ Upload error: {e}")
            return False
    
    def find_scan_files(self, scan_dir: Path) -> Dict[str, List[Path]]:
        """Find all scan files organized by component"""
        print(f"🔍 Scanning for security reports in: {scan_dir}")
        
        scan_files = {}
        
        for component_dir in scan_dir.iterdir():
            if not component_dir.is_dir():
                continue
            
            component_name = component_dir.name.replace('-security-reports', '')
            files = []
            
            for file_path in component_dir.rglob('*'):
                if file_path.is_file():
                    for pattern, scan_type in self.SCAN_TYPE_MAP.items():
                        if file_path.name == pattern:
                            files.append(file_path)
                            break
            
            if files:
                scan_files[component_name] = files
        
        return scan_files
    
    def upload_all(
        self,
        product_name: str,
        engagement_name: str,
        scan_dir: Path,
        use_component_tags: bool = True,
        reuse_engagement: bool = True
    ) -> bool:
        """Upload all scan results (legacy mode - single product with tags)"""
        print("\n" + "="*70)
        print("  DEFECTDOJO SECURITY SCAN UPLOAD")
        print("="*70 + "\n")
        
        if not self.authenticate():
            return False
        
        product_id = self.get_or_create_product(product_name)
        if not product_id:
            return False
        
        engagement_id = self.get_or_create_engagement(
            product_id, 
            engagement_name,
            reuse_engagement
        )
        if not engagement_id:
            return False
        
        # Find all scan files
        scan_files = self.find_scan_files(scan_dir)
        
        if not scan_files:
            print("⚠️  No scan files found")
            return False
        
        print(f"\n📊 Found scan results for {len(scan_files)} component(s)\n")
        
        # Upload each scan
        success_count = 0
        fail_count = 0
        skip_count = 0
        
        for component, files in scan_files.items():
            print(f"\n{'='*70}")
            print(f"  Component: {component}")
            print(f"{'='*70}\n")
            
            tags = [component] if use_component_tags else None
            
            for file_path in files:
                # Determine scan type
                scan_type = None
                for pattern, stype in self.SCAN_TYPE_MAP.items():
                    if file_path.name == pattern:
                        scan_type = stype
                        break
                
                if not scan_type:
                    print(f"⚠️  Unknown scan type for: {file_path.name}")
                    continue
                
                result = self.upload_scan(engagement_id, scan_type, file_path, tags)
                if result is True:
                    success_count += 1
                elif result is False:
                    fail_count += 1
                elif result is None:
                    skip_count += 1
        
        print(f"\n{'='*70}")
        print(f"  UPLOAD SUMMARY")
        print(f"{'='*70}")
        print(f"✅ Successful uploads: {success_count}")
        if skip_count > 0:
            print(f"⚠️  Skipped uploads: {skip_count} (unsupported format)")
        print(f"❌ Failed uploads: {fail_count}")
        print(f"\n🔗 View results: {self.base_url}/dashboard")
        print(f"{'='*70}\n")
        
        return fail_count == 0
    
    def upload_by_component(
        self,
        product_prefix: str,
        engagement_name: str,
        scan_dir: Path,
        reuse_engagement: bool = True
    ) -> bool:
        """Upload scan results - creates separate product per component"""
        print("\n" + "="*70)
        print("  DEFECTDOJO SECURITY SCAN UPLOAD (Per-Component Products)")
        print("="*70 + "\n")
        
        if not self.authenticate():
            return False
        
        # Find all scan files
        scan_files = self.find_scan_files(scan_dir)
        
        if not scan_files:
            print("⚠️  No scan files found")
            return False
        
        print(f"\n📊 Found scan results for {len(scan_files)} component(s)\n")
        
        # Upload each component to its own product
        total_success = 0
        total_fail = 0
        total_skip = 0
        
        for component, files in scan_files.items():
            # Create product name from component
            component_product = f"{product_prefix} - {component.replace('-', ' ').title()}"
            
            print(f"\n{'='*70}")
            print(f"  Product: {component_product}")
            print(f"{'='*70}\n")
            
            # Create/get product for this component
            product_id = self.get_or_create_product(component_product)
            if not product_id:
                print(f"❌ Failed to create product for {component}")
                total_fail += len(files)
                continue
            
            # Create/get engagement for this component
            engagement_id = self.get_or_create_engagement(
                product_id,
                engagement_name,
                reuse_engagement
            )
            if not engagement_id:
                print(f"❌ Failed to create engagement for {component}")
                total_fail += len(files)
                continue
            
            # Upload scans for this component
            for file_path in files:
                # Determine scan type
                scan_type = None
                for pattern, stype in self.SCAN_TYPE_MAP.items():
                    if file_path.name == pattern:
                        scan_type = stype
                        break
                
                if not scan_type:
                    print(f"⚠️  Unknown scan type for: {file_path.name}")
                    continue
                
                result = self.upload_scan(engagement_id, scan_type, file_path, tags=None)
                if result is True:
                    total_success += 1
                elif result is False:
                    total_fail += 1
                elif result is None:
                    total_skip += 1
        
        print(f"\n{'='*70}")
        print(f"  OVERALL UPLOAD SUMMARY")
        print(f"{'='*70}")
        print(f"📦 Products created/updated: {len(scan_files)}")
        print(f"✅ Successful uploads: {total_success}")
        if total_skip > 0:
            print(f"⚠️  Skipped uploads: {total_skip} (unsupported format)")
        print(f"❌ Failed uploads: {total_fail}")
        print(f"\n🔗 View results: {self.base_url}/dashboard")
        print(f"{'='*70}\n")
        
        return total_fail == 0


def main():
    parser = argparse.ArgumentParser(
        description='Upload security scan results to DefectDojo'
    )
    
    parser.add_argument('--url', required=True, help='DefectDojo URL')
    parser.add_argument('--token', help='DefectDojo API token')
    parser.add_argument('--username', help='DefectDojo username (if not using token)')
    parser.add_argument('--password', help='DefectDojo password (if not using token)')
    parser.add_argument('--product', required=True, help='Product name or prefix')
    parser.add_argument('--engagement', required=True, help='Engagement name')
    parser.add_argument('--scan-dir', required=True, help='Directory with scan results')
    parser.add_argument('--component-tags', action='store_true', 
                       help='Tag findings by component (legacy mode)')
    parser.add_argument('--separate-products', action='store_true',
                       help='Create separate product per component (recommended)')
    parser.add_argument('--no-reuse-engagement', action='store_true',
                       help="Don't reuse today's engagement")
    
    args = parser.parse_args()
    
    # Validate authentication method
    if not args.token and not (args.username and args.password):
        print("❌ ERROR: Must provide either --token or both --username and --password")
        sys.exit(1)
    
    scan_dir = Path(args.scan_dir)
    if not scan_dir.exists():
        print(f"❌ Scan directory not found: {scan_dir}")
        sys.exit(1)
    
    uploader = DefectDojoUploader(
        url=args.url,
        username=args.username,
        password=args.password,
        token=args.token
    )
    
    # Use separate products mode if requested
    if args.separate_products:
        success = uploader.upload_by_component(
            product_prefix=args.product,
            engagement_name=args.engagement,
            scan_dir=scan_dir,
            reuse_engagement=not args.no_reuse_engagement
        )
    else:
        success = uploader.upload_all(
            product_name=args.product,
            engagement_name=args.engagement,
            scan_dir=scan_dir,
            use_component_tags=args.component_tags,
            reuse_engagement=not args.no_reuse_engagement
        )
    
    sys.exit(0 if success else 1)


if __name__ == '__main__':
    main()
