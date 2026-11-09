# Demo recording script

1. Show the login and registration screens; register a customer.
2. Create an INR account and show the balance.
3. Make a clearly labelled simulated deposit.
4. Transfer funds to a second account and show the transaction history.
5. Replay the same request with the same idempotency key and show one result; change the payload and show `409`.
6. Attempt concurrent transfers exceeding the balance and show that only affordable transfers succeed.
7. Log in as an auditor and demonstrate read-only account, transfer and audit views.
8. Log in as an administrator and demonstrate user activation and role management.
9. Trigger the configured rate limit and show `429`.
10. Show liveness/readiness probes and the Compose service topology.

No video has been recorded or uploaded; these steps remain the manual submission checklist.
