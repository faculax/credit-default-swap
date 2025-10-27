#!/usr/bin/env python3
"""
Check DefectDojo metrics and findings status
"""
import requests
import sys
from datetime import datetime

def check_metrics(base_url, token):
    """Check DefectDojo products and their metrics"""
    headers = {'Authorization': f'Token {token}'}
    
    print(f"\n🔍 Checking DefectDojo: {base_url}")
    print("="*70)
    
    # Get all products
    response = requests.get(
        f"{base_url}/api/v2/products/",
        headers=headers,
        timeout=30
    )
    
    if response.status_code != 200:
        print(f"❌ Failed to get products: {response.status_code}")
        return False
    
    products = response.json()['results']
    print(f"\n📦 Found {len(products)} products\n")
    
    for product in products:
        product_id = product['id']
        product_name = product['name']
        
        print(f"\n{'='*70}")
        print(f"Product: {product_name} (ID: {product_id})")
        print(f"{'='*70}")
        
        # Get findings for this product
        findings_response = requests.get(
            f"{base_url}/api/v2/findings/",
            headers=headers,
            params={
                'product': product_id,
                'active': 'true',
                'limit': 1000
            },
            timeout=30
        )
        
        if findings_response.status_code == 200:
            findings = findings_response.json()['results']
            
            # Count by severity
            severity_counts = {
                'Critical': 0,
                'High': 0,
                'Medium': 0,
                'Low': 0,
                'Info': 0
            }
            
            verified_count = 0
            active_count = 0
            
            for finding in findings:
                severity = finding.get('severity', 'Info')
                severity_counts[severity] = severity_counts.get(severity, 0) + 1
                
                if finding.get('verified'):
                    verified_count += 1
                if finding.get('active'):
                    active_count += 1
            
            print(f"\n📊 Findings Summary:")
            print(f"  Total: {len(findings)}")
            print(f"  Active: {active_count}")
            print(f"  Verified: {verified_count}")
            print(f"\n  By Severity:")
            for severity, count in severity_counts.items():
                if count > 0:
                    print(f"    {severity}: {count}")
            
            # Check engagement status
            eng_response = requests.get(
                f"{base_url}/api/v2/engagements/",
                headers=headers,
                params={'product': product_id},
                timeout=30
            )
            
            if eng_response.status_code == 200:
                engagements = eng_response.json()['results']
                print(f"\n  Engagements: {len(engagements)}")
                
                for eng in engagements[:3]:  # Show first 3
                    print(f"    - {eng['name']} (Status: {eng['status']})")
        else:
            print(f"❌ Failed to get findings: {findings_response.status_code}")
    
    print(f"\n{'='*70}")
    print("\n✅ Check complete!")
    print("\nℹ️  If metrics show numbers but aren't clickable:")
    print("   1. Check that findings are marked 'active' and 'verified'")
    print("   2. Verify engagement status is 'In Progress' or 'Completed'")
    print("   3. Try accessing: {base_url}/finding?active=true&verified=true")
    print("   4. Run database metrics recalculation in DefectDojo admin")
    
    return True


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python check-defectdojo-metrics.py <url> <token>")
        print("Example: python check-defectdojo-metrics.py https://defectdojo.example.com abc123")
        sys.exit(1)
    
    url = sys.argv[1].rstrip('/')
    token = sys.argv[2]
    
    check_metrics(url, token)
