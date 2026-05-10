export const ROUTES = {
  home: '/',
  login: '/login',
  signup: '/signup',
  course: (courseId) => `/courses/${courseId}`,
  lesson: (lessonId) => `/lessons/${lessonId}`,
};
