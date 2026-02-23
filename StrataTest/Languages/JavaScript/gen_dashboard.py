#!/usr/bin/env python3
"""Generate an HTML dashboard showing TS feature coverage and test status."""
import os
import re
import subprocess
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
os.chdir(SCRIPT_DIR)

# --- Collect test results ---
results = {}
try:
    out = subprocess.check_output(["bash", "run_js_analyze_laurel.sh"],
                                  stderr=subprocess.DEVNULL, text=True)
    for line in out.splitlines():
        m = re.match(r"(PASS|FAIL|XFAIL|XPASS): (\S+)", line)
        if m:
            results[m.group(2)] = m.group(1)
except subprocess.CalledProcessError:
    pass

# --- Collect known failure reasons ---
fail_reasons = {}
reason = ""
with open("known_failures.txt") as f:
    for line in f:
        line = line.strip()
        if line.startswith("#"):
            reason = line.lstrip("# ")
        elif line:
            fail_reasons[line] = reason

# --- Parse coverage markdown ---
features = []
category = ""
with open("TS_FEATURE_COVERAGE.md") as f:
    for line in f:
        m = re.match(r"## \d+\. (.+)", line)
        if m:
            category = m.group(1)
            continue
        if not line.startswith("|") or "Feature" in line or "---" in line:
            continue
        cols = [c.strip() for c in line.split("|")]
        if len(cols) < 5:
            continue
        feature = cols[1].replace("`", "").replace("\\|", "|")
        parser_col = cols[2]
        test_col = cols[3]

        parser_ok = "✅" in parser_col
        test_name = ""
        tm = re.search(r"(test_\w+)", test_col)
        if tm:
            test_name = tm.group(1)

        if test_name:
            test_status = results.get(test_name, "unknown")
        elif "needs test" in test_col:
            test_status = "needs_test"
        elif test_col.strip() in ("—", ""):
            test_status = "no_test"
        else:
            test_status = "no_test"

        reason = fail_reasons.get(test_name, "")
        filter_class = {
            "PASS": "pass", "XFAIL": "xfail", "FAIL": "fail",
            "needs_test": "needs_test", "no_test": "unsupported" if not parser_ok else "needs_test",
        }.get(test_status, "needs_test")

        features.append({
            "cat": category, "feature": feature, "parser": parser_ok,
            "test": test_name, "status": test_status, "reason": reason,
            "filter": filter_class,
        })

# --- Stats ---
n_pass = sum(1 for f in features if f["status"] == "PASS")
n_xfail = sum(1 for f in features if f["status"] == "XFAIL")
n_needs = sum(1 for f in features if f["filter"] == "needs_test")
n_parser = sum(1 for f in features if f["parser"])
n_unsup = sum(1 for f in features if not f["parser"])
n_total = len(features)

# --- Generate HTML ---
def badge(cls, text):
    return f'<span class="badge {cls}">{text}</span>'

rows_by_cat = {}
for f in features:
    rows_by_cat.setdefault(f["cat"], []).append(f)

tables = ""
for cat, rows in rows_by_cat.items():
    tables += f'<div class="category"><h2>{cat}</h2><table>\n'
    tables += '<tr><th>Feature</th><th>Parser</th><th>Test</th><th>Status</th></tr>\n'
    for f in rows:
        pb = badge("supported", "✅") if f["parser"] else badge("unsupported", "❌")
        tn = f'<span class="test-name">{f["test"]}</span>' if f["test"] else ""
        sb = {
            "PASS": badge("pass", "✅ PASS"),
            "XFAIL": badge("xfail", "⚠ XFAIL") + (f'<span class="reason">{f["reason"]}</span>' if f["reason"] else ""),
            "FAIL": badge("fail", "❌ FAIL"),
            "needs_test": badge("needs-test", "needs test"),
        }.get(f["status"], badge("no-test", "—"))
        tables += f'<tr data-status="{f["filter"]}"><td>{f["feature"]}</td><td>{pb}</td><td>{tn}</td><td>{sb}</td></tr>\n'
    tables += '</table></div>\n'

html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>TypeScript Feature Coverage — Strata</title>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: #f5f5f5; color: #333; padding: 20px; max-width: 1100px; margin: 0 auto; }}
  h1 {{ margin-bottom: 8px; }}
  .subtitle {{ color: #666; margin-bottom: 20px; font-size: 14px; }}
  .stats {{ display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap; }}
  .stat {{ background: white; border-radius: 8px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
          min-width: 120px; text-align: center; }}
  .stat .num {{ font-size: 28px; font-weight: 700; }}
  .stat .label {{ font-size: 12px; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }}
  .stat.green .num {{ color: #22863a; }}
  .stat.orange .num {{ color: #e36209; }}
  .stat.red .num {{ color: #cb2431; }}
  .category {{ margin-bottom: 24px; }}
  .category h2 {{ font-size: 16px; background: #e1e4e8; padding: 8px 12px; border-radius: 6px 6px 0 0; }}
  table {{ width: 100%; border-collapse: collapse; background: white;
          box-shadow: 0 1px 3px rgba(0,0,0,0.1); border-radius: 0 0 6px 6px; overflow: hidden; }}
  th {{ text-align: left; padding: 8px 12px; font-size: 12px; color: #666;
       text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 2px solid #e1e4e8; }}
  td {{ padding: 8px 12px; border-bottom: 1px solid #f0f0f0; font-size: 14px; }}
  tr:last-child td {{ border-bottom: none; }}
  .badge {{ display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }}
  .badge.pass {{ background: #dcffe4; color: #22863a; }}
  .badge.xfail {{ background: #fff3cd; color: #856404; }}
  .badge.fail {{ background: #ffdce0; color: #cb2431; }}
  .badge.needs-test {{ background: #e1e4e8; color: #586069; }}
  .badge.no-test {{ background: #f6f8fa; color: #999; }}
  .badge.supported {{ background: #dcffe4; color: #22863a; }}
  .badge.unsupported {{ background: #f6f8fa; color: #999; }}
  .reason {{ font-size: 12px; color: #e36209; margin-left: 6px; }}
  .test-name {{ font-family: monospace; font-size: 13px; color: #0366d6; }}
  .filter {{ margin-bottom: 16px; }}
  .filter button {{ padding: 6px 14px; border: 1px solid #d1d5da; border-radius: 6px;
                   background: white; cursor: pointer; font-size: 13px; margin-right: 4px; }}
  .filter button.active {{ background: #0366d6; color: white; border-color: #0366d6; }}
  .filter button:hover {{ background: #f6f8fa; }}
  .filter button.active:hover {{ background: #0356b0; }}
  tr.hidden {{ display: none; }}
  .generated {{ margin-top: 32px; font-size: 12px; color: #999; text-align: center; }}
</style>
</head>
<body>
<h1>TypeScript Feature Coverage</h1>
<p class="subtitle">Strata JavaScript/TypeScript verification pipeline</p>
<div class="stats">
  <div class="stat green"><div class="num">{n_pass}</div><div class="label">Passing</div></div>
  <div class="stat orange"><div class="num">{n_xfail}</div><div class="label">Known Fail</div></div>
  <div class="stat"><div class="num">{n_needs}</div><div class="label">Needs Test</div></div>
  <div class="stat"><div class="num">{n_parser}</div><div class="label">Parser ✅</div></div>
  <div class="stat red"><div class="num">{n_unsup}</div><div class="label">Unsupported</div></div>
  <div class="stat"><div class="num">{n_total}</div><div class="label">Total</div></div>
</div>
<div class="filter">
  <button class="active" onclick="filter('all',this)">All</button>
  <button onclick="filter('pass',this)">✅ Passing</button>
  <button onclick="filter('xfail',this)">⚠ Known Fail</button>
  <button onclick="filter('needs_test',this)">Needs Test</button>
  <button onclick="filter('unsupported',this)">❌ Unsupported</button>
</div>
{tables}
<p class="generated">Generated by gen_dashboard.sh — re-run to refresh</p>
<script>
function filter(status, btn) {{
  document.querySelectorAll('.filter button').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  document.querySelectorAll('tr[data-status]').forEach(row => {{
    row.classList.toggle('hidden', status !== 'all' && row.dataset.status !== status);
  }});
}}
</script>
</body>
</html>"""

out_path = os.path.join(SCRIPT_DIR, "coverage_dashboard.html")
with open(out_path, "w") as f:
    f.write(html)
print(f"Generated {out_path}")
print(f"Open with: open {out_path}")
