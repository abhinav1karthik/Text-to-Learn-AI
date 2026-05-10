import { createContext, useMemo, useState } from 'react';

export const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [isGlobalLoading, setGlobalLoading] = useState(false);
  const [globalError, setGlobalError] = useState(null);

  const value = useMemo(
    () => ({
      currentUser,
      globalError,
      isGlobalLoading,
      setGlobalError,
      setGlobalLoading,
      setCurrentUser,
    }),
    [currentUser, globalError, isGlobalLoading],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
