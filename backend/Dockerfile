FROM node:22-alpine
ENV NODE_ENV=production
WORKDIR /app
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund \
    && npm cache clean --force
COPY server.js migrate.js schema_public_v1.sql start.sh ./
RUN chmod +x start.sh
USER node
EXPOSE 8081
CMD ["./start.sh"]
