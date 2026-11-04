// Typed API client generated from the OpenAPI contract, with the access token, the server address and French errors.
import createClient, { type Middleware } from 'openapi-fetch'
import type { components, paths } from './schema'
import { serverUrl, token } from '../app/connection.ts'

export type Schemas = components['schemas']

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/** Translates generic or technical details into something a family member understands. */
export function errorMessage(status: number, detail?: string | null): string {
  if (detail && !/^(Invalid request content|Failed to read request|Validation failed)/.test(detail)) return detail
  switch (status) {
    case 0: return 'Serveur injoignable : vérifiez la connexion ou l’adresse du serveur.'
    case 400: return 'Données invalides : vérifiez le formulaire.'
    case 401: return 'Session expirée : reconnectez-vous.'
    case 403: return "Vous n'avez pas accès à cette fonction."
    case 404: return 'Élément introuvable.'
    case 409: return 'Opération impossible dans l’état actuel.'
    case 429: return 'Trop de tentatives, réessayez plus tard.'
    default: return 'Erreur du serveur, réessayez plus tard.'
  }
}

type UnauthorizedListener = () => void
const unauthorizedListeners = new Set<UnauthorizedListener>()

/** Called on every 401 outside login, so the app can return to the login page. */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener)
  return () => unauthorizedListeners.delete(listener)
}

const authentication: Middleware = {
  onRequest({ request }) {
    const current = token()
    if (current) request.headers.set('Authorization', `Bearer ${current}`)
    return request
  },
  onResponse({ request, response }) {
    if (response.status === 401 && !new URL(request.url).pathname.startsWith('/api/auth/')) {
      unauthorizedListeners.forEach((listener) => listener())
    }
    return response
  },
}

function build() {
  const created = createClient<paths>({ baseUrl: serverUrl() })
  created.use(authentication)
  return created
}

let client = build()

export function api() {
  return client
}

/** Rebuilds the client after the server address changed. */
export function reconnect(): void {
  client = build()
}

interface Result<D> {
  data?: D
  error?: unknown
  response: Response
}

/** Data of a successful call; throws an {@link ApiError} with a French message otherwise. */
export async function unwrap<D>(call: Promise<Result<D>>): Promise<D> {
  let result: Result<D>
  try {
    result = await call
  } catch {
    throw new ApiError(0, errorMessage(0))
  }
  if (result.error !== undefined || !result.response.ok) {
    const detail = typeof result.error === 'object' && result.error !== null && 'detail' in result.error
      ? String((result.error as { detail: unknown }).detail) : null
    throw new ApiError(result.response.status, errorMessage(result.response.status, detail))
  }
  return result.data as D
}
