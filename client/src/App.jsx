import { BrowserRouter, Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout.jsx';
import { AppProvider } from './context/AppContext.jsx';
import CourseDetailPage from './pages/CourseDetailPage.jsx';
import HomePage from './pages/HomePage.jsx';
import LessonDetailPage from './pages/LessonDetailPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <AppProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="courses/:courseId" element={<CourseDetailPage />} />
            <Route path="lessons/:lessonId" element={<LessonDetailPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}
