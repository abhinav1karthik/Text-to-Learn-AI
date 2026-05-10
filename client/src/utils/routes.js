export const ROUTES = {
  home: '/',
  login: '/login',
  signup: '/signup',
  course: (courseId) => `/courses/${courseId}`,
  courseLesson: (courseId, moduleIndex, lessonIndex) =>
    `/courses/${courseId}/module/${moduleIndex}/lesson/${lessonIndex}`,
  lesson: (lessonId) => `/lessons/${lessonId}`,
};
