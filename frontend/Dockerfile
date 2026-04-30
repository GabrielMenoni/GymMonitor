FROM node:22-alpine AS build

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . ./

RUN npm run build

FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY --from=build /workspace/dist ./dist

EXPOSE 4200

CMD ["node", "dist/GymMonitor/server/server.mjs"]