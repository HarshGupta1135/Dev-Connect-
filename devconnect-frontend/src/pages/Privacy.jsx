import { Link } from 'react-router-dom';

const CONTACT_URL = 'https://github.com/HarshGupta1135/Dev-Connect-/issues';

/**
 * Privacy policy. Also the URL given to Google's OAuth consent screen, which needs
 * one before the app can leave "Testing". Keep it in step with what the app stores.
 */
export default function Privacy() {
  return (
    <div className="wrap section page-enter" style={{ maxWidth: 760 }}>
      <div className="stack" style={{ gap: 8, marginBottom: 28 }}>
        <span className="eyebrow">Legal</span>
        <h1 style={{ fontSize: 'clamp(2rem, 4vw, 2.7rem)' }}>Privacy policy</h1>
        <p className="lede">What DevConnect stores about you, why, and who else handles it.</p>
        <p className="small muted">Last updated: 16 November 2026</p>
      </div>

      <div className="card card--pad stack" style={{ gap: 22, lineHeight: 1.65 }}>
        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>What we collect</h2>
          <ul style={{ paddingLeft: 20 }} className="stack">
            <li>
              <strong>Account:</strong> username, email address, your role (developer or recruiter), and a
              securely hashed password if you register with one.
            </li>
            <li>
              <strong>Google or GitHub sign-in:</strong> only your name (or GitHub username) and your verified
              email address, used to create or find your account. We do not store your Google or GitHub
              password or access tokens, and we do not access anything else in those accounts.
            </li>
            <li>
              <strong>Developer profile (optional):</strong> full name, bio, location, years of experience,
              skills, LinkedIn URL, phone number, address, city, pincode and an uploaded resume.
            </li>
            <li>
              <strong>Recruiter profile (optional):</strong> full name, company name and description, website
              and location.
            </li>
            <li>
              <strong>Activity:</strong> jobs you post, applications you make, cover notes and application
              status.
            </li>
          </ul>
        </section>

        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>How we use it</h2>
          <p>
            To run the service: signing you in, matching developers to jobs by skills, letting recruiters
            review applicants for their own job postings, and sending account and application emails (for
            example a welcome email or an application status update). When you apply to a job, the recruiter
            who posted it can see your profile, resume and cover note.
          </p>
          <p>We do not sell your data, show ads, or use tracking or analytics cookies.</p>
        </section>

        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>Services that process your data</h2>
          <p>
            DevConnect runs on third-party infrastructure: Vercel (website), Render (API), a hosted MySQL
            database and a Redis cache, Cloudinary (resume and file storage), and Brevo or Gmail (sending
            email). Google and GitHub handle sign-in when you choose them. Each processes data only as
            needed to provide its service.
          </p>
        </section>

        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>Stored in your browser</h2>
          <p>
            Your sign-in token, theme preference, saved jobs and recently viewed jobs are kept in your
            browser&apos;s local storage. Signing out removes the sign-in token.
          </p>
        </section>

        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>Your choices</h2>
          <p>
            You can view and edit your account and profile at any time after signing in. To have your
            account and data deleted, contact us using the link below and we will remove it.
          </p>
        </section>

        <section className="stack" style={{ gap: 8 }}>
          <h2 style={{ fontSize: '1.2rem' }}>Contact</h2>
          <p>
            Questions or deletion requests:{' '}
            <a href={CONTACT_URL} target="_blank" rel="noreferrer" style={{ color: 'var(--accent)', fontWeight: 600 }}>
              open an issue on the DevConnect GitHub repository
            </a>
            .
          </p>
        </section>

        <p className="small muted">
          <Link to="/" style={{ color: 'var(--accent)' }}>Back to DevConnect</Link>
        </p>
      </div>
    </div>
  );
}
