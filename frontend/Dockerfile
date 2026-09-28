# ---- Build stage ----
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# ---- Serve stage ----
FROM nginx:alpine
COPY --from=builder /app/www /usr/share/nginx/html
EXPOSE 8100
CMD ["nginx", "-g", "daemon off;"]
