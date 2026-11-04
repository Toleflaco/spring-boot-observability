# spring-boot-observability

Aplicación Spring Boot de demo instrumentada con logs JSON, correlation ID y métricas
Micrometer, y dos stacks de observabilidad en local:

- **Grafana Stack**: Prometheus, Loki, Promtail y Grafana (`infra/grafana/`)
- **ELK**: Elasticsearch, Logstash y Kibana (`infra/elk/`)

## Requisitos

- Docker con Docker Compose
- Java 21

## Arranque del Grafana Stack

```bash
cd infra/grafana
cp .env.example .env        # cambia GRAFANA_ADMIN_USER y GRAFANA_ADMIN_PASSWORD (vienen como "cambiame")
docker compose up -d
docker compose ps
```

Las versiones de las imágenes y los puertos también se fijan en el `.env`.

Grafana arranca con las data sources (`loki`, `prometheus`) y el dashboard
*App demo – Observabilidad* ya creados por provisioning. No hay que importar nada.

## Arranque de la app

Desde la raíz del repo:

```bash
./mvnw spring-boot:run
```

La app escucha en `http://localhost:8080`.

## Generar tráfico

Sin peticiones, los paneles muestran *No data*.

```bash
for i in 1 2 3; do curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/demo/ok; done
curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/demo/error
curl -s -o /dev/null -w "%{http_code} %{time_total}\n" localhost:8080/api/demo/slow
```

## Ver el dashboard

Puertos por defecto; si los cambias en el `.env`, cambian aquí.

| Qué | URL |
|---|---|
| Grafana | http://localhost:3000 (`GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` del `.env`) |
| Prometheus | http://localhost:9090/targets |
| Métricas de la app | http://localhost:8080/actuator/prometheus |

El desplegable **uri** filtra los tres paneles.

## Cambiar el dashboard

El dashboard está bloqueado para guardar desde la web (`allowUiUpdates: false`).
La fuente de verdad es `infra/grafana/dashboards/app-demo.json`.

1. Edita el panel en la web.
2. Borra los overrides que hayas creado sin querer (por ejemplo, al hacer clic en la leyenda).
3. *Export → Save to file*, con "Export for sharing externally" **desactivado**.
4. Sustituye `infra/grafana/dashboards/app-demo.json` y deja permisos `644`.
5. Comprueba antes del commit:
   ```bash
   cd infra/grafana
   python3 -c 'import json; d=json.load(open("dashboards/app-demo.json")); [print(p["title"], p["fieldConfig"]["overrides"]) for p in d["panels"]]'
   grep -o 'DS_' dashboards/app-demo.json | wc -l
   ```
   Esperado: `[]` en todos los paneles y `0`.
6. Commit. Grafana recoge el fichero solo; recarga con **Ctrl+F5** antes de mirar.

## Si algo no sale

| Síntoma | Dónde mirar |
|---|---|
| Paneles en *No data* | ¿Hay tráfico en los últimos 5 min? |
| Faltan métricas | http://localhost:9090/targets: el target de la app debe estar `UP` |
| Faltan logs | `docker compose logs promtail` y `curl localhost:3100/ready` |
| "Data source not found" | Ctrl+F5 en el navegador |

## Arranque de ELK

_Pendiente._
