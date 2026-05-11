export const ROUTES = {
  home: '/',
  login: '/login',
  signup: '/signup',
  courses: '/courses',
  course: (courseId) => `/courses/${courseId}`,
  courseLesson: (courseId, moduleIndex, lessonIndex) =>
    `/courses/${courseId}/module/${moduleIndex}/lesson/${lessonIndex}`,
};
