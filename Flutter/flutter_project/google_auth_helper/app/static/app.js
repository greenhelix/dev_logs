const byId = (id) => document.getElementById(id);

const API_BASE_STORAGE_KEY = "gah_api_base_url";
const MODE_STORAGE_KEY = "gah_ui_mode";
const CONSOLE_HEIGHT_STORAGE_KEY = "gah_console_height";

const MODE_CONFIG = {
  mode1: {
    label: "MODE 1 · Ubuntu Control",
    summary: "Ubuntu 운영 전체 기능",
  },
  mode2: {
    label: "MODE 2 · Windows/Hosting",
    summary: "조회/분석 중심 기능",
  },
};

const state = {
  wsLogs: null,
  wsTerminal: null,
  parsedResult: null,
  charts: {},
  devices: [],
  mode: "mode1",
  consoleExpanded: false,
  consoleHeight: 280,
  sidebarOpen: false,
};

const viewMeta = {
  dashboard: {
    title: "대시보드",
    description: "FW 인증 추이, Fail 감소 추이, 업로드 추이를 확인합니다.",
  },
  runner: {
    title: "테스트 실행",
    description: "인증 도구 실행과 작업 상태를 확인합니다.",
  },
  firmware: {
    title: "펌웨어 업로드",
    description: "ADB Push로 펌웨어를 업로드합니다.",
  },
  upload: {
    title: "결과서/업로드",
    description: "result.xml / result_failure.html 자동 분석 및 외부 업로드를 수행합니다.",
  },
  environment: {
    title: "환경 점검",
    description: "필수 환경과 권한 상태를 점검하고 watcher를 제어합니다.",
  },
  tools: {
    title: "도구 상태",
    description: "CTS/GTS/TVTS/VTS/STS/CTS-on-GSI 활성 상태를 확인합니다.",
  },
  firebase: {
    title: "Firebase 센터",
    description: "Firestore 상태 확인 및 동기화를 실행합니다.",
  },
  guide: {
    title: "사용방법",
    description: "모드별 기본 작업 흐름을 확인합니다.",
  },
  help: {
    title: "도움말",
    description: "자주 묻는 질문과 운영 팁을 확인합니다.",
  },
  settings: {
    title: "설정",
    description: "API Base URL 등 기본 연결 설정을 관리합니다.",
  },
};

const escapeHtml = (value) =>
  String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");

function getApiBase() {
  const fromWindow = (window.GAH_API_BASE || "").trim();
  if (fromWindow) return fromWindow.replace(/\/+$/, "");
  const fromStorage = (localStorage.getItem(API_BASE_STORAGE_KEY) || "").trim();
  return fromStorage.replace(/\/+$/, "");
}

function buildApiUrl(url) {
  if (!url.startsWith("/")) return url;
  const base = getApiBase();
  return base ? `${base}${url}` : url;
}

function getWsBase() {
  const base = getApiBase();
  if (!base) {
    const protocol = location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${location.host}`;
  }
  if (base.startsWith("https://")) return `wss://${base.slice("https://".length)}`;
  if (base.startsWith("http://")) return `ws://${base.slice("http://".length)}`;
  return base;
}

const badge = (status) => {
  if (status === "ok" || status === "success" || status === true) return `<span class="badge ok">정상</span>`;
  if (status === "warn" || status === "queued" || status === "running") return `<span class="badge warn-badge">${status}</span>`;
  return `<span class="badge error">${status}</span>`;
};

async function fetchJson(url, options = {}) {
  const response = await fetch(buildApiUrl(url), options);
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
  return response.json();
}

function modeAllowsElement(element, mode) {
  const spec = (element.dataset.mode || "all").split(",").map((item) => item.trim());
  return spec.includes("all") || spec.includes(mode);
}

function ensureActiveViewVisible() {
  const active = document.querySelector(".menu-btn.active");
  if (active && !active.classList.contains("hidden-by-mode")) return;
  const fallback = document.querySelector(".menu-btn:not(.hidden-by-mode)");
  if (fallback) switchView(fallback.dataset.view);
}

function switchView(target) {
  if (!viewMeta[target]) return;
  document.querySelectorAll(".menu-btn").forEach((btn) => {
    const active = btn.dataset.view === target;
    btn.classList.toggle("active", active);
  });
  document.querySelectorAll(".view").forEach((section) => {
    section.classList.toggle("active", section.id === `view-${target}`);
  });
  byId("viewTitle").textContent = viewMeta[target].title;
  byId("viewDescription").textContent = viewMeta[target].description;
  if (window.matchMedia("(max-width: 1024px)").matches) {
    setSidebarOpen(false);
  }
}

function setSidebarOpen(open) {
  state.sidebarOpen = Boolean(open);
  const sidebar = document.querySelector(".sidebar");
  const backdrop = byId("sidebarBackdrop");
  const toggle = byId("mobileMenuToggle");
  if (!sidebar || !backdrop || !toggle) return;
  sidebar.classList.toggle("open", state.sidebarOpen);
  backdrop.classList.toggle("show", state.sidebarOpen);
  toggle.textContent = state.sidebarOpen ? "✕" : "☰";
}

function updateConsoleButtons() {
  const dock = byId("consoleDock");
  const toggleBtn = byId("toggleConsoleBtn");
  const expandBtn = byId("expandConsoleBtn");
  const minimized = dock.classList.contains("minimized");
  toggleBtn.textContent = minimized ? "▴" : "▾";
  expandBtn.textContent = state.consoleExpanded ? "🗗" : "⤢";
}

function applyMode(mode, persist = true) {
  if (!MODE_CONFIG[mode]) return;
  state.mode = mode;
  if (persist) localStorage.setItem(MODE_STORAGE_KEY, mode);

  byId("currentModeBadge").textContent = MODE_CONFIG[mode].label;
  byId("currentModeText").textContent = MODE_CONFIG[mode].summary;

  document.querySelectorAll("[data-mode]").forEach((element) => {
    const visible = modeAllowsElement(element, mode);
    element.classList.toggle("hidden-by-mode", !visible);
    if (element.classList.contains("view") && !visible) {
      element.classList.remove("active");
    }
  });

  const terminalTab = document.querySelector('.console-tab[data-console="terminal"]');
  const terminalPanel = byId("terminalLogs");
  if (mode === "mode2" && terminalTab && terminalTab.classList.contains("active")) {
    terminalTab.classList.remove("active");
    terminalPanel.classList.remove("active");
    const logTab = document.querySelector('.console-tab[data-console="logs"]');
    const logPanel = byId("jobLogs");
    if (logTab) logTab.classList.add("active");
    if (logPanel) logPanel.classList.add("active");
  }

  ensureActiveViewVisible();
  ensureSocketsForMode();
}

function initMenu() {
  document.querySelectorAll(".menu-btn").forEach((btn) => {
    btn.addEventListener("click", () => switchView(btn.dataset.view));
  });
  on("mobileMenuToggle", "click", () => setSidebarOpen(!state.sidebarOpen));
  on("sidebarBackdrop", "click", () => setSidebarOpen(false));
}

function initModePicker() {
  const host = (location.hostname || "").toLowerCase();
  const isFirebaseHosting = host.endsWith(".web.app") || host.endsWith(".firebaseapp.com");
  const isWindows = (navigator.userAgent || "").includes("Windows");
  const suggestedMode = (isFirebaseHosting || isWindows) ? "mode2" : "mode1";
  const savedMode = localStorage.getItem(MODE_STORAGE_KEY);
  if (MODE_CONFIG[savedMode]) {
    applyMode(savedMode, false);
  } else {
    applyMode(suggestedMode, false);
  }

  const overlay = byId("modeOverlay");
  overlay.classList.add("show");

  byId("selectMode1Btn").addEventListener("click", () => {
    applyMode("mode1");
    overlay.classList.remove("show");
  });
  byId("selectMode2Btn").addEventListener("click", () => {
    applyMode("mode2");
    overlay.classList.remove("show");
  });
  byId("switchModeBtn").addEventListener("click", () => {
    overlay.classList.add("show");
  });
}

function initConsoleTabs() {
  document.querySelectorAll(".console-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      if (tab.classList.contains("hidden-by-mode")) return;
      const target = tab.dataset.console;
      document.querySelectorAll(".console-tab").forEach((button) => {
        button.classList.toggle("active", button === tab);
      });
      const panelId = target === "logs" ? "jobLogs" : "terminalLogs";
      byId("jobLogs").classList.toggle("active", panelId === "jobLogs");
      byId("terminalLogs").classList.toggle("active", panelId === "terminalLogs");
    });
  });
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(value, max));
}

function applyConsoleHeight(px) {
  const dock = byId("consoleDock");
  const value = clamp(Math.round(px), 160, 760);
  state.consoleHeight = value;
  dock.style.height = `${value}px`;
  localStorage.setItem(CONSOLE_HEIGHT_STORAGE_KEY, String(value));
}

function initConsoleDockControls() {
  const dock = byId("consoleDock");
  const handle = byId("consoleResizeHandle");
  const toggleBtn = byId("toggleConsoleBtn");
  const expandBtn = byId("expandConsoleBtn");
  const saved = Number(localStorage.getItem(CONSOLE_HEIGHT_STORAGE_KEY) || 280);
  applyConsoleHeight(saved);

  let dragging = false;
  let prevHeight = saved;

  handle.addEventListener("mousedown", (event) => {
    if (dock.classList.contains("minimized")) return;
    dragging = true;
    dock.classList.remove("expanded");
    state.consoleExpanded = false;
    event.preventDefault();
  });

  document.addEventListener("mousemove", (event) => {
    if (!dragging) return;
    const desired = window.innerHeight - event.clientY;
    applyConsoleHeight(desired);
  });

  document.addEventListener("mouseup", () => {
    dragging = false;
  });

  toggleBtn.addEventListener("click", () => {
    dock.classList.toggle("minimized");
    updateConsoleButtons();
  });

  expandBtn.addEventListener("click", () => {
    if (!state.consoleExpanded) {
      prevHeight = dock.offsetHeight || state.consoleHeight;
      state.consoleExpanded = true;
      dock.classList.remove("minimized");
      dock.classList.add("expanded");
      applyConsoleHeight(window.innerHeight * 0.58);
      updateConsoleButtons();
      return;
    }
    state.consoleExpanded = false;
    dock.classList.remove("expanded");
    applyConsoleHeight(prevHeight);
    updateConsoleButtons();
  });

  if (window.matchMedia("(max-width: 1024px)").matches) {
    dock.classList.add("minimized");
  }
  updateConsoleButtons();
}

function setConnectionText(elementId, text, tone) {
  const element = byId(elementId);
  if (!element) return;
  element.textContent = text;
  element.style.color = tone === "ok" ? "#8cffc4" : tone === "error" ? "#ffb5b5" : "#eef4ff";
}

async function refreshConnectionStatus() {
  if (!navigator.onLine) {
    setConnectionText("networkStatus", "오프라인", "error");
    setConnectionText("apiStatus", "연결 불가", "error");
    return;
  }
  setConnectionText("networkStatus", "온라인", "ok");
  try {
    await fetchJson("/api/health");
    setConnectionText("apiStatus", "정상", "ok");
  } catch (error) {
    setConnectionText("apiStatus", "오류", "error");
  }
}
async function loadTools() {
  const toolsTableBody = byId("toolsTable").querySelector("tbody");
  const toolIdSelect = byId("toolId");
  const data = await fetchJson("/api/tools");
  toolsTableBody.innerHTML = "";
  toolIdSelect.innerHTML = "";

  data.tools.forEach((tool) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(tool.name)} (${escapeHtml(tool.id)})</td>
      <td>${tool.available ? badge("ok") : badge("error")}</td>
      <td>${escapeHtml(tool.discovered_path || "-")}</td>
      <td>${escapeHtml(tool.reason || "-")}</td>
    `;
    toolsTableBody.appendChild(tr);

    const option = document.createElement("option");
    option.value = tool.id;
    option.textContent = `${tool.name} (${tool.id})${tool.available ? "" : " - 비활성"}`;
    option.disabled = !tool.available;
    toolIdSelect.appendChild(option);
  });
}

async function loadEnvironmentCheck() {
  const envSummary = byId("envSummary");
  const envItems = byId("envItems");
  envSummary.textContent = "환경 검사 중...";
  const data = await fetchJson("/api/environment/check");
  envSummary.textContent = `완료 - OK: ${data.summary.ok}, WARN: ${data.summary.warn}, ERROR: ${data.summary.error}`;

  envItems.innerHTML = "";
  data.items.forEach((item) => {
    const div = document.createElement("div");
    div.style.marginBottom = "8px";
    div.innerHTML = `${badge(item.status)} <strong>${escapeHtml(item.title)}</strong> - ${escapeHtml(item.message)}`;
    envItems.appendChild(div);
  });
}

async function loadJobs() {
  const jobsTableBody = byId("jobsTable").querySelector("tbody");
  const data = await fetchJson("/api/jobs");
  jobsTableBody.innerHTML = "";

  data.jobs.forEach((job) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td style="max-width:180px; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(job.id)}</td>
      <td>${escapeHtml(job.tool_id)}</td>
      <td>${badge(job.status)}</td>
      <td>${job.exit_code ?? "-"}</td>
    `;
    jobsTableBody.appendChild(tr);
  });

  const running = data.jobs.filter((item) => item.status === "running").length;
  const failed = data.jobs.filter((item) => item.status === "failed").length;
  byId("metricRunning").textContent = running;
  byId("metricFails").textContent = failed;
}

async function loadAdbDevices() {
  const deviceSelect = byId("deviceSelect");
  const serialInput = byId("serial");
  const firmwareSerialInput = byId("firmwareSerial");
  deviceSelect.innerHTML = "";

  const autoOption = document.createElement("option");
  autoOption.value = "";
  autoOption.textContent = "자동 선택(첫 device)";
  deviceSelect.appendChild(autoOption);

  try {
    const data = await fetchJson("/api/adb/devices");
    state.devices = data.devices || [];

    state.devices.forEach((item) => {
      const option = document.createElement("option");
      option.value = item.serial;
      const model = item.details?.model ? ` ${item.details.model}` : "";
      option.textContent = `${item.serial} [${item.state}]${model}`;
      option.disabled = item.state !== "device";
      deviceSelect.appendChild(option);
    });

    const firstOnline = state.devices.find((item) => item.state === "device");
    if (firstOnline) {
      if (!serialInput.value.trim()) serialInput.value = firstOnline.serial;
      if (!firmwareSerialInput.value.trim()) firmwareSerialInput.value = firstOnline.serial;
    }
  } catch (error) {
    const option = document.createElement("option");
    option.value = "";
    option.textContent = `adb 조회 실패: ${error.message}`;
    option.disabled = true;
    deviceSelect.appendChild(option);
  }
}

async function startJob() {
  const toolId = byId("toolId").value;
  const picked = byId("deviceSelect").value.trim();
  const serial = (picked || byId("serial").value.trim());
  const extraArgsRaw = byId("extraArgs").value.trim();
  const extraArgs = extraArgsRaw ? extraArgsRaw.split(/\s+/) : [];

  if (!toolId) {
    alert("도구 ID를 입력하세요.");
    return;
  }
  await fetchJson("/api/jobs/start", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      tool_id: toolId,
      serial,
      extra_args: extraArgs,
    }),
  });
  await loadJobs();
}

async function uploadFirmware() {
  const fileInput = byId("firmwareFile");
  const resultBox = byId("firmwareResult");
  if (!fileInput.files.length) {
    alert("펌웨어 파일을 선택하세요.");
    return;
  }

  const form = new FormData();
  form.append("firmware_file", fileInput.files[0]);
  form.append("serial", byId("firmwareSerial").value.trim());
  form.append("remote_path", byId("remotePath").value.trim());
  resultBox.textContent = "업로드 중...";

  try {
    const data = await fetchJson("/api/firmware/upload", { method: "POST", body: form });
    resultBox.textContent = `완료: exit_code=${data.adb_result.exit_code}`;
  } catch (error) {
    resultBox.textContent = `실패: ${error.message}`;
  }
}

function renderParsedMeta(parsed) {
  byId("parsedMeta").textContent = [
    `source_type: ${parsed.source_type}`,
    `firmware_version: ${parsed.firmware_version}`,
    `tool_version: ${parsed.tool_version}`,
    `elapsed_time: ${parsed.elapsed_time}`,
    `total_count: ${parsed.total_count}`,
    `pass_count: ${parsed.pass_count}`,
    `fail_count: ${parsed.fail_count}`,
  ].join("\n");
}

function renderFailedItems(parsed) {
  const tableBody = byId("failedTable").querySelector("tbody");
  tableBody.innerHTML = "";
  if (!parsed.failed_items || !parsed.failed_items.length) {
    const tr = document.createElement("tr");
    tr.innerHTML = "<td colspan='3'>Fail 항목 없음</td>";
    tableBody.appendChild(tr);
    return;
  }
  parsed.failed_items.forEach((item) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(item.module || "-")}</td>
      <td>${escapeHtml(item.testcase || "-")}</td>
      <td>${escapeHtml(item.reason || "-")}</td>
    `;
    tableBody.appendChild(tr);
  });
}

async function parseAndStoreResult() {
  const fileInput = byId("resultFile");
  const messageBox = byId("parseResultMessage");
  if (!fileInput.files.length) {
    alert("결과 파일을 선택하세요.");
    return;
  }

  const form = new FormData();
  form.append("result_file", fileInput.files[0]);
  form.append("source_type", byId("sourceType").value);
  form.append("save_full", "true");
  form.append("firmware_version", byId("overrideFirmware").value.trim());
  form.append("tool_version", byId("overrideTool").value.trim());
  form.append("elapsed_time", byId("overrideElapsed").value.trim());

  messageBox.textContent = "결과서 분석 중...";
  try {
    const data = await fetchJson("/api/reports/import-file", {
      method: "POST",
      body: form,
    });
    state.parsedResult = data;
    renderParsedMeta(data.parsed);
    renderFailedItems(data.parsed);
    messageBox.textContent = `분석 완료 (run_id=${data.run_id ?? "미저장"})`;
    await loadRuns();
    await loadDashboard();
  } catch (error) {
    messageBox.textContent = `분석 실패: ${error.message}`;
  }
}

async function autoUploadParsedResult() {
  const resultBox = byId("reportResultBox");
  if (!state.parsedResult) {
    alert("먼저 결과서를 분석하세요.");
    return;
  }

  const parsed = state.parsedResult.parsed;
  const payload = {
    test_name: `AUTO-${parsed.source_type.toUpperCase()}-${parsed.firmware_version}`,
    device_serial: "AUTO-PARSED",
    summary: [
      `[자동요약] firmware=${parsed.firmware_version}`,
      `tool=${parsed.tool_version}`,
      `elapsed=${parsed.elapsed_time}`,
      `fail_count=${parsed.fail_count}/${parsed.total_count}`,
    ].join("\n"),
    result: parsed.fail_count > 0 ? "FAIL" : "PASS",
    issue_links: [],
    metadata: {
      source_type: parsed.source_type,
      run_id: state.parsedResult.run_id,
      firmware_version: parsed.firmware_version,
      tool_version: parsed.tool_version,
      elapsed_time: parsed.elapsed_time,
      fail_count: parsed.fail_count,
      total_count: parsed.total_count,
    },
  };

  const data = await fetchJson("/api/reports/upload", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  resultBox.textContent = JSON.stringify(data.result.results, null, 2);
}

async function loadRuns() {
  const tableBody = byId("runsTable").querySelector("tbody");
  const data = await fetchJson("/api/reports/runs?limit=100");
  tableBody.innerHTML = "";

  data.runs.forEach((run) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><button class="link-btn" data-run-id="${run.id}">${run.id}</button></td>
      <td>${escapeHtml(run.source_file)}</td>
      <td>${escapeHtml(run.firmware_version || "-")}</td>
      <td>${run.fail_count}</td>
    `;
    tableBody.appendChild(tr);
  });

  tableBody.querySelectorAll(".link-btn").forEach((btn) => {
    btn.addEventListener("click", () => loadRunDetail(btn.dataset.runId));
  });
}

async function loadRunDetail(runId) {
  const detailBox = byId("runDetail");
  const data = await fetchJson(`/api/reports/runs/${runId}`);
  const head = [
    `run_id: ${data.id}`,
    `file: ${data.source_file}`,
    `firmware: ${data.firmware_version}`,
    `tool: ${data.tool_version}`,
    `elapsed: ${data.elapsed_time}`,
    `total: ${data.total_count}, pass: ${data.pass_count}, fail: ${data.fail_count}`,
    "",
  ].join("\n");

  const caseLines = data.cases.slice(0, 400).map((item) => {
    return `[${item.result}] ${item.module_name} :: ${item.testcase_name}`;
  });
  detailBox.textContent = head + caseLines.join("\n");
}
function destroyChart(name) {
  if (state.charts[name]) {
    state.charts[name].destroy();
    state.charts[name] = null;
  }
}

function renderLineChart(canvasId, chartName, labels, datasets) {
  if (!window.Chart) return;
  destroyChart(chartName);
  const canvas = byId(canvasId);
  state.charts[chartName] = new Chart(canvas, {
    type: "line",
    data: { labels, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "bottom" } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

function renderBarChart(canvasId, chartName, labels, values, label, color) {
  if (!window.Chart) return;
  destroyChart(chartName);
  const canvas = byId(canvasId);
  state.charts[chartName] = new Chart(canvas, {
    type: "bar",
    data: {
      labels,
      datasets: [{ label, data: values, backgroundColor: color }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

function renderPieChart(canvasId, chartName, labels, values, chartLabel) {
  if (!window.Chart) return;
  destroyChart(chartName);
  const palette = ["#1459ff", "#00a56a", "#ef6820", "#7f56d9", "#dd2590", "#1f6f8b"];
  const canvas = byId(canvasId);
  state.charts[chartName] = new Chart(canvas, {
    type: "pie",
    data: {
      labels,
      datasets: [{ label: chartLabel, data: values, backgroundColor: palette }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "bottom" } },
    },
  });
}

function renderDoughnutChart(canvasId, chartName, labels, values, chartLabel) {
  if (!window.Chart) return;
  destroyChart(chartName);
  const palette = ["#0d9488", "#ef4444", "#f59e0b", "#64748b"];
  const canvas = byId(canvasId);
  state.charts[chartName] = new Chart(canvas, {
    type: "doughnut",
    data: {
      labels,
      datasets: [{ label: chartLabel, data: values, backgroundColor: palette }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "bottom" } },
      cutout: "55%",
    },
  });
}

function renderRadarChart(canvasId, chartName, labels, values, chartLabel) {
  if (!window.Chart) return;
  destroyChart(chartName);
  const canvas = byId(canvasId);
  state.charts[chartName] = new Chart(canvas, {
    type: "radar",
    data: {
      labels,
      datasets: [{
        label: chartLabel,
        data: values,
        borderColor: "#1459ff",
        backgroundColor: "#1459ff3d",
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        r: {
          beginAtZero: true,
          suggestedMax: 100,
        },
      },
    },
  });
}

async function loadDashboard() {
  const data = await fetchJson("/api/analytics/dashboard");
  const mock = data.mock;
  const stored = data.stored;

  const fwSeries = mock.firmware_timeline_line.series.map((series, index) => {
    const colors = ["#1459ff", "#00a56a", "#ef6820", "#7f56d9", "#dd2590"];
    return {
      label: series.name,
      data: series.values,
      borderColor: colors[index % colors.length],
      backgroundColor: `${colors[index % colors.length]}33`,
      fill: false,
      tension: 0.2,
    };
  });

  renderLineChart(
    "chartFirmwareLine",
    "firmware",
    mock.firmware_timeline_line.labels,
    fwSeries,
  );

  renderLineChart(
    "chartFailTrendArea",
    "failTrend",
    mock.cts_fail_trend_area.labels,
    [{
      label: "CTS Fail",
      data: mock.cts_fail_trend_area.values,
      borderColor: "#b42318",
      backgroundColor: "#b4231844",
      fill: true,
      tension: 0.3,
    }],
  );

  renderBarChart(
    "chartUploadBar",
    "uploadTrend",
    mock.upload_count_bar.labels,
    mock.upload_count_bar.values,
    "업로드 횟수",
    "#1459ff",
  );

  renderPieChart(
    "chartToolPie",
    "toolPie",
    mock.tool_share_pie.labels,
    mock.tool_share_pie.values,
    "도구 비중",
  );

  renderDoughnutChart(
    "chartResultDoughnut",
    "resultDoughnut",
    mock.result_distribution_doughnut.labels,
    mock.result_distribution_doughnut.values,
    "결과 분포",
  );

  renderRadarChart(
    "chartSuiteRadar",
    "suiteRadar",
    mock.suite_radar.labels,
    mock.suite_radar.values,
    "품질 지수",
  );

  const uploadTotal = (stored.monthly_upload || []).reduce((acc, item) => acc + Number(item.upload_count || 0), 0);
  byId("metricUploads").textContent = uploadTotal;
}

async function loadFirebaseStatus() {
  const summary = byId("firebaseStatusSummary");
  const detail = byId("firebaseStatusDetail");
  summary.textContent = "Firebase 상태 확인 중...";
  try {
    const data = await fetchJson("/api/firebase/status");
    const stateText = data.ok ? "정상 연결" : "미연결/오류";
    summary.textContent = `backend=${data.backend}, project=${data.project_id || "-"}, collection=${data.default_collection} (${stateText})`;
    detail.textContent = JSON.stringify(data, null, 2);
  } catch (error) {
    summary.textContent = `Firebase 상태 확인 실패: ${error.message}`;
    detail.textContent = "";
  }
}

async function loadWatcherStatus() {
  const summary = byId("watcherStatusSummary");
  const detail = byId("watcherStatusDetail");
  const eventsBox = byId("watcherEvents");
  summary.textContent = "Watcher 상태 확인 중...";
  try {
    const data = await fetchJson("/api/watcher/status");
    summary.textContent = `enabled=${data.enabled}, running=${data.running}, imported_total=${data.imported_total}, events=${data.events_count || 0}, last_scan_at=${data.last_scan_at || "-"}`;
    detail.textContent = JSON.stringify(data, null, 2);
    const events = await fetchJson("/api/watcher/events?limit=30");
    const lines = (events.events || []).map((item) => {
      return `[${item.level}] ${item.time_utc} :: ${item.message}`;
    });
    eventsBox.textContent = lines.length ? lines.join("\n") : "이벤트 없음";
  } catch (error) {
    summary.textContent = `Watcher 상태 확인 실패: ${error.message}`;
    detail.textContent = "";
    eventsBox.textContent = "";
  }
}

async function controlWatcher(action) {
  const summary = byId("watcherStatusSummary");
  summary.textContent = `Watcher ${action} 실행 중...`;
  try {
    const endpointMap = {
      enable: "/api/watcher/enable",
      disable: "/api/watcher/disable",
      scan: "/api/watcher/scan-now",
    };
    await fetchJson(endpointMap[action], { method: "POST" });
    await loadWatcherStatus();
  } catch (error) {
    summary.textContent = `Watcher ${action} 실패: ${error.message}`;
  }
}

async function syncFirebaseRuns() {
  const summary = byId("firebaseStatusSummary");
  const detail = byId("firebaseStatusDetail");
  summary.textContent = "결과 동기화 실행 중...";
  try {
    const data = await fetchJson("/api/firebase/sync/runs?limit=50", { method: "POST" });
    summary.textContent = `결과 동기화 완료: ${data.synced}건 -> ${data.collection}`;
    detail.textContent = JSON.stringify(data, null, 2);
    await loadFirebaseStatus();
  } catch (error) {
    summary.textContent = `결과 동기화 실패: ${error.message}`;
  }
}

async function syncFirebaseMonitor() {
  const summary = byId("firebaseStatusSummary");
  const detail = byId("firebaseStatusDetail");
  summary.textContent = "모니터링 동기화 실행 중...";
  try {
    const data = await fetchJson("/api/firebase/sync/monitor", { method: "POST" });
    summary.textContent = `모니터링 동기화 완료 -> ${data.collection}`;
    detail.textContent = JSON.stringify(data, null, 2);
    await loadFirebaseStatus();
  } catch (error) {
    summary.textContent = `모니터링 동기화 실패: ${error.message}`;
  }
}
function initLogSocket() {
  if (state.wsLogs) return;
  const jobLogs = byId("jobLogs");
  state.wsLogs = new WebSocket(`${getWsBase()}/ws/logs`);
  state.wsLogs.onmessage = (event) => {
    jobLogs.textContent += `${event.data}\n`;
    jobLogs.scrollTop = jobLogs.scrollHeight;
  };
  state.wsLogs.onclose = () => {
    state.wsLogs = null;
  };
}

function initTerminalSocket() {
  if (state.wsTerminal || state.mode === "mode2") return;
  const terminalLogs = byId("terminalLogs");
  state.wsTerminal = new WebSocket(`${getWsBase()}/ws/terminal`);
  state.wsTerminal.onmessage = (event) => {
    terminalLogs.textContent += event.data;
    terminalLogs.scrollTop = terminalLogs.scrollHeight;
  };
  state.wsTerminal.onclose = () => {
    state.wsTerminal = null;
  };
}

function closeTerminalSocket() {
  if (!state.wsTerminal) return;
  try {
    state.wsTerminal.close();
  } catch (error) {
    console.warn(error);
  }
  state.wsTerminal = null;
}

function ensureSocketsForMode() {
  initLogSocket();
  if (state.mode === "mode1") {
    initTerminalSocket();
    return;
  }
  closeTerminalSocket();
}

function sendTerminalCommand() {
  const input = byId("terminalInput");
  const value = input.value.trim();
  if (!value || !state.wsTerminal || state.wsTerminal.readyState !== WebSocket.OPEN) return;
  state.wsTerminal.send(value);
  input.value = "";
}

function on(id, eventName, handler) {
  const element = byId(id);
  if (element) element.addEventListener(eventName, handler);
}

function bindEvents() {
  on("refreshToolsBtn", "click", () => loadTools());
  on("checkEnvBtn", "click", () => loadEnvironmentCheck());
  on("startJobBtn", "click", () => startJob());
  on("refreshJobsBtn", "click", () => loadJobs());
  on("refreshDevicesBtn", "click", () => loadAdbDevices());
  on("refreshFirebaseStatusBtn", "click", () => loadFirebaseStatus());
  on("syncRunsBtn", "click", () => syncFirebaseRuns());
  on("syncMonitorBtn", "click", () => syncFirebaseMonitor());
  on("refreshWatcherStatusBtn", "click", () => loadWatcherStatus());
  on("enableWatcherBtn", "click", () => controlWatcher("enable"));
  on("disableWatcherBtn", "click", () => controlWatcher("disable"));
  on("scanWatcherNowBtn", "click", () => controlWatcher("scan"));
  on("uploadFirmwareBtn", "click", () => uploadFirmware());
  on("parseResultBtn", "click", () => parseAndStoreResult());
  on("autoUploadBtn", "click", () => autoUploadParsedResult());
  on("refreshRunsBtn", "click", () => loadRuns());
  on("sendTerminalBtn", "click", () => sendTerminalCommand());
  on("openFirebaseCenterBtn", "click", () => switchView("firebase"));

  on("deviceSelect", "change", (event) => {
    const value = String(event.target.value || "").trim();
    if (value) {
      byId("serial").value = value;
      byId("firmwareSerial").value = value;
    }
  });

  on("terminalInput", "keydown", (event) => {
    if (event.key === "Enter") sendTerminalCommand();
  });

  on("saveApiBaseBtn", "click", async () => {
    const value = byId("apiBaseUrl").value.trim();
    if (value) localStorage.setItem(API_BASE_STORAGE_KEY, value);
    await refreshConnectionStatus();
    alert("API 주소를 저장했습니다.");
  });

  on("clearApiBaseBtn", "click", async () => {
    localStorage.removeItem(API_BASE_STORAGE_KEY);
    byId("apiBaseUrl").value = "";
    await refreshConnectionStatus();
    alert("API 주소를 지웠습니다.");
  });
}

async function safeRun(label, runner) {
  try {
    await runner();
  } catch (error) {
    console.warn(`[init] ${label} failed`, error);
  }
}

async function init() {
  byId("apiBaseUrl").value = getApiBase();

  initMenu();
  initModePicker();
  initConsoleTabs();
  initConsoleDockControls();
  bindEvents();

  const mq = window.matchMedia("(max-width: 1024px)");
  const onScreenMode = (event) => {
    if (!event.matches) {
      setSidebarOpen(false);
      byId("consoleDock").classList.remove("minimized");
      updateConsoleButtons();
      return;
    }
    setSidebarOpen(false);
    byId("consoleDock").classList.add("minimized");
    updateConsoleButtons();
  };
  if (typeof mq.addEventListener === "function") {
    mq.addEventListener("change", onScreenMode);
  } else if (typeof mq.addListener === "function") {
    mq.addListener(onScreenMode);
  }

  await safeRun("network", refreshConnectionStatus);
  window.addEventListener("online", refreshConnectionStatus);
  window.addEventListener("offline", refreshConnectionStatus);
  setInterval(refreshConnectionStatus, 15000);

  await Promise.all([
    safeRun("tools", loadTools),
    safeRun("adb", loadAdbDevices),
    safeRun("firebase", loadFirebaseStatus),
    safeRun("watcher", loadWatcherStatus),
    safeRun("jobs", loadJobs),
    safeRun("environment", loadEnvironmentCheck),
    safeRun("runs", loadRuns),
    safeRun("dashboard", loadDashboard),
  ]);

  ensureSocketsForMode();
}

init().catch((error) => {
  console.error(error);
  alert(`초기화 실패: ${error.message}`);
});
