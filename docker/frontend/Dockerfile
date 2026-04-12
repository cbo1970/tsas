FROM node:24-alpine AS build
WORKDIR /app
RUN npm install -g npm@11.12.1
COPY frontend/.npmrc frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npx ng build --configuration docker

FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
