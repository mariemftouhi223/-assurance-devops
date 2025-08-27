FROM node:18-alpine AS build

WORKDIR /app

# Copier package.json pour cache
COPY package*.json ./

# Installer toutes les dépendances (dev + prod pour Angular CLI)
RUN npm ci --silent

# Copier code source
COPY . .

# Build Angular
RUN npm run build

# Production avec Nginx
FROM nginx:alpine

# Config Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier build Angular (dist/book-network-ui ou dist/)
COPY --from=build /app/dist /usr/share/nginx/html

# Sécurité
RUN addgroup -g 1001 -S nginx && \
    adduser -S -D -H -u 1001 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx

RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

RUN touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

USER nginx

EXPOSE 4200

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:4200/health || exit 1

CMD ["nginx", "-g", "daemon off;"]

