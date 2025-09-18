# ---- Build Angular ----
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
ENV NODE_OPTIONS="--max-old-space-size=4096"
RUN npm run build -- --configuration production

# ---- Nginx ----
FROM nginx:1.25-alpine
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/book-network-ui/ /usr/share/nginx/html/
EXPOSE 80
HEALTHCHECK CMD wget -qO- http://localhost/health || exit 1
