# AK General Store Backend Production Deployment

## Profiles

- `local`: local development with `.env.local`
- `prod`: hardened production profile with startup validation

## Local start

```powershell
.\start-backend.ps1
```

## Production requirements

Copy `.env.prod.example` to your secure deployment environment and set real values for:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `BREVO_API_KEY` or SMTP credentials
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `OTP_FROM_EMAIL`
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- `PAYMENT_RAZORPAY_KEY_ID`
- `PAYMENT_RAZORPAY_KEY_SECRET`

## Production behavior

- app fails fast if required production secrets are missing
- CORS is driven by `APP_CORS_ALLOWED_ORIGIN_PATTERNS`
- OTP email delivery uses `BREVO_API_KEY` first when present, otherwise SMTP credentials
- JPA `ddl-auto` defaults to `validate` in production
- security headers are enabled on the API

## Recommended ops steps

- run behind HTTPS reverse proxy
- enable daily MySQL backups
- rotate SMTP and JWT secrets periodically
- restrict database access by IP/VPC
- monitor `/api/products` and auth endpoints for uptime
- keep upload storage on persistent disk or object storage
