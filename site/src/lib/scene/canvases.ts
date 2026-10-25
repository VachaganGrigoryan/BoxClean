import { CanvasTexture, SRGBColorSpace } from "three";
import { COMMITS, STEPS, TELEMETRY_LIST, TELEMETRY_PACKAGES } from "../data";

const W = 1600;
const H = 900;
const PAD = 96;
const MONO = '"JetBrains Mono", ui-monospace, Menlo, monospace';
const SANS = '"Bricolage Grotesque", ui-sans-serif, system-ui, sans-serif';

const INK = "#E8EBF4";
const DIM = "#7B84A0";
const OK = "#8FD9B6";

type Line = { text: string; color?: string; dim?: string };

/** A 16:9 canvas the screen samples; redraws only when what it shows changes. */
abstract class CanvasScreen {
  readonly canvas = document.createElement("canvas");
  readonly ctx: CanvasRenderingContext2D;
  readonly texture: CanvasTexture;
  private last = "";

  constructor() {
    this.canvas.width = W;
    this.canvas.height = H;
    this.ctx = this.canvas.getContext("2d")!;
    this.texture = new CanvasTexture(this.canvas);
    this.texture.colorSpace = SRGBColorSpace;
  }

  /** Draws if the signature of what it would draw changed. */
  update(param: number, now: number) {
    const sig = this.signature(param, now);
    if (sig === this.last) return;
    this.last = sig;
    this.draw(param, now);
    this.texture.needsUpdate = true;
  }

  invalidate() {
    this.last = "";
  }

  protected abstract signature(param: number, now: number): string;
  protected abstract draw(param: number, now: number): void;
}

/** Terminal look: lines top-down, long lines wrapped at spaces, an optional block cursor. */
abstract class Terminal extends CanvasScreen {
  protected size = 40;

  protected paint(lines: Line[], cursor: boolean) {
    const { ctx } = this;
    const g = ctx.createLinearGradient(0, 0, W, H);
    g.addColorStop(0, "#0E1426");
    g.addColorStop(1, "#080B16");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);
    ctx.font = `400 ${this.size}px ${MONO}`;
    ctx.textBaseline = "top";
    const lh = this.size * 1.55;
    let y = PAD;
    let x = PAD;
    for (const line of lines) {
      for (const row of wrap(ctx, line.text, W - PAD * 2)) {
        x = PAD;
        ctx.fillStyle = line.color ?? INK;
        ctx.fillText(row, x, y);
        x += ctx.measureText(row).width;
        y += lh;
      }
      if (line.dim) {
        y -= lh;
        ctx.fillStyle = DIM;
        ctx.fillText(line.dim, x, y);
        x += ctx.measureText(line.dim).width;
        y += lh;
      }
    }
    if (cursor) {
      ctx.fillStyle = INK;
      ctx.fillRect(x + 6, y - lh + 4, this.size * 0.6, this.size * 1.1);
    }
  }
}

function wrap(ctx: CanvasRenderingContext2D, text: string, max: number): string[] {
  if (ctx.measureText(text).width <= max) return [text];
  const rows: string[] = [];
  let row = "";
  for (const word of text.split(" ")) {
    const next = row ? `${row} ${word}` : word;
    if (row && ctx.measureText(next).width > max) {
      rows.push(row);
      row = `  ${word}`;
    } else row = next;
  }
  return [...rows, row];
}

const blink = (now: number) => Math.floor(now / 530) % 2 === 0;

/** `boxclean.py disable` on the real telemetry list; param 0..1 is how far the run has got. */
export class DisableRun extends Terminal {
  protected size = 36;
  private shown = (p: number) => Math.round(p * TELEMETRY_PACKAGES.length);

  protected signature(p: number, now: number) {
    return `${this.shown(p)}|${blink(now)}`;
  }

  protected draw(p: number, now: number) {
    const n = this.shown(p);
    const lines: Line[] = [{ text: `$ boxclean.py disable ${TELEMETRY_LIST}` }, { text: " " }];
    for (const pkg of TELEMETRY_PACKAGES.slice(0, n)) lines.push({ text: pkg, color: INK, dim: "  disabled-user" });
    if (n === TELEMETRY_PACKAGES.length) {
      lines.push({ text: " " });
      lines.push({ text: `disabled ${n}; undo with: restore ${TELEMETRY_LIST}`, color: OK });
    }
    this.paint(lines, n === TELEMETRY_PACKAGES.length && blink(now));
  }
}

/** The first version's eight commits; param 0..1 reveals them oldest first. */
export class GitLog extends Terminal {
  protected size = 34;
  private shown = (p: number) => Math.round(p * COMMITS.length);

  protected signature(p: number, now: number) {
    return `${this.shown(p)}|${blink(now)}`;
  }

  protected draw(p: number, now: number) {
    const lines: Line[] = [{ text: "$ git log --reverse --oneline", color: INK }, { text: " " }];
    for (const c of COMMITS.slice(0, this.shown(p))) {
      const [hash, ...rest] = c.split(" ");
      const subject = rest.join(" ");
      lines.push({ text: hash, color: OK, dim: " " + fit(this.ctx, subject, W - PAD * 2 - this.size * 5) });
    }
    lines.push({ text: " " });
    lines.push({ text: "# all eight on 21 October 2026", color: DIM });
    this.paint(lines, blink(now));
  }
}

function fit(ctx: CanvasRenderingContext2D, text: string, max: number) {
  if (ctx.measureText(text).width <= max) return text;
  let t = text;
  while (t.length > 4 && ctx.measureText(t + "…").width > max) t = t.slice(0, -1);
  return t + "…";
}

/** The install step in view, its command typed out after the step changes. */
export class InstallStep extends Terminal {
  protected size = 38;
  private step = -1;
  private since = 0;

  protected signature(step: number, now: number) {
    if (step !== this.step) {
      this.step = step;
      this.since = now;
    }
    return `${step}|${this.typed(now)}|${blink(now)}`;
  }

  private typed(now: number) {
    return Math.floor((now - this.since) / 22);
  }

  protected draw(step: number, now: number) {
    const s = STEPS[step];
    const cmd =
      s.cmd ?? "# on the TV:\n# Settings → System → About\n# press OK on Build seven times\n# Developer options → USB debugging ON";
    let left = this.typed(now);
    const lines: Line[] = [{ text: `# ${step + 1} of ${STEPS.length}  ${s.title}`, color: DIM }, { text: " " }];
    for (const raw of cmd.split("\n")) {
      if (left <= 0) break;
      const comment = raw.startsWith("#");
      const full = comment ? raw : `$ ${raw}`;
      lines.push({ text: full.slice(0, left), color: comment ? DIM : INK });
      left -= full.length;
    }
    this.paint(lines, blink(now) || left < 0);
  }
}

/** The launcher's screensaver: a thin clock and date drifting over a soft glow. */
export class ClockDream extends CanvasScreen {
  protected signature(_: number, now: number) {
    return String(Math.floor(now / 50));
  }

  protected draw(_: number, now: number) {
    const { ctx } = this;
    const t = now / 1000;
    const x = W * (0.5 + Math.sin(t * 0.07) * 0.22);
    const y = H * (0.5 + Math.cos(t * 0.05) * 0.18);
    ctx.fillStyle = "#000";
    ctx.fillRect(0, 0, W, H);
    const g = ctx.createRadialGradient(x, y, 0, x, y, 520);
    g.addColorStop(0, "rgba(90,110,190,0.30)");
    g.addColorStop(1, "rgba(0,0,0,0)");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);
    const d = new Date();
    const time = d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }).replace(/\s?[AP]M$/i, "");
    const date = d.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" }).toUpperCase();
    ctx.textAlign = "center";
    ctx.textBaseline = "alphabetic";
    ctx.fillStyle = "#F2F4FA";
    ctx.font = `200 220px ${SANS}`;
    ctx.fillText(time, x, y + 40);
    ctx.fillStyle = "rgba(242,244,250,0.7)";
    ctx.font = `500 30px ${SANS}`;
    ctx.letterSpacing = "6px";
    ctx.fillText(date, x, y + 110);
    ctx.letterSpacing = "0px";
    ctx.textAlign = "start";
  }
}
