import express from "express";

const app = express();
app.use(express.json({ limit: "32kb" }));

const PORT = Number(process.env.PORT || 8080);
const AI_API_URL = process.env.AI_API_URL;
const AI_API_KEY = process.env.AI_API_KEY;
const AI_MODEL = process.env.AI_MODEL;

app.get("/health", (_req, res) => res.json({ ok: true, service: "WidgetAI" }));

app.post("/v1/widgets/generate", async (req, res) => {
  try {
    const prompt = String(req.body?.prompt || "").trim();
    const device = req.body?.device && typeof req.body.device === "object" ? req.body.device : {};
    if (!prompt || prompt.length > 2000) {
      return res.status(400).json({ error: "prompt_required_or_too_long" });
    }
    if (!AI_API_URL || !AI_API_KEY || !AI_MODEL) {
      return res.status(503).json({ error: "ai_provider_not_configured" });
    }

    const system = [
      "You design Android home-screen widgets for WidgetAI.",
      "Return only valid JSON with keys: message, widget.",
      "widget must be a declarative specification, never executable code.",
      "Allowed widget fields: name, description, refreshSeconds, elements.",
      "Each element may use type (text, battery, clock, progress), label, value, size, and color.",
      "Do not request private device data beyond the supplied context.",
      "Use only supplied device values; use live Android components for clock and battery when appropriate."
    ].join(" ");

    const upstream = await fetch(AI_API_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${AI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: AI_MODEL,
        messages: [
          { role: "system", content: system },
          { role: "user", content: JSON.stringify({ prompt, device }) }
        ],
        response_format: { type: "json_object" }
      })
    });

    const raw = await upstream.text();
    if (!upstream.ok) {
      console.error("AI provider error", upstream.status, raw.slice(0, 500));
      return res.status(502).json({ error: "ai_provider_error", status: upstream.status });
    }

    const envelope = JSON.parse(raw);
    const content = envelope.choices?.[0]?.message?.content;
    if (typeof content !== "string") throw new Error("Provider returned no message content");
    const result = JSON.parse(content);
    if (!result.widget || !Array.isArray(result.widget.elements)) {
      throw new Error("AI returned an invalid widget specification");
    }
    return res.json({
      message: String(result.message || "Виджет подготовлен"),
      widget: {
        name: String(result.widget.name || "Новый виджет").slice(0, 80),
        description: String(result.widget.description || "").slice(0, 240),
        refreshSeconds: Math.min(3600, Math.max(60, Number(result.widget.refreshSeconds || 900))),
        elements: result.widget.elements.slice(0, 16).map((item) => ({
          type: ["text", "battery", "clock", "progress"].includes(item.type) ? item.type : "text",
          label: String(item.label || "").slice(0, 80),
          value: String(item.value || "").slice(0, 120),
          size: Math.min(32, Math.max(10, Number(item.size || 14))),
          color: /^#[0-9a-fA-F]{6}$/.test(item.color) ? item.color : "#FFFFFF"
        }))
      }
    });
  } catch (error) {
    console.error("Widget generation failed", error);
    return res.status(500).json({ error: "generation_failed" });
  }
});

app.listen(PORT, "0.0.0.0", () => console.log(`WidgetAI API listening on ${PORT}`));
