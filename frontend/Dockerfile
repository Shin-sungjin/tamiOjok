# ---- Build stage ----
FROM rockylinux:9 AS build

RUN dnf module enable -y nodejs:20 && dnf install -y nodejs && dnf clean all

WORKDIR /workspace

COPY package.json ./
RUN npm install

COPY . .

# 빈 값(기본값)이면 axios가 상대경로로 요청 → nginx가 /api/ 를 backend로 프록시
# (Cloudflare Tunnel처럼 frontend만 노출되는 환경에서 CORS 없이 동작하게 하기 위함)
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# ---- Run stage ----
FROM rockylinux:9 AS run

RUN dnf install -y nginx && dnf clean all

COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
