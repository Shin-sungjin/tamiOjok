# ---- Build stage ----
FROM rockylinux:9 AS build

RUN dnf module enable -y nodejs:20 && dnf install -y nodejs && dnf clean all

WORKDIR /workspace

COPY package.json ./
RUN npm install

COPY . .

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ---- Run stage ----
FROM rockylinux:9 AS run

RUN dnf install -y nginx && dnf clean all

COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
