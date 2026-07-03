# syntax=docker/dockerfile:1.7

FROM node:24-alpine AS build
WORKDIR /app
RUN npm install -g npm@11.12.1
COPY frontend/.npmrc frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npx ng build --configuration docker

# nginx-unprivileged listens on 8080 by default and runs as UID 101 (nginx).
# Required for TEN-63 / STRIDE E5: image runs as non-root.
FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build --chown=nginx:nginx /app/dist/frontend/browser /usr/share/nginx/html
COPY --chown=nginx:nginx docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost:8080/ > /dev/null || exit 1
