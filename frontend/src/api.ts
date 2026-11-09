export type TokenResponse = {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string
  refreshTokenExpiresAt: string
}

export type Account = {
  id: string
  accountNumber: string
  balance: number
  currency: string
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  createdAt: string
}

export type Transfer = {
  reference: string
  senderAccountNumber: string
  recipientAccountNumber: string
  amount: number
  currency: string
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED'
  createdAt: string
}

export type AuditLog = {
  actorEmail: string | null
  action: string
  resourceType: string
  resourceId: string
  createdAt: string
  details: string
}

export type User = {
  id: string
  fullName: string
  email: string
  roles: string[]
  createdAt: string
}

const API_BASE = import.meta.env.VITE_API_URL ?? ''

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(response.status, body?.message ?? 'Request failed')
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  login: (email: string, password: string) =>
    request<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  register: (fullName: string, email: string, password: string) =>
    request<TokenResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password }),
    }),
  refresh: (refreshToken: string) =>
    request<TokenResponse>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
  logout: (refreshToken: string) =>
    request<void>('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
  accounts: (token: string) => request<Account[]>('/api/accounts', {}, token),
  createAccount: (token: string) =>
    request<Account>('/api/accounts', { method: 'POST' }, token),
  changeAccountStatus: (token: string, accountNumber: string, status: Account['status']) =>
    request<Account>(`/api/accounts/${accountNumber}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }, token),
  transfers: (token: string) => request<Transfer[]>('/api/transfers', {}, token),
  transfer: (
    token: string,
    body: {
      senderAccountNumber: string
      recipientAccountNumber: string
      amount: number
    },
  ) =>
    request<Transfer>(
      '/api/transfers',
      {
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
        body: JSON.stringify(body),
      },
      token,
    ),
  auditLogs: (token: string) => request<AuditLog[]>('/api/audit-logs', {}, token),
  users: (token: string) => request<User[]>('/api/admin/users', {}, token),
  assignRole: (token: string, email: string, role: string) =>
    request<User>(`/api/admin/users/${encodeURIComponent(email)}/roles`, {
      method: 'POST',
      body: JSON.stringify({ role }),
    }, token),
}
