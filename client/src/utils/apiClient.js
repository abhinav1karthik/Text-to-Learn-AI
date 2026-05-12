const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export async function apiRequest(path, options = {}) {
  const { accessToken, headers, responseType = 'json', ...requestOptions } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...headers,
    },
    ...requestOptions,
  });

  if (!response.ok) {
    const errorBody = await readErrorBody(response);
    throw new Error(errorBody?.message ?? `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  if (responseType === 'blob') {
    return response.blob();
  }

  return response.json();
}

async function readErrorBody(response) {
  const responseClone = response.clone();
  try {
    return await response.json();
  } catch {
    try {
      const message = await responseClone.text();
      return message ? { message } : null;
    } catch {
      return null;
    }
  }
}
