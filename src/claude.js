// Optional Claude integration. If ANTHROPIC_API_KEY is unset, `available` is
// false and the server falls back to the rule-based summary from predict.js.

import Anthropic from "@anthropic-ai/sdk";

const apiKey = process.env.ANTHROPIC_API_KEY;
const MODEL = process.env.POWERPAL_MODEL || "claude-haiku-4-5-20251001";

export const available = Boolean(apiKey);

const client = available ? new Anthropic({ apiKey }) : null;

/**
 * Generate a concise, user-facing summary + action recommendation for a given
 * area's prediction. Kept small and cheap — Haiku by default.
 */
export async function generateSummary(area, prediction) {
  if (!available) {
    throw new Error("Claude is not configured (ANTHROPIC_API_KEY unset).");
  }

  const system = [
    "You are PowerPal, an assistant that helps people plan around unreliable electricity.",
    "You receive structured stats about a user's area and must return a SHORT, friendly summary",
    "(2-3 sentences max) followed by ONE concrete action recommendation starting with 'Action:'.",
    "Be specific about times if provided. Never invent data. If confidence is 'none' or 'low',",
    "say so plainly.",
  ].join(" ");

  const user = JSON.stringify({ area, prediction }, null, 2);

  const resp = await client.messages.create({
    model: MODEL,
    max_tokens: 300,
    system,
    messages: [{ role: "user", content: user }],
  });

  const text = resp.content
    .filter((block) => block.type === "text")
    .map((block) => block.text)
    .join("\n")
    .trim();

  return text;
}
