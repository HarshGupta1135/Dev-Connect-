import { Route, Routes } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ProtectedRoute from './components/ProtectedRoute';
import { BackToTopButton, ScrollToTopOnNavigate } from './components/ScrollHelpers';
import { useTheme } from './context/ThemeContext';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Jobs from './pages/Jobs';
import JobDetail from './pages/JobDetail';
import DeveloperDashboard from './pages/DeveloperDashboard';
import DeveloperProfile from './pages/DeveloperProfile';
import RecruiterDashboard from './pages/RecruiterDashboard';
import RecruiterJobNew from './pages/RecruiterJobNew';
import RecruiterApplicants from './pages/RecruiterApplicants';
import NotFound from './pages/NotFound';

export default function App() {
  const { resolved } = useTheme();

  return (
    <div className="shell">
      <ScrollToTopOnNavigate />
      <Navbar />

      <main className="page">
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
      </main>

      <Footer />
      <BackToTopButton />

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
  );
}
