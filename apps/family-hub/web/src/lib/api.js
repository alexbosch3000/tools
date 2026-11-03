// HTTP client of the API: JSON in and out, session cookie, CSRF header, readable French errors.

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

let unauthorizedHandler = null

/** Called on every 401, so the application can go back to the login page. */
export function onUnauthorized(handler) {
  unauthorizedHandler = handler
}

function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

/** Translates the generic English details of Spring into something a family member understands. */
export function errorMessage(status, detail) {
  if (!detail || /^(Invalid request content|Failed to read request)/.test(detail)) {
    if (status === 400) return 'Données invalides : vérifiez le formulaire.'
    if (status === 403) return "Vous n'avez pas accès à cette fonction."
    if (status === 404) return 'Élément introuvable.'
    if (status === 409) return 'Opération impossible dans l’état actuel.'
    return 'Erreur du serveur, réessayez plus tard.'
  }
  return detail
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    const token = csrfToken()
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (response.status === 401 && unauthorizedHandler && !path.startsWith('/api/auth/')) unauthorizedHandler()
  if (!response.ok) {
    let detail = null
    try { detail = (await response.json()).detail } catch { /* not a problem response */ }
    throw new ApiError(response.status, errorMessage(response.status, detail))
  }
  if (response.status === 204) return null
  const type = response.headers.get('Content-Type') || ''
  return type.includes('json') ? response.json() : response.text()
}

export const get = (path) => api(path)
export const post = (path, body = {}) => api(path, { method: 'POST', body })
export const put = (path, body = {}) => api(path, { method: 'PUT', body })
export const del = (path) => api(path, { method: 'DELETE' })
