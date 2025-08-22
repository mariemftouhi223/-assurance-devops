FROM node:18 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration development

FROM nginx:1.25-alpine

COPY --from=build /app/dist /opt/dist

RUN set -e; \
    if [ -d "/opt/dist/browser" ]; then \
        cp -r /opt/dist/browser/* /usr/share/nginx/html/; \
    elif find /opt/dist -maxdepth 2 -type d -name browser | grep -q .; then \
        d=$(find /opt/dist -maxdepth 2 -type d -name browser | head -n1); \
        cp -r "$d"/* /usr/share/nginx/html/; \
    elif [ "$(find /opt/dist -mindepth 1 -maxdepth 1 -type d | wc -l)" -gt 0 ]; then \
        d=$(find /opt/dist -mindepth 1 -maxdepth 1 -type d | head -n1); \
        cp -r "$d"/* /usr/share/nginx/html/; \
    else \
        cp -r /opt/dist/* /usr/share/nginx/html/; \
    fi

EXPOSE 80
