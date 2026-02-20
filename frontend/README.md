# TechQuarter Corporate Booking - Frontend

## Quick Start (Local Development)

### Option 1: Open directly in browser
Simply double-click `index.html` or open it in your browser. 
Then go to **⚙️ Settings** and click **"🖥️ Use Local (SAM Local)"**.

### Option 2: Use a simple HTTP server
```bash
# Python
cd frontend
python -m http.server 8080

# Node.js (npx)
cd frontend
npx serve .
```

Then visit `http://localhost:8080`

## Testing with SAM Local

1. **Build & start the local API**:
   ```bash
   sam build
   sam local start-api --warm-containers EAGER
   ```
   This starts the API at `http://127.0.0.1:3000`

2. **Open the frontend** and go to **Settings** → click **"Use Local (SAM Local)"**

3. **Test everything!**
   - Register employees in the **Employees** tab
   - Create bookings in the **Bookings** tab
   - Search, update status, delete

## Deploy to AWS

### 1. Deploy backend (SAM)
```bash
sam build
sam deploy --guided
```

### 2. Deploy frontend to S3
```bash
# Get the bucket name from the SAM output
aws s3 sync frontend/ s3://YOUR-FRONTEND-BUCKET-NAME --delete

# Clear CloudFront cache
aws cloudfront create-invalidation --distribution-id YOUR-DIST-ID --paths "/*"
```

### 3. Configure the frontend
Open the CloudFront URL → **Settings** → paste the `ApiBaseUrl` from SAM outputs → **Save**

## Architecture

```
frontend/
├── index.html    # Single-page HTML app
├── styles.css    # Responsive CSS styles
├── app.js        # Vanilla JS API calls & UI logic
└── README.md     # This file
```

No build step, no npm, no bundler. Just HTML + CSS + JS. 🚀

