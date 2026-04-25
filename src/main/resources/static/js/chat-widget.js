(function () {
  function currentLang(widget) {
    const fromWidget = widget.getAttribute("data-lang");
    if (fromWidget) return fromWidget;
    const params = new URLSearchParams(window.location.search);
    return params.get("lang") || "en";
  }

  function appendMessage(container, text, type, meta) {
    const message = document.createElement("div");
    message.className = "chat-test-message " + type;
    message.textContent = text;
    if (meta) {
      const metaNode = document.createElement("span");
      metaNode.className = "chat-test-meta";
      metaNode.textContent = meta;
      message.appendChild(metaNode);
    }
    container.appendChild(message);
    container.scrollTop = container.scrollHeight;
  }

  async function sendMessage(widget, text) {
    const messages = widget.querySelector("[data-chat-messages]");
    const lang = currentLang(widget);

    appendMessage(messages, text, "user");
    appendMessage(messages, "Sending...", "bot");
    const pending = messages.lastElementChild;

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: text, locale: lang })
      });

      const body = await response.json();
      pending.remove();

      if (!response.ok) {
        appendMessage(messages, JSON.stringify(body, null, 2), "bot error", "HTTP " + response.status);
        return;
      }

      const meta = [
        "intent=" + body.intent,
        "llmUsed=" + body.llmUsed,
        "noData=" + body.noData,
        "sources=" + (body.sourceIds || []).join(",")
      ].join(" | ");
      appendMessage(messages, body.answer || "(empty answer)", "bot", meta);
    } catch (error) {
      pending.remove();
      appendMessage(messages, error.message || "Request failed", "bot error");
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    const widget = document.querySelector("[data-chat-widget]");
    if (!widget) return;
    if (widget.dataset.chatReady === "true") return;
    widget.dataset.chatReady = "true";

    const toggle = widget.querySelector("[data-chat-toggle]");
    const close = widget.querySelector("[data-chat-close]");
    const panel = widget.querySelector("[data-chat-panel]");
    const send = widget.querySelector("[data-chat-send]");
    const input = widget.querySelector("[data-chat-input]");
    let composing = false;
    let sending = false;

    toggle.addEventListener("click", function () {
      const willOpen = panel.hidden;
      panel.hidden = !willOpen;
      toggle.setAttribute("aria-expanded", String(willOpen));
      if (willOpen) input.focus();
    });

    close.addEventListener("click", function () {
      panel.hidden = true;
      toggle.setAttribute("aria-expanded", "false");
      toggle.focus();
    });

    function submitCurrentMessage() {
      if (sending || composing) return;
      const text = input.value.trim();
      if (!text) return;
      input.value = "";
      sending = true;
      send.disabled = true;
      sendMessage(widget, text);
      window.setTimeout(function () {
        sending = false;
        send.disabled = false;
      }, 300);
    }

    send.addEventListener("click", function () {
      submitCurrentMessage();
    });

    input.addEventListener("compositionstart", function () {
      composing = true;
    });

    input.addEventListener("compositionend", function () {
      window.setTimeout(function () {
        composing = false;
      }, 0);
    });

    input.addEventListener("keydown", function (event) {
      if (event.key !== "Enter") return;
      if (event.isComposing || event.keyCode === 229 || composing) return;
      event.preventDefault();
      submitCurrentMessage();
    });
  });
})();
