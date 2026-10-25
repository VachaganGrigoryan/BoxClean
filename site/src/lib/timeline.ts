import { CHAPTERS, type LayoutId, STEPS } from "./data";

export type SectionId = "hero" | "story" | "layouts" | "tools" | "rebuilt" | "install" | "footer";

/**
 * Where the camera sits around the TV and where the TV lands on screen.
 * yaw/pitch orbit the focus point (radians), size is the TV's width as a share of the viewport width,
 * ox/oy move the TV's centre in NDC (-1..1) by shifting the lens, focusY lifts or lowers what the camera aims at.
 */
export interface Pose {
  yaw: number;
  pitch: number;
  size: number;
  ox: number;
  oy: number;
  focusY: number;
}

/** What the screen shows: texture a, wiped left-to-right into b by mix; term drives terminal/step screens. */
export interface Screen {
  a: string;
  b: string;
  mix: number;
  term: number;
}

export interface SectionBox {
  id: SectionId;
  top: number;
  height: number;
}

export interface Frame {
  section: SectionId;
  p: number;
  step: number;
  pose: Pose;
  screen: Screen;
}

const P = (yaw: number, pitch: number, size: number, ox: number, oy: number, focusY = 0): Pose => ({
  yaw, pitch, size, ox, oy, focusY,
});

const DESKTOP: Record<SectionId, Pose> = {
  hero: P(0.42, 0.07, 0.5, 0.4, -0.04),
  story: P(0.16, 0.04, 0.5, 0.36, 0),
  layouts: P(0, 0.03, 0.52, 0, 0.04),
  tools: P(-0.55, 0.34, 0.4, -0.38, 0.02, -0.55),
  rebuilt: P(-0.3, 0.08, 0.38, -0.4, 0),
  install: P(0.32, 0.05, 0.42, 0.42, 0),
  footer: P(0, -0.02, 0.4, 0, 0.3, 0.2),
};

// phones: the canvas is only the top half of the viewport, so the TV is centred in it
const MOBILE: Record<SectionId, Pose> = {
  hero: P(0.26, 0.06, 0.8, 0, 0),
  story: P(0.1, 0.04, 0.8, 0, 0),
  layouts: P(0, 0.03, 0.84, 0, 0),
  tools: P(-0.34, 0.3, 0.6, 0, 0.04, -0.55),
  rebuilt: P(-0.18, 0.06, 0.74, 0, 0),
  install: P(0.16, 0.04, 0.76, 0, 0),
  footer: P(0, 0.02, 0.8, 0, 0),
};

export const clamp = (v: number, a = 0, b = 1) => Math.min(b, Math.max(a, v));
export const smooth = (v: number) => {
  const x = clamp(v);
  return x * x * (3 - 2 * x);
};
const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

export function mixPose(a: Pose, b: Pose, t: number): Pose {
  return {
    yaw: lerp(a.yaw, b.yaw, t),
    pitch: lerp(a.pitch, b.pitch, t),
    size: lerp(a.size, b.size, t),
    ox: lerp(a.ox, b.ox, t),
    oy: lerp(a.oy, b.oy, t),
    focusY: lerp(a.focusY, b.focusY, t),
  };
}

/** The screen for a section at local progress p (0..1). */
export function screenOf(id: SectionId, p: number, layout: LayoutId): { screen: Screen; step: number } {
  const still = (key: string, term = 0) => ({ a: key, b: key, mix: 0, term });
  switch (id) {
    case "hero":
      return { screen: still("video"), step: 0 };
    case "story": {
      const shots = CHAPTERS.map((c) => c.shot);
      const t = p * shots.length;
      // wipe i (to shot i) is centred where chapter i begins, so it runs between two blocks of text
      for (let i = shots.length - 1; i > 0; i--) {
        const w = smooth((t - i + 0.3) / 0.6);
        if (w > 0) return { screen: { a: shots[i - 1], b: shots[i], mix: w, term: 0 }, step: Math.min(shots.length - 1, Math.floor(t)) };
      }
      return { screen: still(shots[0]), step: 0 };
    }
    case "layouts":
      return { screen: still(layout), step: 0 };
    case "tools":
      return { screen: still("term-tools", clamp((p - 0.12) / 0.6)), step: 0 };
    case "rebuilt":
      if (p < 0.5) return { screen: { a: "classic", b: "bento", mix: smooth((p - 0.12) / 0.26), term: 0 }, step: 0 };
      return { screen: { a: "bento", b: "term-git", mix: smooth((p - 0.5) / 0.16), term: clamp((p - 0.6) / 0.3) }, step: 1 };
    case "install": {
      const step = Math.min(STEPS.length - 1, Math.floor(p * STEPS.length));
      return { screen: still("term-install", step), step };
    }
    case "footer":
      return { screen: still("clock"), step: 0 };
  }
}

/** Everything the stage needs for one scroll position. Boxes are in page coordinates, sorted top to bottom. */
export function frame(boxes: SectionBox[], scrollY: number, vh: number, mobile: boolean, layout: LayoutId): Frame {
  const poses = mobile ? MOBILE : DESKTOP;
  // the reading line: mid-screen, or the middle of the text area under the TV on phones
  const centre = scrollY + vh * (mobile ? 0.74 : 0.5);
  let i = boxes.findIndex((b) => centre < b.top + b.height);
  if (i < 0) i = boxes.length - 1;
  const box = boxes[i];
  const p = clamp((centre - box.top) / box.height);

  // camera moves only in a band around each boundary, so it holds still while a section reads
  const band = vh * 0.4;
  let pose = poses[box.id];
  if (i > 0 && centre < box.top + band) {
    pose = mixPose(poses[boxes[i - 1].id], pose, smooth((centre - (box.top - band)) / (2 * band)));
  } else if (i < boxes.length - 1 && centre > box.top + box.height - band) {
    const end = box.top + box.height;
    pose = mixPose(pose, poses[boxes[i + 1].id], smooth((centre - (end - band)) / (2 * band)));
  }

  const { screen, step } = screenOf(box.id, p, layout);
  return { section: box.id, p, step, pose, screen };
}
