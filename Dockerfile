FROM node:20.12.2-alpine AS frontend-builder
WORKDIR /app/frontend
COPY web/src/main/javascript/package*.json ./
RUN npm config set fetch-timeout 600000
RUN npm ci
COPY web/src/main/javascript/ ./
RUN npm run build

FROM golang:1.24.5-alpine AS backend-builder
RUN apk add --no-cache git openssh-client
WORKDIR /app/backend

COPY web/src/main/go/go.mod web/src/main/go/go.sum ./
RUN go mod download
COPY web/src/main/go/ ./
RUN go install github.com/swaggo/swag/cmd/swag@latest
RUN swag init -g ./cmd/server/main.go -o ./docs
RUN CGO_ENABLED=0 GOOS=linux go build -o main ./cmd/server/

COPY trees ./trees

FROM alpine:latest
RUN apk --no-cache add ca-certificates
WORKDIR /root/backend/
COPY --from=backend-builder /app/backend/ ./
COPY --from=frontend-builder /app/resources/static/ ./frontend/static

ENV TREE_DIR=/root/backend/trees
ENV STATIC_DIR=/root/backend/frontend/static

EXPOSE 8080
ENV GIN_MODE=release
CMD ["./main"]