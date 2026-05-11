import { createContext, useMemo, useState } from 'react';

export const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [isGlobalLoading, setGlobalLoading] = useState(false);
  const [globalError, setGlobalError] = useState(null);
  const [sidebarContent, setSidebarContent] = useState(null);

  const value = useMemo(
    () => ({
      currentUser,
      globalError,
      isGlobalLoading,
      sidebarContent,
      setGlobalError,
      setGlobalLoading,
      setCurrentUser,
      setSidebarContent,
    }),
    [currentUser, globalError, isGlobalLoading, sidebarContent],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
