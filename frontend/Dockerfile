# Baut die SPA und liefert sie mit nginx ohne Root-Rechte aus.
# Umgebungsabhängige Werte (Keycloak) kommen zur Laufzeit über Umgebungsvariablen, siehe nginx/default.conf.template.

FROM node:24-alpine AS build
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.31-alpine
COPY nginx/templates /etc/nginx/templates
COPY --from=build /app/dist /usr/share/nginx/html
ENV OIDC_AUTHORITY=http://localhost:8081/realms/debitoren \
    OIDC_CLIENT_ID=debitoren-spa \
    OIDC_ORIGIN=http://localhost:8081
EXPOSE 8080
