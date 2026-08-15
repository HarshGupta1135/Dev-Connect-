import { Suspense, lazy } from 'react';
import { Route, Routes } from 'react-router-dom';
import { LazyMotion, MotionConfig } from 'motion/react';
import { Toaster } from 'react-hot-toast';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import Ambient from './components/Ambient';
import CommandPalette from './components/CommandPalette';
import RouteProgress from './components/RouteProgress';
import { BackToTopButton, ScrollToTopOnNavigate } from './components/ScrollHelpers';
import { Skeleton } from './components/Skeleton';
import { useTheme } from './context/ThemeContext';
import Landing from './pages/Landing';

/*
 * Motion's animation engine loads as its own async chunk — via the intermediate
 * module, never by importing 'motion/react' dynamically here (see that file for
 * why that welds the engine into the main bundle). `strict` guarantees nothing
 * can pull the full-fat motion.* components in by accident.
 */
const loadMotionFeatures = () => import('./lib/motion-features').then((mod) => mod.default);

/*
 * Every page except the landing loads on demand. The landing stays in the main
 * chunk because it IS the first paint; the rest ship as their own files, so a
 * first-time visitor downloads the shell and one page instead of ten. Chunks are
 * content-hashed and cached, so a route is fetched at most once per deploy.
 */
const Login = lazy(() => import('./pages/Login'));
const Register = lazy(() => import('./pages/Register'));
const Jobs = lazy(() => import('./pages/Jobs'));
const JobDetail = lazy(() => import('./pages/JobDetail'));
const DeveloperDashboard = lazy(() => import('./pages/DeveloperDashboard'));
const DeveloperProfile = lazy(() => import('./pages/DeveloperProfile'));
const RecruiterDashboard = lazy(() => import('./pages/RecruiterDashboard'));
const RecruiterJobNew = lazy(() => import('./pages/RecruiterJobNew'));
const RecruiterApplicants = lazy(() => import('./pages/RecruiterApplicants'));
const NotFound = lazy(() => import('./pages/NotFound'));

/** Shown for the instant a route chunk is in flight — skeleton, not spinner. */
function PageFallback() {
  return (
    <div className="wrap section--tight" style={{ paddingTop: 34 }} aria-busy="true">
      <div className="stack" style={{ gap: 14 }}>
        <Skeleton width="30%" height={12} />
        <Skeleton width="55%" height={32} />
        <Skeleton height={220} radius={16} style={{ marginTop: 14 }} />
      </div>
    </div>
  );
}

export default function App() {
  const { resolved } = useTheme();

  return (
    <LazyMotion features={loadMotionFeatures} strict>
    {/* reducedMotion="user" turns every spring into an instant state change for
        visitors who asked for less motion — one switch instead of per-component checks. */}
    <MotionConfig reducedMotion="user">
      {/* Outside .shell so the drifting colour field sits behind everything. */}
      <Ambient />

      <div className="shell">
      <RouteProgress />
      <ScrollToTopOnNavigate />
      <Navbar />

      <main className="page">
        <Suspense fallback={<PageFallback />}>
        <Routes>
          {/* public */}
          <Route path="/" element={<Landing />} />
          <Route path="/jobs" element={<Jobs />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* developer */}
          <Route
            path="/developer/dashboard"
            element={
              <ProtectedRoute roles={['DEVELOPER']}>
                <DeveloperDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/developer/profile"
            element={
              <ProtectedRoute roles={['DEVELOPER']}>
                <DeveloperProfile />
              </ProtectedRoute>
            }
          />

          {/* recruiter */}
          <Route
            path="/recruiter/dashboard"
            element={
              <ProtectedRoute roles={['RECRUITER']}>
                <RecruiterDashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/recruiter/jobs/new"
            element={
              <ProtectedRoute roles={['RECRUITER']}>
                <RecruiterJobNew />
              </ProtectedRoute>
            }
          />
          <Route
            path="/recruiter/jobs/:jobId/applicants"
            element={
              <ProtectedRoute roles={['RECRUITER']}>
                <RecruiterApplicants />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<NotFound />} />
        </Routes>
        </Suspense>
      </main>

      <Footer />
      <BackToTopButton />
      {/* Owns its own Ctrl/⌘K and "/" listeners, so it works from any route. */}
      <CommandPalette />

      <Toaster
        position="bottom-right"
        toastOptions={{
          duration: 4200,
          style: {
            background: resolved === 'dark' ? '#181d28' : '#ffffff',
            color: resolved === 'dark' ? '#e8ecf3' : '#101521',
            border: `1px solid ${resolved === 'dark' ? '#262e3a' : '#dde2ea'}`,
            borderRadius: '10px',
            fontSize: '0.9rem',
            fontFamily: "'Plus Jakarta Sans', system-ui, sans-serif",
            boxShadow: '0 12px 28px -12px rgba(15, 23, 42, 0.35)',
            maxWidth: '420px',
          },
          success: { iconTheme: { primary: '#4f46e5', secondary: '#ffffff' } },
          error: { iconTheme: { primary: '#be123c', secondary: '#ffffff' } },
        }}
      />
      </div>
    </MotionConfig>
    </LazyMotion>
  );
}
