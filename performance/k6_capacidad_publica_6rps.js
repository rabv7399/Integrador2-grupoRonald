// APF3 · Prueba de capacidad graduada, SOLAMENTE GET publico.
// 2 RPS x 15 s, 4 RPS x 15 s, 6 RPS x 20 s; maximo teorico 210 requests.
// No acredita la capacidad de endpoints autenticados ni base de datos.
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'https://delicias-peruanas-api.onrender.com').replace(/\/$/, '');
export const options = {
  scenarios: {
    publico_escalonado: {
      executor: 'ramping-arrival-rate',
      startRate: 2,
      timeUnit: '1s',
      preAllocatedVUs: 12,
      maxVUs: 15,
      stages: [
        { target: 2, duration: '15s' },
        { target: 4, duration: '15s' },
        { target: 6, duration: '20s' },
      ],
      gracefulStop: '10s',
    },
  },
  discardResponseBodies: true,
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
    http_req_duration: [{ threshold: 'p(95)<1500', abortOnFail: false }],
    checks: [{ threshold: 'rate>0.99', abortOnFail: false }],
    dropped_iterations: ['count==0'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/`, {
    timeout: '15s',
    tags: { caso: 'apf3_capacidad_publica_6rps' },
    headers: { 'User-Agent': 'APF3-UTP-Capacidad-Controlada/1.0' },
  });
  check(res, { 'GET publico 200': (r) => r.status === 200 });
}
