# CitySeva Dhule — Premium Admin Website

A responsive React + Vite admin console for the existing Firebase project `cityseva-dhule`.

## Included
- Firebase Phone Authentication with reCAPTCHA
- Admin-only access using a trusted Firebase custom claim (`admin: true`)
- Live dashboard counts and request activity
- Retailer approve / reject / deactivate controls
- Customer directory
- Service request status management
- Firebase Hosting configuration
- Starter Firestore security rules

## 1. Configure environment
Copy `.env.example` to `.env`. The Firebase Web App configuration is already filled in for project `cityseva-dhule`. Do not commit `.env` if you later add private values.

If your Android app uses different Firestore collection names, change:
- `VITE_USERS_COLLECTION` (default `users`)
- `VITE_REQUESTS_COLLECTION` (default `serviceRequests`)

The app currently expects:
- `users/{uid}` documents with `role: "customer"` or `role: "retailer"`, plus fields such as `name`, `phoneNumber`, `address`, `active`, `status`, `createdAt`.
- `serviceRequests/{requestId}` documents with `userId`, `name` or `fullName`, `phoneNumber` or `mobile`, `service` or `serviceName`, `address`, `status`, `createdAt`.

These collection/field names must be aligned with the Android app before production. This starter does not guess or migrate existing records.

## 2. Install and run locally
```bash
npm install
npm run dev
```
Open the local URL printed by Vite.

## 3. Enable Phone sign-in
In Firebase Console:
1. Authentication → Sign-in method → Phone: enabled.
2. Authentication → Settings → Authorized domains: add your Firebase Hosting domain (for example `cityseva-dhule.web.app`) and your custom domain if used.
3. Configure SMS region policy to allow India and review Firebase phone-auth quotas/billing.

## 4. Give your own phone account admin access (required)
A Firestore document with `role: "admin"` is NOT sufficient to authorize this website. The website checks the Firebase Authentication custom claim `admin: true`.

After you have signed in once with your own phone number on the website and the Firebase Authentication user exists:

1. Create a service account key in Firebase Console → Project settings → Service accounts → Generate new private key. Keep it private; never upload it to GitHub or put it in the website.
2. On your own trusted computer, set the environment variable:
   - Windows PowerShell:
     `$env:GOOGLE_APPLICATION_CREDENTIALS="C:\secure\service-account.json"`
   - macOS/Linux:
     `export GOOGLE_APPLICATION_CREDENTIALS="/secure/service-account.json"`
3. From this project folder run:
   `npm run set-admin -- +91YOURMOBILENUMBER`
4. Sign out and sign back in to the admin website.

Only the trusted Admin SDK script can grant this claim. Never expose a service-account JSON in browser code.

## 5. Firestore rules
The included `firestore.rules` is a secure starter:
- Admin custom claim can manage all documents.
- A signed-in user can read their own `users/{uid}` record.
- A signed-in user can create their own user record, but cannot self-assign the admin role.
- Users can create/read their own `serviceRequests` documents; only admins can update them.
- Everything else is denied unless the caller has the admin claim.

**Important:** Before publishing these rules, compare them with the Android app's actual document paths and fields. If your app writes to another collection name or uses a different ownership field than `userId`, adjust the rules and the web app together. Do not deploy rules blindly because they can block existing app features.

## 6. Build and deploy
Install Firebase CLI if needed:
```bash
npm install -g firebase-tools
firebase login
```
Then from this project folder:
```bash
npm run build
firebase use cityseva-dhule
firebase deploy --only hosting
```
Deploy Firestore rules separately only after checking compatibility:
```bash
firebase deploy --only firestore:rules
```

## Data mapping note
The exact customer/retailer/request schemas in the Android app have not yet been confirmed. The dashboard uses the field aliases listed above. If the live data uses different names or nested collections, update `src/App.jsx` after checking the Android source. The admin website intentionally does not create fake sample customers or retailers.
