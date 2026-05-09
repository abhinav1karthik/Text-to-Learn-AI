const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const errorBody = await readErrorBody(response);
    throw new Error(errorBody?.message ?? `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function readErrorBody(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
