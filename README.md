# SmartHire

Production-grade AI hiring platform with:

- Spring Boot backend
- FastAPI ML service
- PostgreSQL, Redis, Kafka
- JWT auth and STOMP chat
- Sentence-BERT, FAISS, XGBoost, LightGBM

## Project Structure

```text
backend (Spring Boot)
src/main/java/com/smarthire
src/main/java/com/smarthire/platform/controller
src/main/java/com/smarthire/platform/service
src/main/java/com/smarthire/platform/repository
src/main/java/com/smarthire/platform/entity
src/main/java/com/smarthire/platform/config
src/main/java/com/smarthire/platform/security
src/main/java/com/smarthire/platform/websocket

ml-service/
  app/models
  app/services
  app/utils
  pipelines
```

## Run

```bash
docker compose up --build
```
