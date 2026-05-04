const http = require("http");
const fs = require("fs");
const path = require("path");
const net = require("net");

const WEB_PORT = Number(process.env.WEB_CLIENT_PORT || 3000);
const WEB_HOST = process.env.WEB_CLIENT_HOST || "127.0.0.1";
const STATIC_DIR = path.join(__dirname, "public");
const REQUEST_TIMEOUT_MS = Number(process.env.WEB_CLIENT_TIMEOUT_MS || 7000);

const SENDER_ENDPOINTS = {
  teacher: { host: "127.0.0.1", port: 1600, id: "teacher" },
  student1: { host: "127.0.0.1", port: 1701, id: "student1" },
  student2: { host: "127.0.0.1", port: 1702, id: "student2" },
  student3: { host: "127.0.0.1", port: 1703, id: "student3" },
};

const TARGET_OPTIONS = {
  teacher: { type: "TEACHER", value: "teacher", label: "Teacher" },
  all_students: { type: "ALL_STUDENTS", value: "-", label: "Toti studentii" },
  student1: { type: "STUDENT", value: "student1", label: "Student 1" },
  student2: { type: "STUDENT", value: "student2", label: "Student 2" },
  student3: { type: "STUDENT", value: "student3", label: "Student 3" },
};

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = "";
    req.on("data", (chunk) => {
      data += chunk.toString("utf8");
      if (data.length > 128 * 1024) {
        reject(new Error("Payload prea mare."));
        req.destroy();
      }
    });
    req.on("end", () => resolve(data));
    req.on("error", reject);
  });
}

function askMicroservice(sender, target, question) {
  return new Promise((resolve, reject) => {
    const senderConfig = SENDER_ENDPOINTS[sender];
    const targetConfig = TARGET_OPTIONS[target];

    if (!senderConfig) {
      reject(new Error("Sender invalid."));
      return;
    }
    if (!targetConfig) {
      reject(new Error("Target invalid."));
      return;
    }

    const trimmedQuestion = (question || "").trim();
    if (!trimmedQuestion) {
      reject(new Error("Intrebarea nu poate fi goala."));
      return;
    }

    const payload = `ASK|${targetConfig.type}|${targetConfig.value}|${trimmedQuestion}\n`;
    const socket = net.createConnection(
      { host: senderConfig.host, port: senderConfig.port },
      () => socket.write(payload)
    );

    const chunks = [];
    let settled = false;
    const done = (err, result) => {
      if (settled) {
        return;
      }
      settled = true;
      socket.destroy();
      if (err) {
        reject(err);
      } else {
        resolve(result);
      }
    };

    socket.setTimeout(REQUEST_TIMEOUT_MS, () => {
      done(new Error("Timeout la conectarea catre microserviciu."));
    });

    socket.on("data", (chunk) => chunks.push(chunk));
    socket.on("end", () => {
      const raw = Buffer.concat(chunks).toString("utf8").trim();
      const responseText = raw || "Nu s-a primit niciun raspuns.";
      done(null, {
        senderId: senderConfig.id,
        targetLabel: targetConfig.label,
        question: trimmedQuestion,
        responseText,
      });
    });
    socket.on("error", (err) => {
      done(
        new Error(
          `Eroare de conectare la microserviciul ${senderConfig.id} (${senderConfig.port}): ${err.message}`
        )
      );
    });
  });
}

function sendJson(res, code, payload) {
  res.writeHead(code, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
  });
  res.end(JSON.stringify(payload));
}

function serveStatic(req, res) {
  const urlPath = req.url === "/" ? "/index.html" : req.url;
  const safePath = path.normalize(urlPath).replace(/^(\.\.[\/\\])+/, "");
  const filePath = path.join(STATIC_DIR, safePath);

  if (!filePath.startsWith(STATIC_DIR)) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end("Not Found");
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType =
      ext === ".html"
        ? "text/html; charset=utf-8"
        : ext === ".js"
        ? "application/javascript; charset=utf-8"
        : ext === ".css"
        ? "text/css; charset=utf-8"
        : "application/octet-stream";

    res.writeHead(200, {
      "Content-Type": contentType,
      "Cache-Control": "no-store",
    });
    res.end(data);
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === "POST" && req.url === "/api/ask") {
    try {
      const rawBody = await readBody(req);
      const body = JSON.parse(rawBody || "{}");
      const answer = await askMicroservice(body.sender, body.target, body.question);
      sendJson(res, 200, { ok: true, ...answer });
    } catch (err) {
      sendJson(res, 400, { ok: false, error: err.message || "Eroare necunoscuta." });
    }
    return;
  }

  if (req.method === "GET") {
    serveStatic(req, res);
    return;
  }

  res.writeHead(405);
  res.end("Method Not Allowed");
});

server.listen(WEB_PORT, WEB_HOST, () => {
  console.log(`Web client pornit: http://${WEB_HOST}:${WEB_PORT}`);
});
