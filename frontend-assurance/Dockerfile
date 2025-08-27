FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
RUN npm run build

FROM nginx:alpine
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/book-network-ui /usr/share/nginx/html
EXPOSE 4200
CMD ["nginx", "-g", "daemon off;"]
