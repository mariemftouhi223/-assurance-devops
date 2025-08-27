# ---- Build Angular ----
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
# évite les OOM lors du build Angular
ENV NODE_OPTIONS="--max-old-space-size=4096"
RUN npm run build

# ---- Nginx ----
FROM nginx:alpine
# ta conf Nginx
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf
# copie du build Angular
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# pas de USER ici
