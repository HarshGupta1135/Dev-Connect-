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

That no longer needs a code change. The backend reads an allowlist from an environment
variable and emits no CORS headers while it is empty, which is what keeps local
development unaffected:

```
CORS_ALLOWED_ORIGINS=https://your-app.vercel.app
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
- **Sessions expire after 10 hours.** The token's `exp` claim is read client-side and
  the session is cleared exactly when it lapses, instead of firing requests that the
  backend would reject.
- **Some business errors still arrive as HTTP 500** with the real reason in the message,
  prefixed by `An unexpected error occurred:`. `errorMessage()` strips that prefix so
  users read the actual problem. Conflicts and validation failures on the account
  endpoints answer 409 and 400 properly; the rest still fall to the catch-all.

## Deploying to Vercel

This directory lives inside the backend repository, so set Vercel's **Root Directory**
to `devconnect-frontend`. Everything else it detects on its own — Vite framework preset,
`npm run build`, output in `dist`.

One environment variable:

```
VITE_API_BASE_URL=https://your-backend.onrender.com
```

The name matters. Vite only exposes variables prefixed `VITE_` to client code; a
`REACT_APP_`-prefixed name is Create React App's convention and would be silently
ignored here, leaving `baseURL` empty and every request pointed at the Vercel domain
itself.

It is also read at **build** time, not run time — `import.meta.env.VITE_API_BASE_URL` is
substituted into the bundle by the bundler. Changing it in Vercel therefore requires a
redeploy; there is no live value to update.

`vercel.json` holds the rewrite that makes client-side routes survive a refresh:

```json
{ "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
```

Without it, only `/` works. Vercel looks for a file at `/jobs` on a direct hit or a
refresh, finds none, and answers 404 — every route except the root, and only when the
router is not already loaded, which is why it survives in-app navigation and breaks on
shared links.

Then set `CORS_ALLOWED_ORIGINS` on the backend to the Vercel origin, and redeploy the
backend. The two point at each other, so it takes two passes.
