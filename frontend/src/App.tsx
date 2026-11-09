import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'
import { api } from './api'
import type { Account, AuditLog, TokenResponse, Transfer, User } from './api'

type Session = TokenResponse & { roles: string[] }

function readSession(): Session | null {
  const raw = localStorage.getItem('ledgerline.session')
  return raw ? JSON.parse(raw) as Session : null
}

function decodeRoles(token: string): string[] {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.roles ?? []
  } catch {
    return []
  }
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
  }).format(value)
}

function App() {
  const [session, setSession] = useState<Session | null>(readSession)

  const saveSession = (tokens: TokenResponse) => {
    const next = { ...tokens, roles: decodeRoles(tokens.accessToken) }
    localStorage.setItem('ledgerline.session', JSON.stringify(next))
    setSession(next)
  }

  if (!session) return <AuthScreen onAuthenticated={saveSession} />
  return <Dashboard session={session} onLogout={() => {
    void api.logout(session.refreshToken).catch(() => undefined)
    localStorage.removeItem('ledgerline.session')
    setSession(null)
  }} />
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (tokens: TokenResponse) => void }) {
  const [registering, setRegistering] = useState(false)
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const tokens = registering
        ? await api.register(fullName, email, password)
        : await api.login(email, password)
      onAuthenticated(tokens)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to authenticate')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <div className="brand-mark">LL</div>
        <p className="eyebrow">SECURE LEDGER PLATFORM</p>
        <h1>{registering ? 'Open your account' : 'Welcome back'}</h1>
        <p className="muted">{registering ? 'Start with a secure INR account.' : 'Sign in to manage your LedgerLine accounts.'}</p>
        <form onSubmit={submit}>
          {registering && <label>Full name<input required value={fullName} onChange={(event) => setFullName(event.target.value)} /></label>}
          <label>Email<input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} /></label>
          <label>Password<input type="password" minLength={12} required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          {error && <p className="error">{error}</p>}
          <button disabled={busy}>{busy ? 'Please wait…' : registering ? 'Create account' : 'Sign in'}</button>
        </form>
        <button className="link-button" onClick={() => setRegistering(!registering)}>
          {registering ? 'Already registered? Sign in' : 'New to LedgerLine? Register'}
        </button>
      </section>
    </main>
  )
}

function Dashboard({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [recipient, setRecipient] = useState('')
  const [sender, setSender] = useState('')
  const [amount, setAmount] = useState('')

  const canAudit = session.roles.some((role) => ['ADMIN', 'AUDITOR'].includes(role))
  const canAdmin = session.roles.includes('ADMIN')
  const totalBalance = useMemo(() => accounts.reduce((total, account) => total + Number(account.balance), 0), [accounts])

  const load = useCallback(async () => {
    try {
      const [nextAccounts, nextTransfers] = await Promise.all([
        api.accounts(session.accessToken),
        api.transfers(session.accessToken),
      ])
      setAccounts(nextAccounts)
      setTransfers(nextTransfers)
      if (canAudit) setAuditLogs(await api.auditLogs(session.accessToken))
      if (canAdmin) setUsers(await api.users(session.accessToken))
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to load dashboard')
    }
  }, [canAdmin, canAudit, session.accessToken])

  useEffect(() => { void load() }, [load])

  async function createAccount() {
    try {
      const account = await api.createAccount(session.accessToken)
      setAccounts((current) => [...current, account])
      setNotice(`Account ${account.accountNumber} created`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to create account')
    }
  }

  async function sendTransfer(event: FormEvent) {
    event.preventDefault()
    try {
      await api.transfer(session.accessToken, {
        senderAccountNumber: sender,
        recipientAccountNumber: recipient,
        amount: Number(amount),
      })
      setRecipient('')
      setAmount('')
      setNotice('Transfer completed successfully')
      await load()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Transfer failed')
    }

  }

  async function changeStatus(accountNumber: string, status: Account['status']) {
    try {
      const updated = await api.changeAccountStatus(session.accessToken, accountNumber, status)
      setAccounts((current) => current.map((account) => account.id === updated.id ? updated : account))
      setNotice(`Account ${accountNumber} is now ${status.toLowerCase()}`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to update account status')
    }
  }

  async function assignRole(email: string, role: string) {
    try {
      const updated = await api.assignRole(session.accessToken, email, role)
      setUsers((current) => current.map((user) => user.id === updated.id ? updated : user))
      setNotice(`${role} role assigned to ${email}`)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to assign role')
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand"><span className="brand-mark small">LL</span><strong>LedgerLine</strong></div>
        <div className="topbar-actions"><span className="role-pill">{session.roles.join(' · ')}</span><button className="ghost-button" onClick={onLogout}>Sign out</button></div>
      </header>
      <main className="dashboard">
        <section className="welcome"><div><p className="eyebrow">ACCOUNT OVERVIEW</p><h1>Your financial command center</h1></div><div className="balance-card"><span>Total balance</span><strong>{formatMoney(totalBalance)}</strong><small>Across {accounts.length} INR account{accounts.length === 1 ? '' : 's'}</small></div></section>
        {error && <div className="alert error">{error}<button onClick={() => setError('')}>×</button></div>}
        {notice && <div className="alert success">{notice}<button onClick={() => setNotice('')}>×</button></div>}
        <section className="grid">
          <div className="panel accounts-panel"><div className="panel-heading"><div><p className="eyebrow">MY ACCOUNTS</p><h2>Accounts</h2></div><button onClick={createAccount} disabled={accounts.length >= 3}>+ New account</button></div><div className="account-list">{accounts.map((account) => <div className={`account-row ${sender === account.accountNumber ? 'selected' : ''}`} key={account.id} onClick={() => setSender(account.accountNumber)}><span className="account-icon">₹</span><span><strong>{account.accountNumber}</strong><small>{account.status} · {account.currency}</small></span><b>{formatMoney(Number(account.balance))}</b>{canAdmin && account.status !== 'CLOSED' && <select aria-label={`Change status for ${account.accountNumber}`} value={account.status} onChange={(event) => void changeStatus(account.accountNumber, event.target.value as Account['status'])}><option value="ACTIVE">Active</option><option value="FROZEN">Frozen</option><option value="CLOSED">Closed</option></select>}</div>)}{accounts.length === 0 && <p className="muted">Your first account will appear here.</p>}</div></div>
          <div className="panel transfer-panel"><p className="eyebrow">MONEY MOVEMENT</p><h2>Send money</h2><form onSubmit={sendTransfer}><label>From<select required value={sender} onChange={(event) => setSender(event.target.value)}><option value="">Select account</option>{accounts.filter((account) => account.status === 'ACTIVE').map((account) => <option key={account.id} value={account.accountNumber}>{account.accountNumber} · {formatMoney(Number(account.balance))}</option>)}</select></label><label>Recipient account number<input required placeholder="LL123456789012" value={recipient} onChange={(event) => setRecipient(event.target.value)} /></label><label>Amount (INR)<input required min="0.01" step="0.01" type="number" placeholder="0.00" value={amount} onChange={(event) => setAmount(event.target.value)} /></label><button type="submit">Review and send</button></form></div>
        </section>
        <section className="panel"><div className="panel-heading"><div><p className="eyebrow">ACTIVITY</p><h2>Transaction history</h2></div><span className="count">{transfers.length} records</span></div>{transfers.length === 0 ? <p className="muted">Completed transfers will appear here.</p> : <div className="table-wrap"><table><thead><tr><th>Reference</th><th>Direction</th><th>Account</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead><tbody>{transfers.map((transfer) => { const sent = accounts.some((account) => account.accountNumber === transfer.senderAccountNumber); return <tr key={transfer.reference}><td className="mono">{transfer.reference.slice(0, 8)}…</td><td>{sent ? 'Sent' : 'Received'}</td><td className="mono">{sent ? transfer.recipientAccountNumber : transfer.senderAccountNumber}</td><td>{formatMoney(Number(transfer.amount))}</td><td><span className={`status ${transfer.status.toLowerCase()}`}>{transfer.status}</span></td><td>{new Date(transfer.createdAt).toLocaleDateString()}</td></tr> })}</tbody></table></div>}</section>
        {canAudit && <section className="panel"><div className="panel-heading"><div><p className="eyebrow">CONTROL TRAIL</p><h2>Audit log</h2></div><span className="count">{auditLogs.length} records</span></div><div className="audit-list">{auditLogs.slice(0, 8).map((log) => <div className="audit-row" key={`${log.resourceId}-${log.createdAt}`}><span className="status-dot" /><span><strong>{log.action.replaceAll('_', ' ')}</strong><small>{log.actorEmail ?? 'System'} · {log.details}</small></span><time>{new Date(log.createdAt).toLocaleString()}</time></div>)}</div></section>}
        {canAdmin && <section className="panel"><div className="panel-heading"><div><p className="eyebrow">ADMINISTRATION</p><h2>User roles</h2></div><span className="count">{users.length} users</span></div><div className="user-list">{users.map((user) => <div className="user-row" key={user.id}><span><strong>{user.fullName}</strong><small>{user.email} · {user.roles.join(', ')}</small></span><select aria-label={`Assign role to ${user.email}`} defaultValue="" onChange={(event) => void assignRole(user.email, event.target.value)}><option value="" disabled>Assign role</option><option value="CUSTOMER">Customer</option><option value="ADMIN">Admin</option><option value="AUDITOR">Auditor</option></select></div>)}</div></section>}
      </main>
    </div>
  )
}

export default App
