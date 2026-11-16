#!/usr/bin/env python3
"""接口自动化测试执行引擎。

用法: python runner.py <input.json> <output.json>

输入格式:
{
  "runId": "...",
  "cases": [
    {
      "caseId": 1, "name": "示例", "method": "GET", "url": "https://httpbin.org/get",
      "headersJson": "{\"X-Token\":\"abc\"}", "body": "",
      "assertionsJson": "[{\"type\":\"status\",\"op\":\"eq\",\"value\":200}]",
      "timeoutSeconds": 10
    }
  ]
}

输出格式:
{
  "runId": "...",
  "results": [
    {"caseId": 1, "name": "示例", "success": true, "status": 200,
     "durationMs": 123, "responsePreview": "...", "message": ""}
  ]
}

仅使用 Python 标准库，无第三方依赖。
"""

import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


def load_headers(headers_json):
    try:
        headers = json.loads(headers_json or "{}")
        return headers if isinstance(headers, dict) else {}
    except json.JSONDecodeError:
        return {}


def load_assertions(assertions_json):
    try:
        items = json.loads(assertions_json or "[]")
        return items if isinstance(items, list) else []
    except json.JSONDecodeError:
        return []


def json_get(data, path):
    """按 'a.b.0.c' 的点路径取嵌套 JSON 值。"""
    cur = data
    for part in path.split("."):
        if isinstance(cur, list):
            try:
                cur = cur[int(part)]
            except (ValueError, IndexError):
                return None, "路径 %s 索引无效" % path
        elif isinstance(cur, dict):
            if part not in cur:
                return None, "路径 %s 字段不存在" % path
            cur = cur[part]
        else:
            return None, "路径 %s 无法继续下钻" % path
    return cur, None


def check_assertion(rule, status, body, parsed):
    """执行单条断言，返回 None 表示通过，否则返回失败原因。"""
    atype = rule.get("type")
    op = rule.get("op", "eq")
    value = rule.get("value")

    if atype == "status":
        if op == "eq" and status != value:
            return "状态码断言失败: 期望 %s 实际 %s" % (value, status)
        if op == "lt" and not (status < value):
            return "状态码断言失败: 期望 <%s 实际 %s" % (value, status)
        return None

    if atype == "body_contains":
        if str(value) not in (body or ""):
            return "响应体不包含: %r" % value
        return None

    if atype == "json_field":
        path = rule.get("path", "")
        actual, err = json_get(parsed, path) if parsed is not None else (None, "响应不是合法 JSON")
        if err:
            return err
        if op == "eq" and str(actual) != str(value):
            return "字段 %s 断言失败: 期望 %r 实际 %r" % (path, value, actual)
        if op == "exists" and actual is None:
            return "字段 %s 不存在" % path
        if op == "contains" and str(value) not in str(actual):
            return "字段 %s 不包含 %r" % (path, value)
        return None

    return "未知断言类型: %s" % atype


def run_case(case):
    method = (case.get("method") or "GET").upper()
    url = case.get("url") or ""
    timeout = min(int(case.get("timeoutSeconds") or 10), 60)
    headers = load_headers(case.get("headersJson"))
    body = case.get("body") or ""
    assertions = load_assertions(case.get("assertionsJson"))

    result = {
        "caseId": case.get("caseId"),
        "name": case.get("name"),
        "success": False,
        "status": None,
        "durationMs": None,
        "responsePreview": "",
        "message": "",
    }

    data = None
    if body and method in ("POST", "PUT", "PATCH", "DELETE"):
        data = body.encode("utf-8")
        headers.setdefault("Content-Type", "application/json")

    started = time.time()
    try:
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            status = resp.status
            raw = resp.read(65536).decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        status = e.code
        raw = e.read(65536).decode("utf-8", errors="replace")
    except Exception as e:  # 网络错误、超时等
        result["durationMs"] = int((time.time() - started) * 1000)
        result["message"] = "请求失败: %s" % e
        return result

    result["durationMs"] = int((time.time() - started) * 1000)
    result["status"] = status
    result["responsePreview"] = raw[:1000]

    try:
        parsed = json.loads(raw) if raw.strip().startswith(("{", "[")) else None
    except json.JSONDecodeError:
        parsed = None

    failures = []
    for rule in assertions:
        reason = check_assertion(rule, status, raw, parsed)
        if reason:
            failures.append(reason)

    if failures:
        result["message"] = "; ".join(failures)
    else:
        result["success"] = True
    return result


def main():
    if len(sys.argv) != 3:
        print("用法: python runner.py <input.json> <output.json>", file=sys.stderr)
        sys.exit(2)

    with open(sys.argv[1], "r", encoding="utf-8") as f:
        payload = json.load(f)

    results = [run_case(c) for c in payload.get("cases", [])]

    out = {"runId": payload.get("runId"), "results": results}
    with open(sys.argv[2], "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False)


if __name__ == "__main__":
    main()
