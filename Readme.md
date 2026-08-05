# Byteblog api

- Blog application api specialized for tech

## Docker commands
### Run only the db
```bash
docker compose -f docker-compose.db.yaml up -d
```

```bash
docker compose -f docker-compose.db.yaml down
```

### Run the application and db
```bash
docker compose up -d
```