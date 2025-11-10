# Branch Protection: Suggested Configuration

This file documents the recommended branch protection rule to require the `Code Quality Gate` workflow before merges.

Recommended JSON payload (example):

```json
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "Code Quality Gate"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 1
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
```

gh CLI example (apply locally):

```bash
gh api -X PUT \
  -H "Accept: application/vnd.github+json" \
  /repos/<owner>/<repo>/branches/main/protection \
  -f required_status_checks.strict=true \
  -f required_status_checks.contexts='["Code Quality Gate"]' \
  -f enforce_admins=true \
  -f required_pull_request_reviews.dismiss_stale_reviews=false \
  -f required_pull_request_reviews.require_code_owner_reviews=false \
  -f required_pull_request_reviews.required_approving_review_count=1 \
  -f allow_force_pushes=false \
  -f allow_deletions=false
```

UI steps:

1. Settings → Branches → Add rule
2. Branch name: `main` (or `develop`)
3. Require status checks: add `Code Quality Gate`
4. Require pull request reviews: 1
5. Require branches to be up-to-date before merging (strict)
6. Include administrators (optional)

Verification:
- Create a PR and confirm the `Code Quality Gate` check is present. The merge button should be blocked until the check passes and required approvals are present.

Notes:
- Use the exact check name as reported by GitHub in PR checks. If the label differs, update the contexts array.
- Applying branch protection requires admin rights on the repo.
