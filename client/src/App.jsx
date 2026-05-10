import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './components/auth/ProtectedRoute.jsx';
import AppLayout from './components/layout/AppLayout.jsx';
import { AppProvider } from './context/AppContext.jsx';
import CourseDetailPage from './pages/CourseDetailPage.jsx';
import HomePage from './pages/HomePage.jsx';
import LessonDetailPage from './pages/LessonDetailPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import Auth0ProviderWithNavigate from './services/auth/Auth0ProviderWithNavigate.jsx';
import { ROUTES } from './utils/routes.js';

export default function App() {
  return (
    <BrowserRouter>
      <Auth0ProviderWithNavigate>
        <AppProvider>
          <Routes>
            <Route path="/" element={<AppLayout />}>
              <Route index element={<HomePage />} />
              <Route path="login" element={<LoginPage />} />
              <Route path="signup" element={<SignupPage />} />
              <Route
                path="courses/:courseId"
                element={
                  <ProtectedRoute>
                    <CourseDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="courses/:courseId/module/:moduleIndex/lesson/:lessonIndex"
                element={
                  <ProtectedRoute>
                    <LessonDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="lessons/:lessonId"
                element={<Navigate to={ROUTES.courseLesson('demo-course', 0, 0)} replace />}
              />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </AppProvider>
      </Auth0ProviderWithNavigate>
    </BrowserRouter>
  );
}
