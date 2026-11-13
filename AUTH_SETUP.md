# CitySeva Dhule authentication setup (mobile + password)

The Android app and admin website use Firebase Email/Password Authentication with a deterministic email alias for each Indian mobile number. No phone OTP, SMS, or reCAPTCHA flow is used.

## 1. Enable Firebase sign-in

In Firebase Console for project `cityseva-dhule`:

1. Open **Authentication → Sign-in method**.
2. Enable **Email/Password**.
3. Keep **Phone** sign-in disabled if you do not want OTP/SMS in this app.
4. Confirm Firestore is created and the deployed rules match `admin-website/firestore.rules`.

## 2. How mobile login is mapped

A 10-digit mobile number such as `9876543210` maps to the Firebase Auth email alias `919876543210@cityseva.app`. Users enter only their mobile number and password in the app. Do not ask users to enter this alias.

Passwords are handled by Firebase Authentication. The app never writes a plaintext password to Firestore.

## 3. Customer and retailer registration

- Customers can register in the app. Their Firestore profile is created at `users/{firebaseUid}` with `role: customer`, `active: true`, and `status: active`.
- Retailers can register in the app. Their profile starts with `role: retailer`, `active: false`, and `status: pending` until an administrator approves them.
- Admin registration is not available in the public app.

## 4. Create the first admin account

1. In Firebase Console → Authentication → Users, add a user with email `91YOUR10DIGITMOBILENUMBER@cityseva.app` and set a strong password.
2. On a trusted computer, install dependencies in `admin-website` using `npm install`.
3. Create a Firebase service-account key from Project settings → Service accounts. Keep the JSON private; never upload it to GitHub or the app.
4. Set `GOOGLE_APPLICATION_CREDENTIALS` to that private JSON file path.
5. Run `npm run set-admin -- YOUR10DIGITMOBILENUMBER` from `admin-website`.
6. The trusted script assigns the `admin: true` custom claim and creates/updates `users/{uid}` with `role: admin`.
7. Sign in to the Android app or admin website using the same 10-digit mobile number and password. If already signed in, sign out and back in to refresh the claim.

## 5. Password recovery

There is no OTP/SMS password reset. For this current mobile-alias design, a forgotten password must be reset by an administrator using Firebase Authentication Console or a trusted Admin SDK process. Never store or share user passwords in Firestore.

## 6. Build

Push the updated source to the repository's `main` branch or run the **CitySeva APK Builder** workflow manually in GitHub Actions. Confirm the workflow succeeds before installing the new APK.
