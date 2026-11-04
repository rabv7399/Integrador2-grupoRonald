"""Sondeo limitado de lectura autenticada; no es una prueba de estrés."""
import concurrent.futures
import getpass
import json
import math
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

BASE = "https://delicias-peruanas-api.onrender.com"
ROUTES = ("/api/categorias", "/api/productos")
STOP = threading.Event()


def request(path, token=None, credentials=None):
    headers = {"Accept": "application/json"}
    payload = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if credentials is not None:
        headers["Content-Type"] = "application/json"
        payload = json.dumps(credentials).encode()
    req = urllib.request.Request(BASE + path, data=payload, headers=headers)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
    elapsed = (time.perf_counter() - start) * 1000
    return status, body, elapsed


def percentile(values, p):
    return sorted(values)[max(0, math.ceil(len(values) * p) - 1)]


def worker(number, token):
    rows = []
    for cycle in range(10):
        if STOP.is_set():
            break
        route = ROUTES[(cycle + number) % 2]
        try:
            status, body, elapsed = request(route, token=token)
            valid = status == 200 and isinstance(json.loads(body), list)
        except Exception:
            status, elapsed, valid = 0, None, False
        rows.append({"worker": number, "cycle": cycle + 1,
                     "route": route, "status": status,
                     "duration_ms": round(elapsed, 2) if elapsed is not None else None,
                     "valid_list": valid})
        if not valid or (elapsed is not None and elapsed > 5000):
            STOP.set()
            break
        STOP.wait(1)
    return rows


def main():
    correo = input("Correo de cuenta de prueba autorizada: ").strip()
    password = getpass.getpass("Contraseña (no se muestra ni se guarda): ")
    try:
        status, body, _ = request("/api/auth/login",
                                  credentials={"correo": correo, "password": password})
        password = ""
        data = json.loads(body)
        token = data.get("token", "")
        if status != 200 or not token:
            print("Login no validado. Sondeo cancelado; no se guarda la respuesta.")
            return
        del data, body
        warmup = []
        for route in ROUTES:
            status, body, duration = request(route, token=token)
            valid = status == 200 and isinstance(json.loads(body), list)
            warmup.append({"route": route, "status": status,
                           "duration_ms": round(duration, 2), "valid_list": valid})
            if not valid:
                print("Preparación no validada. Sondeo cancelado.")
                return
        started = datetime.now(timezone.utc).isoformat()
        start = time.perf_counter()
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda n: worker(n, token), range(2)))
        token = ""
        rows = [row for result in results for row in result]
        seconds = time.perf_counter() - start
        summary = {}
        for route in ROUTES:
            selected = [r for r in rows if r["route"] == route]
            durations = [r["duration_ms"] for r in selected if r["duration_ms"] is not None]
            summary[route] = {
                "requests": len(selected),
                "valid_200": sum(r["valid_list"] for r in selected),
                "failed": sum(not r["valid_list"] for r in selected),
                "mean_ms": round(sum(durations) / len(durations), 2) if durations else None,
                "p50_ms": percentile(durations, .5) if durations else None,
                "p95_ms": percentile(durations, .95) if durations else None,
                "max_ms": max(durations) if durations else None}
        report = {"started_utc": started, "base_url": BASE,
                  "revision_observed": input("SHA Live observado en Render: ").strip(),
                  "workers": 2, "planned_requests": 20,
                  "measured_requests": len(rows), "aborted": STOP.is_set(),
                  "duration_seconds": round(seconds, 2),
                  "observed_rps": round(len(rows) / seconds, 3),
                  "percentile_method": "nearest rank; muestra exploratoria pequeña",
                  "warmup_excluded": warmup, "summary": summary, "samples": rows}
        filename = Path("APF3_sondeo_autenticado_" +
                        datetime.now().strftime("%Y%m%d_%H%M%S") + ".json")
        filename.write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(json.dumps(summary, indent=2))
        print("Muestra completada:", len(rows), "/20; interrupción:", STOP.is_set())
        print("Resultado sin credenciales:", filename)
        print("No valida 20 usuarios, 6 RPS, SLA ni capacidad máxima.")
    except Exception:
        print("No se completó el sondeo. Revisar disponibilidad sin publicar credenciales.")


if __name__ == "__main__":
    main()
