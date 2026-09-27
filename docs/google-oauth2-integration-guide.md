# Google OAuth2 integration for Flutter students

## Scope

- Google login is available only to `STUDENT` accounts in the Flutter app.
- `ADMIN` and `TEACHER` accounts are provisioned by an Admin and use email/password login.
- Flutter uses the backend web-server OAuth flow. It does not send a Google ID token from a native Google Sign-In SDK.
- The backend never places an access token or refresh token in a redirect URL.

## Login flow

1. Flutter opens the system browser at:

   ```text
   GET {BACKEND_URL}/oauth2/authorization/google
   ```

2. Google redirects to Spring Security's backend callback:

   ```text
   GET {BACKEND_URL}/login/oauth2/code/google
   ```

3. The backend validates the Google profile and account:

   - the email must be verified;
   - the account must be active;
   - an existing account must have role `STUDENT`;
   - a new Google account is always created as `STUDENT`.

4. The backend stores an opaque, short-lived, single-use exchange code in Redis and redirects to:

   ```text
   englishapp://oauth2/redirect?code={oneTimeCode}
   ```

5. Flutter exchanges the code for application tokens:

   ```http
   POST /api/v1/auth/oauth2/exchange
   Content-Type: application/json

   {"code":"{oneTimeCode}"}
   ```

6. Redis consumes the code atomically. Reusing, racing or submitting an expired code returns `INVALID_OAUTH2_CODE`.

## Authentication response

The login and refresh responses include:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 15,
  "email": "student@example.com",
  "fullName": "Student",
  "avatarUrl": "https://...",
  "role": "STUDENT",
  "provider": "GOOGLE",
  "onboardingCompleted": false
}
```

For `ADMIN` and `TEACHER`, `onboardingCompleted` is `null`. Flutter should call `/api/v1/onboarding/status` only when the authenticated Student has `onboardingCompleted=false`.

The response intentionally has no `nextStep` and no `loginClient` field.

## Staff accounts

An Admin creates a staff account through:

```http
POST /api/v1/admin/users
Authorization: Bearer {adminAccessToken}
```

The account is stored with `provider=LOCAL` and logs in through:

```http
POST /api/v1/auth/login
```

Google OAuth rejects an existing `ADMIN` or `TEACHER` email with `student_role_required`.

## Google Cloud Console

Use an OAuth client of type **Web application**. Register the exact backend callback URI, for example:

```text
https://english-app-backend-fvhdetejdng4aceg.japaneast-01.azurewebsites.net/login/oauth2/code/google
```

For local development:

```text
http://localhost:8080/login/oauth2/code/google
```

The Flutter deep link is not a Google redirect URI. Google redirects to the backend; the backend then redirects to Flutter.

## Backend environment variables

```text
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
OAUTH2_FLUTTER_POST_LOGIN_URI=englishapp://oauth2/redirect
OAUTH2_EXCHANGE_CODE_TTL=2m
OAUTH2_SESSION_TIMEOUT=10m
SESSION_COOKIE_SECURE=true
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...
REDIS_SSL=true
```

`SESSION_COOKIE_SECURE` may be `false` for local HTTP development but must be `true` behind production HTTPS.

## Flutter requirements

- Open the authorization URL with the system browser, Chrome Custom Tabs or `ASWebAuthenticationSession`; do not use an embedded WebView.
- Register the `englishapp` custom scheme on Android and iOS.
- Keep the returned exchange code only long enough to call the exchange endpoint.
- Store access and refresh tokens in Android Keystore/iOS Keychain-backed secure storage.
- Preserve the same login attempt while the system browser is open; do not launch multiple OAuth sessions concurrently.
