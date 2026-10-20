FROM node:22.13.0-alpine3.21

WORKDIR /app
RUN npm install --global pnpm@10.18.0

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --prod --frozen-lockfile

COPY server ./server
COPY public ./public
ENV NODE_ENV=production
EXPOSE 3000
CMD ["pnpm", "start"]
