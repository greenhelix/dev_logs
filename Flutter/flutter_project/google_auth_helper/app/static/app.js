const byId = (id) => document.getElementById(id);

const state = {
  wsLogs: null,
  wsTerminal: null,
  parsedResult: null,
  charts: {},
};

const viewMeta = {
  dashboard: {
    title: "대시보드",
    description: "FW 인증 추이, Fail 감소 추이, 업로드 추이를 확인합니다.",
  },
  runner: {
    title: "테스트 실행",
    description: "인증 도구 실행/작업 상태/펌웨어 업로드를 처리합니다.",
  },
  upload: {
    title: "결과서/업로드",
    description: "result.xml / result_failure.html 자동 분석 및 외부 업로드를 수행합니다.",
  },
  environment: {
    title: "환경 점검",
    description: "필수 환경과 권한 상태를 별도 화면에서 점검합니다.",
  },
  tools: {
    title: "도구 상태",
    description: "CTS/GTS/TVTS/VTS/STS/CTS-on-GSI 활성 상태를 확인합니다.",
  },
};

const escapeHtml = (value) =>
  String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");

const badge = (status) => {
  if (status === "ok" || status === "success" || status === true) return `<span class="badge ok">정상</span>`;
  if (status === "warn" || status === "queued" || status === "running") return `<span class="badge warn-badge">${status}</span>`;
  return `<span class="badge error">${status}</span>`;
};

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
  return response.json();
}

function switchView(target) {
  document.querySelectorAll(".menu-btn").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === target);
  });
  document.querySelectorAll(".view").forEach((section) => {
    section.classList.toggle("active", section.id === `view-${target}`);
  });
  byId("viewTitle").textContent = viewMeta[target].title;
  byId("viewDescription").textContent = viewMeta[target].description;
}

function initMenu() {
  document.querySelectorAll(".menu-btn").forEach((btn) => {
    btn.addEventListener("click", () => switchView(btn.dataset.view));
  });
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

async function startJob() {
  const toolId = byId("toolId").value;
  const serial = byId("serial").value.trim();
  const extraArgsRaw = byId("extraArgs").value.trim();
  const extraArgs = extraArgsRaw ? extraArgsRaw.split(/\s+/) : [];

  if (!toolId || !serial) {
    alert("도구 ID와 디바이스 시리얼을 입력하세요.");
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

function initConsoleTabs() {
  document.querySelectorAll(".console-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      const target = tab.dataset.console;
      document.querySelectorAll(".console-tab").forEach((button) => {
        button.classList.toggle("active", button === tab);
      });
      document.querySelectorAll(".console-panel").forEach((panel) => {
        panel.classList.toggle("active", panel.id === (target === "logs" ? "jobLogs" : "terminalLogs"));
      });
    });
  });
}

function initLogSocket() {
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  const jobLogs = byId("jobLogs");
  state.wsLogs = new WebSocket(`${protocol}://${location.host}/ws/logs`);
  state.wsLogs.onmessage = (event) => {
    jobLogs.textContent += `${event.data}\n`;
    jobLogs.scrollTop = jobLogs.scrollHeight;
  };
}

function initTerminalSocket() {
  const protocol = location.protocol === "https:" ? "wss" : "ws";
  const terminalLogs = byId("terminalLogs");
  state.wsTerminal = new WebSocket(`${protocol}://${location.host}/ws/terminal`);
  state.wsTerminal.onmessage = (event) => {
    terminalLogs.textContent += event.data;
    terminalLogs.scrollTop = terminalLogs.scrollHeight;
  };
}

function sendTerminalCommand() {
  const input = byId("terminalInput");
  const value = input.value.trim();
  if (!value || !state.wsTerminal || state.wsTerminal.readyState !== WebSocket.OPEN) return;
  state.wsTerminal.send(value);
  input.value = "";
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

function bindEvents() {
  byId("refreshToolsBtn").addEventListener("click", loadTools);
  byId("checkEnvBtn").addEventListener("click", loadEnvironmentCheck);
  byId("startJobBtn").addEventListener("click", startJob);
  byId("refreshJobsBtn").addEventListener("click", loadJobs);
  byId("uploadFirmwareBtn").addEventListener("click", uploadFirmware);
  byId("parseResultBtn").addEventListener("click", parseAndStoreResult);
  byId("autoUploadBtn").addEventListener("click", autoUploadParsedResult);
  byId("refreshRunsBtn").addEventListener("click", loadRuns);
  byId("sendTerminalBtn").addEventListener("click", sendTerminalCommand);

  byId("terminalInput").addEventListener("keydown", (event) => {
    if (event.key === "Enter") sendTerminalCommand();
  });
}

async function init() {
  initMenu();
  initConsoleTabs();
  bindEvents();
  await Promise.all([
    loadTools(),
    loadJobs(),
    loadEnvironmentCheck(),
    loadRuns(),
    loadDashboard(),
  ]);
  initLogSocket();
  initTerminalSocket();
}

init().catch((error) => {
  console.error(error);
  alert(`초기화 실패: ${error.message}`);
});
