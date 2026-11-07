#!/usr/bin/env bash
# Copia/restaura exclusivamente datos SINTETICOS en MySQL temporal Docker.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p evidencias-restauracion
CONTAINER="apf3-mysql-restore-${GITHUB_RUN_ID:-manual-$$}"
PASSWORD='only-fixture-local-demo-2026'
cleanup(){ docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT

# Sin publicar puertos: la instancia no es accesible desde la red del runner.
docker run -d --name "$CONTAINER" -e MYSQL_ROOT_PASSWORD="$PASSWORD" mysql:8.4 >/dev/null
ready=0
for i in $(seq 1 90); do
  if docker exec -e MYSQL_PWD="$PASSWORD" "$CONTAINER" mysql -uroot --batch --skip-column-names -e "SELECT 1" >/dev/null 2>&1; then ready=1; break; fi
  sleep 2
done
[ "$ready" = 1 ] || { echo 'MySQL laboratorio no inició'; exit 1; }

CLIENT=(docker exec -i -e "MYSQL_PWD=$PASSWORD" "$CONTAINER" mysql -uroot --batch --skip-column-names)
DUMP=(docker exec -e "MYSQL_PWD=$PASSWORD" "$CONTAINER" mysqldump -uroot --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF)
"${CLIENT[@]}" < backend/database/schema.sql
"${CLIENT[@]}" delicias_peruanas < backup/fixture_recuperacion.sql
"${DUMP[@]}" delicias_peruanas > evidencias-restauracion/backup_sintetico.sql
sha256sum evidencias-restauracion/backup_sintetico.sql | awk '{print $1}' > evidencias-restauracion/sha256.txt
"${CLIENT[@]}" -e 'CREATE DATABASE dp_restauracion_laboratorio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
"${CLIENT[@]}" dp_restauracion_laboratorio < evidencias-restauracion/backup_sintetico.sql
printf 'tabla,origen,destino,coinciden\n' > evidencias-restauracion/comparacion.csv
for tab in usuarios categorias productos mesas reservas; do
  origen=$("${CLIENT[@]}" -e "SELECT COUNT(*) FROM delicias_peruanas.$tab;")
  destino=$("${CLIENT[@]}" -e "SELECT COUNT(*) FROM dp_restauracion_laboratorio.$tab;")
  [ "$origen" = "$destino" ] && coinciden='SI' || { coinciden='NO'; echo "FALLA: $tab" >&2; exit 1; }
  printf '%s,%s,%s,%s\n' "$tab" "$origen" "$destino" "$coinciden" >> evidencias-restauracion/comparacion.csv
done
# Validar integridad referencial y consultas con JOIN sobre la copia.
"${CLIENT[@]}" -e 'SELECT COUNT(*) FROM dp_restauracion_laboratorio.productos p JOIN dp_restauracion_laboratorio.categorias c ON p.categoria_id=c.id;' | grep -qx 1
"${CLIENT[@]}" -e 'SELECT COUNT(*) FROM dp_restauracion_laboratorio.reservas r JOIN dp_restauracion_laboratorio.mesas m ON r.mesa_id=m.id;' | grep -qx 1
# Un dump SQL puede contener datos sensibles. Borrar tras la comparación; conservar solo métricas.
rm -f evidencias-restauracion/backup_sintetico.sql
printf 'RESULTADO=RESTAURACION_SINTETICA_VERIFICADA\n' | tee evidencias-restauracion/resultado.txt
cat evidencias-restauracion/comparacion.csv
