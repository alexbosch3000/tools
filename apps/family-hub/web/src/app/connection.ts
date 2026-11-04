// Where the API is and who calls it: the server address (UI served by GitHub Pages) and the access token.

/** Built for GitHub Pages: the UI talks to a family-hub server whose address the family enters once. */
export const remoteMode = import.meta.env.VITE_REMOTE_API === 'true'

const SERVER_KEY = 'familyhub.server'
const TOKEN_KEY = 'familyhub.token'

function read(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key)
    else localStorage.setItem(key, value)
  } catch {
    // Private browsing or blocked storage: the session simply does not survive a reload.
  }
}

/** Origin of the API: empty when the UI is served by the service itself. */
export function serverUrl(): string {
  return remoteMode ? (read(SERVER_KEY) ?? '') : ''
}

/** Normalises "famille.example.fr" or "https://famille.example.fr/" to "https://famille.example.fr". */
export function normalizeServerUrl(input: string): string {
  const trimmed = input.trim().replace(/\/+$/, '')
  return /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`
}

export function setServerUrl(url: string | null): void {
  write(SERVER_KEY, url)
}

export function token(): string | null {
  return read(TOKEN_KEY)
}

export function setToken(value: string | null): void {
  write(TOKEN_KEY, value)
}

/** Public address of the API, for links handed to other applications (calendar feed). */
export function publicApiOrigin(): string {
  return serverUrl() || window.location.origin
}
