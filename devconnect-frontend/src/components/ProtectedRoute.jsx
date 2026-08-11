import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Gate for authenticated pages.
 *
 * @param {string[]} [roles] when given, the user must hold one of these roles;
 *   a signed-in user with the wrong role is sent to their own dashboard rather
 *   than to the login page, which would look like a failed sign-in.
 */
export default function ProtectedRoute({ children, roles }) {
  const { isAuthenticated, roles: userRoles, homeFor } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // Remember where they were headed so login can return them there.
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles?.length && !roles.some((role) => userRoles.includes(role))) {
    return <Navigate to={homeFor} replace />;
  }

  return children;
}
