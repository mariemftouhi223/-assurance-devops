FROM node:18 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# Build prod via Angular CLI installé en devDependency
RUN npm run build -- --configuration production

FROM nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copie le build Angular vers le dossier servi par Nginx
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
