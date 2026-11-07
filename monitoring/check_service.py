"""Monitoreo HTTPS APF3: valida HTTP 200 con reintentos.
No modifica estado del servicio; emite registros auditables sin secretos.
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone

url = os.getenv("MONITOR_URL", "https://delicias-peruanas-api.onrender.com/")
attempts = int(os.getenv("MONITOR_ATTEMPTS", "3"))
timeout = int(os.getenv("MONITOR_TIMEOUT_SECONDS", "45"))
delay = int(os.getenv("MONITOR_RETRY_DELAY_SECONDS", "15"))
results = []
ok = False
for n in range(1, attempts + 1):
    start = time.monotonic()
    code = None
    error = None
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "DeliciasPeruanas-APF3-Monitor/1.0"})
        with urllib.request.urlopen(req, timeout=timeout) as response:
            code = response.status
            response.read(256)
        if code == 200:
            ok = True
    except urllib.error.HTTPError as exc:
        code = exc.code
        error = "HTTPError"
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        error = type(exc).__name__
    ms = round((time.monotonic() - start) * 1000, 1)
    results.append({"attempt": n, "status": code, "elapsed_ms": ms, "error": error})
    print(f"intento={n} HTTP={code} ms={ms} error={error}", flush=True)
    if ok:
        break
    if n < attempts:
        time.sleep(delay)

summary = {"timestamp_utc": datetime.now(timezone.utc).isoformat(), "url": url,
           "availability": "OK" if ok else "FAIL", "attempts": results,
           "note": "No calcula percentiles ni mide CPU/RAM; las demoras de arranque se registran."}
with open("resultado-monitor.json", "w", encoding="utf-8") as f:
    json.dump(summary, f, ensure_ascii=False, indent=2)
print("RESULTADO=" + summary["availability"])
sys.exit(0 if ok else 1)
