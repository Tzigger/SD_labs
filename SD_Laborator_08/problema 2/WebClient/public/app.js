const senderEl = document.getElementById("sender");
const targetEl = document.getElementById("target");
const questionEl = document.getElementById("question");
const outputEl = document.getElementById("output");
const askBtn = document.getElementById("askBtn");
const clearBtn = document.getElementById("clearBtn");

function nowStamp() {
  return new Date().toLocaleTimeString("ro-RO", { hour12: false });
}

function appendLine(text) {
  outputEl.textContent += `${text}\n`;
  outputEl.scrollTop = outputEl.scrollHeight;
}

async function askQuestion() {
  const sender = senderEl.value;
  const target = targetEl.value;
  const question = questionEl.value.trim();

  if (!question) {
    appendLine(`[${nowStamp()}] Intrebarea nu poate fi goala.`);
    return;
  }

  askBtn.disabled = true;
  appendLine(`[${nowStamp()}] Trimis: ${sender} -> ${target} | ${question}`);

  try {
    const response = await fetch("/api/ask", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sender, target, question }),
    });

    const data = await response.json();
    if (!response.ok || !data.ok) {
      appendLine(`[${nowStamp()}] EROARE: ${data.error || "Eroare necunoscuta."}`);
      return;
    }

    appendLine(`[${nowStamp()}] Raspuns:\n${data.responseText}`);
  } catch (error) {
    appendLine(`[${nowStamp()}] EROARE: ${error.message}`);
  } finally {
    askBtn.disabled = false;
  }
}

askBtn.addEventListener("click", askQuestion);
clearBtn.addEventListener("click", () => {
  outputEl.textContent = "";
});
