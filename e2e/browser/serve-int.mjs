// Servidor del host de prueba para el navegador. / -> host-int.html, /kc-token -> Keycloak de PRUEBA del edge
// (publicado en 127.0.0.1:9091) reenviando Host: keycloak:8080 para que el `iss` del token coincida con el que
// valida el PDP; el resto -> security-ui/dist. Puerto 5190.
import http from "node:http"; import fs from "node:fs"; import path from "node:path"; import url from "node:url";
const here = path.dirname(url.fileURLToPath(import.meta.url));
const dist = path.resolve(here, "../../../security-ui/dist");
const types = { ".js": "text/javascript", ".html": "text/html", ".json": "application/json", ".css": "text/css" };
http.createServer((q, s) => {
  if (q.url === "/kc-token" && q.method === "POST") {
    const up = http.request({ host: "127.0.0.1", port: 9091, method: "POST",
      path: "/realms/security-baseline/protocol/openid-connect/token",
      headers: { "content-type": q.headers["content-type"], host: "keycloak:8080" } }, (r) => {
      s.writeHead(r.statusCode, { "content-type": r.headers["content-type"] ?? "application/json" }); r.pipe(s); });
    up.on("error", (e) => s.writeHead(502).end(String(e))); q.pipe(up); return;
  }
  const f = q.url === "/" ? path.join(here, "host-int.html") : path.join(dist, decodeURIComponent(q.url.split("?")[0]));
  if (!f.startsWith(here) && !f.startsWith(dist)) { s.writeHead(403).end(); return; }
  fs.readFile(f, (e, d) => { if (e) { s.writeHead(404).end(); return; }
    s.writeHead(200, { "content-type": types[path.extname(f)] ?? "application/octet-stream" }).end(d); });
}).listen(5190, "127.0.0.1", () => console.log("host de prueba en http://localhost:5190"));
