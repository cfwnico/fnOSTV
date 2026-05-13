import { spawn } from "node:child_process";
import { mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";

const chromePath = process.env.FNOS_CHROME;
const baseUrl = process.env.FNOS_WEB_URL;
const username = process.env.FNOS_WEB_USER;
const password = process.env.FNOS_WEB_PASSWORD;
const outDir = resolve(process.env.FNOS_OUT_DIR || ".tooling/fnos-network-capture");
const port = Number(process.env.FNOS_CDP_PORT || "9555");

if (!chromePath || !baseUrl || !username || !password) {
  throw new Error("Missing FNOS_CHROME/FNOS_WEB_URL/FNOS_WEB_USER/FNOS_WEB_PASSWORD");
}

mkdirSync(outDir, { recursive: true });
const userDataDir = join(outDir, "chrome-profile");
rmSync(userDataDir, { recursive: true, force: true });

const chrome = spawn(chromePath, [
  "--headless",
  "--disable-gpu",
  "--disable-dev-shm-usage",
  "--no-first-run",
  "--no-default-browser-check",
  "--remote-allow-origins=*",
  `--remote-debugging-port=${port}`,
  `--user-data-dir=${userDataDir}`,
  "--window-size=1920,1080",
  "about:blank",
], { stdio: ["ignore", "ignore", "pipe"] });

const chromeErr = [];
chrome.stderr.on("data", (chunk) => chromeErr.push(String(chunk)));
chrome.on("exit", (code) => {
  writeFileSync(join(outDir, "chrome-stderr.log"), chromeErr.join("") + `\nexit=${code}\n`, "utf8");
});
process.on("exit", () => chrome.kill());

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function retry(fn, times = 80) {
  let last;
  for (let i = 0; i < times; i++) {
    try {
      return await fn();
    } catch (error) {
      last = error;
      await sleep(250);
    }
  }
  throw last;
}

const target = await retry(async () => {
  const response = await fetch(`http://127.0.0.1:${port}/json/list`);
  if (!response.ok) throw new Error(`CDP list ${response.status}`);
  const targets = await response.json();
  const page = targets.find((item) => item.type === "page" && item.webSocketDebuggerUrl);
  if (!page) throw new Error("No CDP page target");
  return page;
});

const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
  ws.addEventListener("open", resolve, { once: true });
  ws.addEventListener("error", reject, { once: true });
});

let id = 0;
const pending = new Map();
const events = [];
const requestBodies = new Map();
const responseBodies = new Map();

function record(type, payload) {
  events.push({ type, ts: new Date().toISOString(), ...payload });
}

ws.addEventListener("message", async (event) => {
  const message = JSON.parse(event.data);
  if (message.id && pending.has(message.id)) {
    const { resolve, reject } = pending.get(message.id);
    pending.delete(message.id);
    message.error ? reject(new Error(message.error.message)) : resolve(message.result);
    return;
  }
  if (!message.method) return;
  if (message.method === "Network.requestWillBeSent") {
    const request = message.params.request || {};
    record("request", {
      requestId: message.params.requestId,
      method: request.method,
      url: request.url,
      resourceType: message.params.type,
      postData: request.postData || "",
    });
  } else if (message.method === "Network.responseReceived") {
    const response = message.params.response || {};
    record("response", {
      requestId: message.params.requestId,
      url: response.url,
      status: response.status,
      mimeType: response.mimeType,
      resourceType: message.params.type,
    });
  } else if (message.method === "Network.webSocketCreated") {
    record("websocket-created", {
      requestId: message.params.requestId,
      url: message.params.url,
    });
  } else if (message.method === "Network.webSocketFrameSent") {
    record("websocket-sent", {
      requestId: message.params.requestId,
      opcode: message.params.response?.opcode,
      payloadData: message.params.response?.payloadData || "",
    });
  } else if (message.method === "Network.webSocketFrameReceived") {
    record("websocket-received", {
      requestId: message.params.requestId,
      opcode: message.params.response?.opcode,
      payloadData: message.params.response?.payloadData || "",
    });
  }
});

function send(method, params = {}) {
  const callId = ++id;
  ws.send(JSON.stringify({ id: callId, method, params }));
  return new Promise((resolve, reject) => pending.set(callId, { resolve, reject }));
}

async function evaluate(expression) {
  const result = await send("Runtime.evaluate", {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  return result.result?.value;
}

async function clickText(text) {
  return evaluate(`
    (() => {
      const nodes = [...document.querySelectorAll('button,a,div,span,p')];
      const node = nodes.find(n => (n.innerText || n.textContent || '').trim() === ${JSON.stringify(text)});
      if (!node) return false;
      node.click();
      return true;
    })()
  `);
}

async function clickTitle(title) {
  return evaluate(`
    (() => {
      const node = [...document.querySelectorAll('[title],button,a,div,span')]
        .find(n => (n.getAttribute('title') || '').trim() === ${JSON.stringify(title)}
          || (n.innerText || n.textContent || '').trim() === ${JSON.stringify(title)});
      if (!node) return false;
      node.click();
      return true;
    })()
  `);
}

async function dumpResponseBodies() {
  for (const event of events.filter((item) => item.type === "response")) {
    if (!/json|text|javascript|html/i.test(event.mimeType || "")) continue;
    if (responseBodies.has(event.requestId)) continue;
    try {
      const body = await send("Network.getResponseBody", { requestId: event.requestId });
      responseBodies.set(event.requestId, body.body?.slice(0, 20000) || "");
    } catch {
      responseBodies.set(event.requestId, "");
    }
  }
}

await send("Page.enable");
await send("Runtime.enable");
await send("Network.enable", { maxPostDataSize: 1024 * 1024 });
await send("Emulation.setDeviceMetricsOverride", {
  width: 1920,
  height: 1080,
  deviceScaleFactor: 1,
  mobile: false,
});

await send("Page.navigate", { url: baseUrl });
await sleep(5000);
await evaluate(`
  (() => {
    const inputs = [...document.querySelectorAll('input')];
    const userInput = inputs.find(i => /user|用户名|账号|name/i.test(i.placeholder || i.name || i.id)) || inputs[0];
    const passInput = inputs.find(i => i.type === 'password' || /pass|密码/i.test(i.placeholder || i.name || i.id)) || inputs[1];
    function setValue(input, value) {
      if (!input) return;
      input.focus();
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
      setter.call(input, value);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
    setValue(userInput, ${JSON.stringify(username)});
    setValue(passInput, ${JSON.stringify(password)});
  })()
`);
await sleep(500);
await clickText("登录");
await sleep(7000);

const actions = [
  ["favorites", () => clickText("收藏")],
  ["library", () => clickText("影视大全")],
  ["all", () => clickText("全部")],
  ["movie", () => clickText("电影")],
  ["tv", () => clickText("电视节目")],
  ["other", () => clickText("其他")],
  ["settings", () => clickTitle("设置")],
  ["account", () => clickText("账号连接")],
  ["media-settings", () => clickText("媒体库")],
];

for (const [name, action] of actions) {
  record("action", { name });
  await action();
  await sleep(2500);
}

await dumpResponseBodies();

writeFileSync(join(outDir, "network-events.json"), JSON.stringify(events, null, 2), "utf8");
writeFileSync(join(outDir, "response-bodies.json"), JSON.stringify(Object.fromEntries(responseBodies), null, 2), "utf8");

ws.close();
chrome.kill();
