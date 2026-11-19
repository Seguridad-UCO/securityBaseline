// Servidor estatico minimo para el host de prueba: / -> e2e/host.html, el resto -> security-ui/dist. Puerto 5190.
import http from "node:http"; import fs from "node:fs"; import path from "node:path"; import url from "node:url";
const here = path.dirname(url.fileURLToPath(import.meta.url));
const dist = path.resolve(here, "../../security-ui/dist");
http.createServer((q, s) => {
  const f = q.url === "/" ? path.join(here, "host.html") : path.join(dist, decodeURIComponent(q.url.split("?")[0]));
  if (!f.startsWith(here) && !f.startsWith(dist)) { s.writeHead(403).end(); return; }
  fs.readFile(f, (e, d) => { if (e) { s.writeHead(404).end(); return; }
    s.writeHead(200, { "content-type": f.endsWith(".js") ? "text/javascript" : f.endsWith(".html") ? "text/html" : "application/octet-stream" }).end(d); });
}).listen(5190, () => console.log("host de prueba en http://localhost:5190"));
