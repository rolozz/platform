export const KEYCLOAK_BASE_URL = (import.meta.env.VITE_KEYCLOAK_URL as string | undefined) ?? 'http://localhost:8095'
export const KEYCLOAK_REALM = (import.meta.env.VITE_KEYCLOAK_REALM as string | undefined) ?? 'my-realm'
export const KEYCLOAK_CLIENT_ID = (import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string | undefined) ?? 'react-frontend'

export const GATEWAY_BASE_URL = (import.meta.env.VITE_GATEWAY_URL as string | undefined) ?? 'http://localhost:8082'
