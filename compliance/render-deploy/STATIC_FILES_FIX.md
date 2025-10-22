# Static Files 404 Fix - Root Cause Analysis

## Problem
DefectDojo UI was loading HTML but all CSS/JavaScript files returned 404 errors:
- `/static/jquery/dist/jquery.js` - 404
- `/static/bootstrap/dist/css/bootstrap.min.css` - 404
- `/static/moment/min/moment.min.js` - 404
- `/static/jszip/dist/jszip.min.js` - 404
- And 22+ other static assets

## Root Cause

The **`/app/components/node_modules/` directory was EMPTY** in the container!

### Why This Happened

1. **Official DefectDojo Architecture** uses **separate containers**:
   - **defectdojo/defectdojo-django** - Python/Django application (NO node_modules)
   - **defectdojo/defectdojo-nginx** - Nginx with pre-built static files

2. **Django's settings.dist.py** includes:
   ```python
   STATICFILES_DIRS = (
       Path(DOJO_ROOT).parent / "components" / "node_modules",
   )
   ```

3. **collectstatic command** was configured correctly but had **nothing to copy**:
   - Django's `STATICFILES_DIRS` pointed to `/app/components/node_modules/`
   - The directory existed but contained **0 items**
   - Result: Only Django app static files were collected (10,040 files)
   - Missing: All npm packages (jquery, bootstrap, moment, datatables, etc.)

### Investigation Journey

1. **Initial Symptoms**: Browser console showing 22+ 404 errors for static files

2. **First Hypothesis**: collectstatic not running → **WRONG** (was running fine)

3. **Second Hypothesis**: Wrong paths or nginx config → **WRONG** (config was correct)

4. **Third Hypothesis**: `--clear` flag deleting files → **PARTIALLY CORRECT** (removed it)

5. **Manual Symlinking Attempts**: Tried creating symlinks for node_modules packages → **FAILED** (shell expansion issues)

6. **Breakthrough**: Analyzed user's local DefectDojo instance
   - Found `/usr/share/nginx/html/static/` contained all npm packages
   - Discovered packages are in **nginx container**, not Django container
   - Official architecture: Static files built separately

7. **Final Discovery**: `/app/components/node_modules/` was empty
   ```
   Contents: 0 items
     - /app/components/node_modules (exists: True)
   ```

## Solution

**Install node_modules during Docker build:**

1. **Added Node.js and yarn** to Dockerfile
2. **Created package.json** with DefectDojo's frontend dependencies
3. **Run `yarn install`** during image build

### Changes Made

**Dockerfile additions:**
```dockerfile
# Install Node.js 20.x (LTS)
RUN apt-get update && \
    apt-get install -y gnupg ca-certificates && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g yarn

# Install node_modules
WORKDIR /app/components
COPY package.json yarn.lock* ./
RUN yarn install --production --frozen-lockfile && \
    yarn cache clean
WORKDIR /app
```

**package.json created** with dependencies:
- jquery ^3.7.1
- bootstrap ^3.4.1
- moment ^2.30.1
- jszip ^3.10.1
- datatables.net ^2.3.4
- flot, morris.js, pdfmake, etc.

## Expected Outcome

After rebuild:
1. `/app/components/node_modules/` will contain ~30+ npm packages
2. `collectstatic` will copy **many more files** (not just 10,040)
3. Browser will load CSS/JS files successfully (200 OK)
4. DefectDojo UI will display with proper styling

## Deployment

Changes pushed to GitHub in commits:
- `e54f91c` - Install node_modules in Dockerfile
- `da4232a` - Add package.json

Render will automatically rebuild and redeploy.

## Verification

After deployment completes, check logs for:
```
==> Collecting static files...
<many more files than before>

==> Verifying static files...
✓ jquery/dist/jquery.js EXISTS
✓ bootstrap/dist/css/bootstrap.min.css EXISTS
✓ moment/min/moment.min.js EXISTS
✓ jszip/dist/jszip.min.js EXISTS
```

Browser console should show 200 OK for all static files instead of 404 errors.

---

**Status**: Fix deployed, awaiting Render rebuild completion.
