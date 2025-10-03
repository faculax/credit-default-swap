# Risk Engine Service

This service provides a REST API wrapper around the Open Source Risk Engine (ORE) for calculating credit default swap (CDS) risk metrics.

## Architecture

The service consists of:
- **Base Layer**: Pre-built ORE v1.8.13.1 container with all quantitative libraries
- **Service Layer**: Spring Boot REST API that interfaces with ORE binaries
- **Integration Layer**: Process management and data transformation

## Building

### Prerequisites
1. Pre-built ORE image: `ore:v1.8.13.1`
   ```bash
   # Verify you have the ORE image
   docker images | grep ore
   ```

### Build the Service
```bash
# Build the risk-engine service
docker build -t risk-engine:latest .

# Or build with specific tag
docker build -t risk-engine:v1.0.0 .
```

## Running

### Standalone
```bash
docker run -p 8082:8082 risk-engine:latest
```

### With Docker Compose
```yaml
services:
  risk-engine:
    image: risk-engine:latest
    ports:
      - "8082:8082"
    environment:
      - RISK_IMPL=ORE
      - ORE_TIMEOUT=120
    volumes:
      - ore-work:/tmp/ore-work
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/api/risk/health"]
      interval: 30s
      timeout: 5s
      retries: 3
```

## API Endpoints

### Health Check
```bash
curl http://localhost:8082/api/risk/health
```

### Risk Calculation
```bash
curl -X POST http://localhost:8082/api/risk/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "trades": [...],
    "marketData": {...},
    "config": {...}
  }'
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `RISK_IMPL` | `ORE` | Risk engine implementation |
| `ORE_BINARY_PATH` | `/usr/local/bin/ore` | Path to ORE binary |
| `ORE_CONFIG_PATH` | `/app/ore/config/ore.xml` | ORE configuration file |
| `ORE_WORK_DIR` | `/tmp/ore-work` | Working directory for ORE |
| `ORE_TIMEOUT` | `60` | Timeout for ORE calculations (seconds) |
| `ORE_WARMUP_TIMEOUT` | `30` | Warmup timeout (seconds) |
| `ORE_RESTART_DELAY` | `5` | Delay between restarts (seconds) |
| `ORE_MAX_RESTARTS` | `3` | Maximum restart attempts |

### ORE Configuration

The service includes a basic ORE configuration at `/app/ore/config/ore.xml`. For advanced scenarios, mount your own configuration:

```bash
docker run -v /path/to/your/ore.xml:/app/ore/config/ore.xml risk-engine:latest
```

## Development

### Local Development
```bash
# Build Java artifacts
mvn clean package

# Run with local Maven
mvn spring-boot:run

# Run verification script
./scripts/verify-ore.sh
```

### Debugging
```bash
# Check ORE integration
docker run -it risk-engine:latest /app/scripts/verify-ore.sh

# Interactive shell
docker run -it risk-engine:latest bash
```

## Integration with Existing ORE Image

This Dockerfile is designed to reuse your expensive 2-hour ORE build (`ore:v1.8.13.1`) by:

1. **Using it as base image** - No rebuild of QuantLib/ORE stack
2. **Adding Java runtime** - Installs OpenJDK 21 on top of ORE
3. **Layering REST API** - Adds Spring Boot service as final layer
4. **Preserving ORE binaries** - Uses actual ORE binaries instead of placeholders

### Benefits
- ✅ **Fast iterations** - Only rebuild Spring Boot layer (~30 seconds)
- ✅ **Production ORE** - Real quantitative calculations
- ✅ **Consistent environment** - Same ORE version across dev/prod
- ✅ **Resource efficient** - Reuse expensive computational layer

## Troubleshooting

### Common Issues

1. **ORE binary not found**
   ```bash
   # Check if ORE is in the container
   docker run -it risk-engine:latest which ore
   ```

2. **Java version mismatch**
   ```bash
   # Verify Java installation
   docker run -it risk-engine:latest java -version
   ```

3. **Permission issues**
   ```bash
   # Check file permissions
   docker run -it risk-engine:latest ls -la /app/
   ```

### Logs
```bash
# View service logs
docker logs risk-engine-container

# Follow logs
docker logs -f risk-engine-container
```