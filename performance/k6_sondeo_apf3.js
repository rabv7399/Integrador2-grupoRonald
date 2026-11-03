// APF3 — Sondeo de concurrencia ligero, SOLO LECTURA.
// No mide capacidad maxima ni garantiza SLA; nunca ejecuta POST/PUT/DELETE.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'https://delicias-peruanas-api.onrender.com').replace(/\/$/, '');

export const options = {
  scenarios: {
    lectura_publica: {
      executor: 'constant-vus',
      vus: 2,
      duration: '12s',
      gracefulStop: '5s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    http_req_failed: ['rate<0.05'],
    checks: ['rate>0.95'],
  },
  discardResponseBodies: true,
};

export default function () {
  const response = http.get(`${BASE_URL}/`, {
    tags: { escenario: 'apf3_lectura_publica' },
    timeout: '20s',
    headers: { 'User-Agent': 'APF3-k6-UTP/1.0' },
  });
  check(response, {
    'GET / devuelve HTTP 200': (r) => r.status === 200,
  });
  sleep(1);
}
