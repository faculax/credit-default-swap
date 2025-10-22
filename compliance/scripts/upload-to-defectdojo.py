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
    
    def __init__(self, url: str, username: str, password: str):
        self.base_url = url.rstrip('/')
        self.username = username
        self.password = password
        self.token = None
        self.session = self._create_session()
    
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
            
            # Create new product
            response = self.session.post(
                f"{self.base_url}/api/v2/products/",
                json={
                    "name": product_name,
                    "description": "Credit Default Swap Trading Platform",
                    "prod_type": 1  # Assuming product type 1 exists
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
    ) -> bool:
        """Upload a scan result"""
        print(f"📤 Uploading {scan_type}: {file_path.name}")
        
        try:
            with open(file_path, 'rb') as f:
                files = {'file': (file_path.name, f, 'application/octet-stream')}
                
                data = {
                    'engagement': engagement_id,
                    'scan_type': scan_type,
                    'active': 'true',
                    'verified': 'true',
                    'close_old_findings': 'true',
                }
                
                if tags:
                    data['tags'] = ','.join(tags)
                
                # Remove Content-Type header for multipart
                headers = dict(self.session.headers)
                if 'Content-Type' in headers:
                    del headers['Content-Type']
                
                response = self.session.post(
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
                    print(f"❌ Upload failed ({response.status_code}): {response.text}")
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
        """Upload all scan results"""
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
                
                if self.upload_scan(engagement_id, scan_type, file_path, tags):
                    success_count += 1
                else:
                    fail_count += 1
        
        print(f"\n{'='*70}")
        print(f"  UPLOAD SUMMARY")
        print(f"{'='*70}")
        print(f"✅ Successful uploads: {success_count}")
        print(f"❌ Failed uploads: {fail_count}")
        print(f"\n🔗 View results: {self.base_url}/dashboard")
        print(f"{'='*70}\n")
        
        return fail_count == 0


def main():
    parser = argparse.ArgumentParser(
        description='Upload security scan results to DefectDojo'
    )
    
    parser.add_argument('--url', required=True, help='DefectDojo URL')
    parser.add_argument('--username', required=True, help='DefectDojo username')
    parser.add_argument('--password', required=True, help='DefectDojo password')
    parser.add_argument('--product', required=True, help='Product name')
    parser.add_argument('--engagement', required=True, help='Engagement name')
    parser.add_argument('--scan-dir', required=True, help='Directory with scan results')
    parser.add_argument('--component-tags', action='store_true', 
                       help='Tag findings by component')
    parser.add_argument('--no-reuse-engagement', action='store_true',
                       help="Don't reuse today's engagement")
    
    args = parser.parse_args()
    
    scan_dir = Path(args.scan_dir)
    if not scan_dir.exists():
        print(f"❌ Scan directory not found: {scan_dir}")
        sys.exit(1)
    
    uploader = DefectDojoUploader(args.url, args.username, args.password)
    
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
