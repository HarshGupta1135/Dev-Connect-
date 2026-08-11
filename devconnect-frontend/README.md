# DevConnect — frontend

React + Vite client for the DevConnect Spring Boot API. Lives outside the backend
repository on purpose, so the two can be deployed and versioned independently.

## Run it

The backend must be running on `http://localhost:8080` first.

```bash
npm install
npm run dev      # http://localhost:3000
```

```bash
npm run build     # production bundle in dist/
npm run preview   # serve the built bundle locally
```

## Why there is no CORS setup in the backend

The dev server proxies `/api`, `/admin` and `/health` to `http://localhost:8080`
(see `vite.config.js`). The browser only ever talks to `localhost:3000`, and Vite
forwards the request server-side, where the same-origin policy does not apply — so
no backend change is needed for local development.

That changes when the frontend is deployed. Set `VITE_API_BASE_URL` to the backend
origin and the browser starts making genuine cross-origin requests, which the
backend must then allow:

```java
// SecurityConfig
http.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://your-app.vercel.app"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    return config;
}));
```

## Structure

```
src/
  api/client.js        axios instance, JWT interceptor, error normalisation
  api/endpoints.js     one function per backend endpoint
  context/             AuthContext (token + roles), ThemeContext (light/dark)
  components/          ProtectedRoute, Navbar, JobCard, SkillPicker, Modal, …
  pages/               one file per route
  styles/global.css    design tokens and every component style
  utils/               JWT decoding, date and text formatting
```

## Routes

| Route | Access | Purpose |
| --- | --- | --- |
| `/` | public | Landing page with search and latest roles |
| `/jobs` | public | Filterable, paginated job list (match scores when signed in as a developer) |
| `/jobs/:id` | public | Full role detail and the apply flow |
| `/login`, `/register` | public | Authentication |
| `/developer/dashboard` | DEVELOPER | Applications with status, profile summary, resume |
| `/developer/profile` | DEVELOPER | Create or edit profile, upload resume |
| `/recruiter/dashboard` | RECRUITER | Posted jobs, applicant counts, company profile |
| `/recruiter/jobs/new` | RECRUITER | Post a role |
| `/recruiter/jobs/:jobId/applicants` | RECRUITER | Review applicants, shortlist or pass |

## Notes on the current API

Things the UI works around rather than pretends about:

- **The skills catalogue requires authentication.** `GET /api/get/all/skills` is not
  public, so for signed-out visitors the skill filter accepts free text instead of
  offering suggestions.
- **Applications do not identify the candidate.** `ApplicationResponse` carries the
  application id, job title, status, cover note and dates — no developer name or id —
  so the recruiter's applicant list shows an application reference. Adding candidate
  details to that response is a backend change.
- **"Already applied" is matched on job title,** because the applications list returns
  `jobTitle` rather than `jobId`. A duplicate attempt is still refused by the API, and
  that response is handled.
- **Sessions expire after 10 hours.** The token's `exp` claim is read client-side and
  the session is cleared exactly when it lapses, instead of firing requests that the
  backend would reject.
- **Several business errors arrive as HTTP 500** with the real reason in the message,
  prefixed by `An unexpected error occurred:`. `errorMessage()` strips that prefix so
  users read the actual problem.

## Deploying to Vercel

1. Set `VITE_API_BASE_URL` to the deployed backend origin.
2. Add that Vercel origin to the backend's CORS configuration.
3. Build command `npm run build`, output directory `dist`.
4. Add a rewrite so client-side routes resolve on refresh:

```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```
