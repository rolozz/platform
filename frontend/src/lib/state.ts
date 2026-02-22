export type AsyncState<T> = {
  loading: boolean
  error: string | null
  data: T | null
}

export function idleState<T>(): AsyncState<T> {
  return { loading: false, error: null, data: null }
}

export function loadingState<T>(): AsyncState<T> {
  return { loading: true, error: null, data: null }
}

export function successState<T>(data: T): AsyncState<T> {
  return { loading: false, error: null, data }
}

export function errorState<T>(message: string): AsyncState<T> {
  return { loading: false, error: message, data: null }
}
