import { useCallback } from 'react';
import { useAuth } from './useAuth.js';
import { apiRequest } from '../utils/apiClient.js';

export function useApiClient() {
  const { getAccessTokenSilently, isAuthenticated } = useAuth();

  return useCallback(
    async (path, options = {}) => {
      if (!isAuthenticated) {
        return apiRequest(path, options);
      }

      const accessToken = await getAccessTokenSilently();
      return apiRequest(path, {
        ...options,
        accessToken,
      });
    },
    [getAccessTokenSilently, isAuthenticated],
  );
}
