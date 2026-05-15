
(function () {
  const searchInput = document.querySelector("[data-doc-search]");
  const cards = Array.from(document.querySelectorAll("[data-search]"));
  const emptyState = document.querySelector("[data-empty-state]");

  function filterCards() {
    if (!cards.length) return;
    const query = (searchInput && searchInput.value ? searchInput.value : "").trim().toLowerCase();
    let visible = 0;
    cards.forEach((card) => {
      const haystack = card.dataset.search || "";
      const show = !query || haystack.includes(query);
      card.hidden = !show;
      if (show) visible += 1;
    });
    if (emptyState) emptyState.hidden = visible !== 0;
  }
  if (searchInput) searchInput.addEventListener("input", filterCards);

  document.querySelectorAll("[data-tabs]").forEach((root) => {
    const buttons = Array.from(root.querySelectorAll("[data-tab]"));
    const panels = Array.from(root.querySelectorAll("[data-tab-panel]"));
    buttons.forEach((button) => {
      button.addEventListener("click", () => {
        const key = button.dataset.tab;
        buttons.forEach((item) => item.classList.toggle("active", item === button));
        panels.forEach((panel) => panel.classList.toggle("active", panel.dataset.tabPanel === key));
      });
    });
  });

  document.querySelectorAll("pre").forEach((pre) => {
    if (pre.querySelector(".copy-code")) return;
    const button = document.createElement("button");
    button.className = "copy-code";
    button.type = "button";
    button.textContent = "复制";
    button.addEventListener("click", async () => {
      const text = pre.innerText.replace(/^复制\s*/, "");
      try {
        await navigator.clipboard.writeText(text);
        button.textContent = "已复制";
        setTimeout(() => (button.textContent = "复制"), 1200);
      } catch (error) {
        button.textContent = "复制失败";
        setTimeout(() => (button.textContent = "复制"), 1200);
      }
    });
    pre.appendChild(button);
  });

  const endpoint = document.getElementById("example-endpoint");
  const scope = document.getElementById("example-scope");
  const output = document.getElementById("example-output");
  const copyExample = document.querySelector("[data-copy-example]");

  const scopes = {
    photo: {
      query: "fixing time 怎么生成？",
      input: "Failed message mentions fixing time cutoff and NDF settlement.",
      channel: "tradition",
      product: "stella",
      pack: "fxd-ndf-cutoff-fixing",
      domain: "cutoff-fixing"
    },
    payments: {
      query: "payment amount 怎么生成？",
      input: "src.amount=123.45\nsrc.currency=USD",
      channel: "tradition",
      product: "stella",
      pack: "payments",
      domain: "core"
    }
  };

  function scopeJson(selected) {
    return `{
      "channel": "${selected.channel}",
      "product": "${selected.product}",
      "pack": "${selected.pack}",
      "domain": "${selected.domain}"
    }`;
  }

  function buildExample() {
    if (!endpoint || !scope || !output) return;
    const selectedScope = scopes[scope.value] || scopes.photo;
    const endpointValue = endpoint.value;

    if (endpointValue === "mcp") {
      output.textContent = "curl -s http://localhost:8080/mcp/tools | jq";
      return;
    }

    if (endpointValue === "scenario") {
      output.textContent = `curl -sS -X POST http://localhost:8080/api/v1/scenario/analyze-failed-message \\
  -H 'Content-Type: application/json' \\
  -H 'X-RTS-API-Key: tester-key' \\
  -d '{
    "input": "${selectedScope.input.replace(/\n/g, "\\n")}",
    "caller_id": "tester",
    "scope": ${scopeJson(selectedScope)},
    "output_mode": "default",
    "max_objects": 5
  }' | jq`;
      return;
    }

    const path = endpointValue === "ask" ? "/api/v1/ask" : "/api/v1/query";
    const llmFlag = endpointValue === "ask" ? "true" : "false";
    output.textContent = `curl -sS -X POST http://localhost:8080${path} \\
  -H 'Content-Type: application/json' \\
  -H 'X-RTS-API-Key: tester-key' \\
  -d '{
    "query": "${selectedScope.query}",
    "caller_id": "tester",
    "scope_hint": ${scopeJson(selectedScope)},
    "output_mode": "default",
    "use_llm": ${llmFlag}
  }' | jq`;
  }

  if (endpoint && scope && output) {
    endpoint.addEventListener("change", buildExample);
    scope.addEventListener("change", buildExample);
    buildExample();
  }
  if (copyExample && output) {
    copyExample.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(output.textContent || "");
        copyExample.textContent = "已复制";
        setTimeout(() => (copyExample.textContent = "复制示例"), 1200);
      } catch (error) {
        copyExample.textContent = "复制失败";
        setTimeout(() => (copyExample.textContent = "复制示例"), 1200);
      }
    });
  }
})();
