/* 接口自动化测试平台前端逻辑（原生 JS，无框架依赖） */
let token = localStorage.getItem("token") || "";
let username = localStorage.getItem("username") || "";
let authMode = "login";
let editingCaseId = null;
let editingSuiteId = null;
let runPollTimer = null;

const $ = (id) => document.getElementById(id);

/* ---------- 通用请求 ---------- */
async function api(path, options = {}) {
  const opts = { headers: { "Content-Type": "application/json" }, ...options };
  if (token) opts.headers["Authorization"] = "Bearer " + token;
  const res = await fetch(path, opts);
  if (res.status === 401) { showAuth(); throw new Error("登录已过期"); }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error((data && data.error) || ("请求失败 " + res.status));
  return data;
}

function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.style.display = "block";
  setTimeout(() => (t.style.display = "none"), 2000);
}

function closeModal(id) { $(id).style.display = "none"; }
function escapeHtml(s) {
  return (s || "").replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

/* ---------- 登录 / 注册 ---------- */
function showAuth() {
  $("authView").classList.remove("hidden");
  $("mainView").classList.add("hidden");
  $("topbar").classList.add("hidden");
}
function showMain() {
  $("authView").classList.add("hidden");
  $("mainView").classList.remove("hidden");
  $("topbar").classList.remove("hidden");
  $("whoami").textContent = "当前用户：" + username;
  loadCases();
  loadSuites();
  loadRuns();
}

$("authSwitchLink").onclick = () => {
  authMode = authMode === "login" ? "register" : "login";
  $("authTitle").textContent = authMode === "login" ? "登录" : "注册";
  $("authSubmit").textContent = authMode === "login" ? "登录" : "注册";
  $("authSwitchText").textContent = authMode === "login" ? "还没有账号？" : "已有账号？";
  $("authSwitchLink").textContent = authMode === "login" ? "立即注册" : "去登录";
  $("authError").textContent = "";
};

$("authSubmit").onclick = async () => {
  $("authError").textContent = "";
  try {
    const data = await api("/api/auth/" + authMode, {
      method: "POST",
      body: JSON.stringify({
        username: $("authUsername").value.trim(),
        password: $("authPassword").value,
      }),
    });
    token = data.token;
    username = data.username;
    localStorage.setItem("token", token);
    localStorage.setItem("username", username);
    showMain();
  } catch (e) {
    $("authError").textContent = e.message;
  }
};

$("logoutBtn").onclick = () => {
  token = ""; username = "";
  localStorage.removeItem("token");
  localStorage.removeItem("username");
  showAuth();
};

/* ---------- Tab 切换 ---------- */
document.querySelectorAll("nav.tabs button").forEach((btn) => {
  btn.onclick = () => {
    document.querySelectorAll("nav.tabs button").forEach((b) => b.classList.remove("active"));
    btn.classList.add("active");
    ["cases", "suites", "runs"].forEach((t) =>
      $("tab-" + t).classList.toggle("hidden", t !== btn.dataset.tab));
    if (btn.dataset.tab === "runs") loadRuns();
  };
});

/* ---------- 用例 ---------- */
async function loadCases() {
  const cases = await api("/api/cases");
  const tbody = $("caseTbody");
  tbody.innerHTML = cases.length
    ? cases.map((c) => `
      <tr>
        <td>${escapeHtml(c.name)}</td>
        <td><span class="method ${c.method}">${c.method}</span></td>
        <td style="max-width:280px;word-break:break-all">${escapeHtml(c.url)}</td>
        <td class="muted">${JSON.parse(c.assertions || "[]").length} 条</td>
        <td>
          <button class="ghost small" onclick="editCase(${c.id},'${escapeHtml(c.name).replace(/'/g, "\\'")}','${c.method}','${escapeHtml(c.url).replace(/'/g, "\\'")}')">编辑</button>
          <button class="danger small" onclick="deleteCase(${c.id})">删除</button>
        </td>
      </tr>`).join("")
    : '<tr><td colspan="5" class="muted">暂无用例，点击右上角“新建用例”</td></tr>';
  window._cases = cases;
}

$("newCaseBtn").onclick = () => {
  editingCaseId = null;
  $("caseModalTitle").textContent = "新建用例";
  ["f_name", "f_url", "f_headers", "f_body", "f_assertions"].forEach((id) => ($(id).value = ""));
  $("f_method").value = "GET";
  $("f_timeout").value = "10";
  $("caseError").textContent = "";
  $("caseModal").style.display = "flex";
};

window.editCase = (id, name, method, url) => {
  const c = window._cases.find((x) => x.id === id);
  editingCaseId = id;
  $("caseModalTitle").textContent = "编辑用例";
  $("f_name").value = name;
  $("f_method").value = method;
  $("f_url").value = url;
  $("f_headers").value = c.headers || "{}";
  $("f_body").value = c.body || "";
  $("f_assertions").value = c.assertions || "[]";
  $("f_timeout").value = c.timeoutSeconds || 10;
  $("caseError").textContent = "";
  $("caseModal").style.display = "flex";
};

$("caseSaveBtn").onclick = async () => {
  $("caseError").textContent = "";
  const body = {
    name: $("f_name").value.trim(),
    method: $("f_method").value,
    url: $("f_url").value.trim(),
    headers: $("f_headers").value,
    body: $("f_body").value,
    assertions: $("f_assertions").value,
    timeoutSeconds: parseInt($("f_timeout").value) || 10,
  };
  try {
    await api(editingCaseId ? "/api/cases/" + editingCaseId : "/api/cases", {
      method: editingCaseId ? "PUT" : "POST",
      body: JSON.stringify(body),
    });
    closeModal("caseModal");
    toast("已保存");
    loadCases();
  } catch (e) {
    $("caseError").textContent = e.message;
  }
};

window.deleteCase = async (id) => {
  if (!confirm("确认删除该用例？")) return;
  await api("/api/cases/" + id, { method: "DELETE" });
  toast("已删除");
  loadCases();
};

/* ---------- 用例集 ---------- */
async function loadSuites() {
  const [suites, runs] = await Promise.all([api("/api/suites"), api("/api/runs")]);
  const lastRunBySuite = {};
  runs.forEach((r) => { if (r.suite && !lastRunBySuite[r.suite.id]) lastRunBySuite[r.suite.id] = r; });
  const tbody = $("suiteTbody");
  tbody.innerHTML = suites.length
    ? suites.map((s) => `
      <tr>
        <td>${escapeHtml(s.name)}<div class="muted">${escapeHtml(s.description || "")}</div></td>
        <td>${s.cases.length}</td>
        <td>${s.scheduleMinutes > 0 ? "每 " + s.scheduleMinutes + " 分钟" : '<span class="muted">关闭</span>'}</td>
        <td class="muted">${lastRunBySuite[s.id] ? new Date(lastRunBySuite[s.id].startedAt).toLocaleString() : "未执行"}</td>
        <td>
          <button class="small" onclick="runSuite(${s.id})">执行</button>
          <button class="ghost small" onclick="editSuite(${s.id})">编辑</button>
          <button class="danger small" onclick="deleteSuite(${s.id})">删除</button>
        </td>
      </tr>`).join("")
    : '<tr><td colspan="5" class="muted">暂无用例集</td></tr>';
  window._suites = suites;
}

$("newSuiteBtn").onclick = () => {
  editingSuiteId = null;
  $("suiteModalTitle").textContent = "新建用例集";
  $("s_name").value = "";
  $("s_schedule").value = "0";
  renderSuiteCasePicker([]);
  $("suiteError").textContent = "";
  $("suiteModal").style.display = "flex";
};

window.editSuite = (id) => {
  const s = window._suites.find((x) => x.id === id);
  editingSuiteId = id;
  $("suiteModalTitle").textContent = "编辑用例集";
  $("s_name").value = s.name;
  $("s_schedule").value = s.scheduleMinutes || 0;
  renderSuiteCasePicker(s.cases.map((c) => c.id));
  $("suiteError").textContent = "";
  $("suiteModal").style.display = "flex";
};

function renderSuiteCasePicker(selectedIds) {
  const box = $("s_cases");
  box.innerHTML = (window._cases || []).length
    ? window._cases.map((c) => `
      <label style="display:flex;gap:8px;align-items:center;margin-bottom:4px;color:var(--text)">
        <input type="checkbox" value="${c.id}" style="width:auto;margin:0"
          ${selectedIds.includes(c.id) ? "checked" : ""}>
        <span class="method ${c.method}">${c.method}</span> ${escapeHtml(c.name)}
      </label>`).join("")
    : '<span class="muted">还没有用例，请先创建用例</span>';
}

$("suiteSaveBtn").onclick = async () => {
  $("suiteError").textContent = "";
  const ids = [...document.querySelectorAll("#s_cases input:checked")].map((i) => parseInt(i.value));
  const body = {
    name: $("s_name").value.trim(),
    description: "",
    caseIds: ids,
    scheduleMinutes: parseInt($("s_schedule").value) || 0,
  };
  try {
    await api(editingSuiteId ? "/api/suites/" + editingSuiteId : "/api/suites", {
      method: editingSuiteId ? "PUT" : "POST",
      body: JSON.stringify(body),
    });
    closeModal("suiteModal");
    toast("已保存");
    loadSuites();
  } catch (e) {
    $("suiteError").textContent = e.message;
  }
};

window.deleteSuite = async (id) => {
  if (!confirm("确认删除该用例集？")) return;
  await api("/api/suites/" + id, { method: "DELETE" });
  toast("已删除");
  loadSuites();
};

window.runSuite = async (id) => {
  try {
    const r = await api("/api/suites/" + id + "/run", { method: "POST" });
    toast("已触发执行 #" + r.runId);
    document.querySelector('nav.tabs button[data-tab="runs"]').click();
    pollRun(r.runId);
  } catch (e) {
    toast(e.message);
  }
};

/* ---------- 执行记录 ---------- */
const statusBadge = (s) =>
  s === "SUCCESS" ? '<span class="badge ok">成功</span>'
  : s === "FAILED" ? '<span class="badge bad">失败</span>'
  : s === "RUNNING" ? '<span class="badge running">执行中</span>'
  : '<span class="badge error">错误</span>';

async function loadRuns() {
  const runs = await api("/api/runs");
  const tbody = $("runTbody");
  tbody.innerHTML = runs.length
    ? runs.map((r) => {
        const rate = r.totalCases ? Math.round((r.passedCases / r.totalCases) * 100) : 0;
        return `
        <tr>
          <td>${r.id}</td>
          <td>${r.suite ? escapeHtml(r.suite.name) : "-"}</td>
          <td>${statusBadge(r.status)}</td>
          <td>${r.passedCases} / ${r.totalCases}</td>
          <td>
            <div class="bar"><div class="${rate < 100 ? "fail" : ""}" style="width:${rate}%"></div></div>
            <span class="muted">${rate}%</span>
          </td>
          <td>${r.scheduled ? "定时" : "手动"}</td>
          <td class="muted">${new Date(r.startedAt).toLocaleString()}</td>
          <td><button class="ghost small" onclick="showRun(${r.id})">详情</button></td>
        </tr>`;
      }).join("")
    : '<tr><td colspan="8" class="muted">暂无执行记录，去“用例集”页触发一次执行</td></tr>';
  return runs;
}

$("refreshRunsBtn").onclick = () => loadRuns();

function pollRun(runId) {
  clearInterval(runPollTimer);
  let count = 0;
  runPollTimer = setInterval(async () => {
    const runs = await loadRuns();
    const run = runs.find((r) => r.id === runId);
    count++;
    if (count > 100 || (run && run.status !== "RUNNING")) {
      clearInterval(runPollTimer);
      showRun(runId);
    }
  }, 3000);
}

window.showRun = async (id) => {
  const run = await api("/api/runs/" + id);
  $("runModalTitle").textContent = "执行 #" + run.id + "（" + run.status + "）";
  $("runModalBody").innerHTML = run.results.length
    ? run.results.map((r) => `
      <div class="card" style="margin-bottom:8px;padding:10px 14px">
        <div style="display:flex;justify-content:space-between">
          <b>${r.success ? "✅" : "❌"} ${escapeHtml(r.caseName)}</b>
          <span class="muted">${r.responseStatus || "-"} · ${r.durationMs || "-"}ms</span>
        </div>
        ${r.message ? `<div class="error-msg" style="margin-top:6px">${escapeHtml(r.message)}</div>` : ""}
        ${r.responsePreview ? `<pre class="preview">${escapeHtml(r.responsePreview)}</pre>` : ""}
      </div>`).join("")
    : '<p class="muted">暂无结果明细</p>';
  $("runModal").style.display = "flex";
};

/* ---------- 启动 ---------- */
if (token) {
  showMain();
} else {
  showAuth();
}
