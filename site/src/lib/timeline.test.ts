import { describe, expect, it } from "vitest";
import { frame, screenOf, type SectionBox } from "./timeline";

const VH = 1000;
const boxes: SectionBox[] = [
  { id: "hero", top: 0, height: 1000 },
  { id: "story", top: 1000, height: 4000 },
  { id: "layouts", top: 5000, height: 2000 },
  { id: "tools", top: 7000, height: 1400 },
  { id: "rebuilt", top: 8400, height: 1600 },
  { id: "install", top: 10000, height: 3000 },
  { id: "footer", top: 13000, height: 1000 },
];
const at = (centre: number, mobile = false) => frame(boxes, centre - VH / 2, VH, mobile, "ribbon");

describe("frame", () => {
  it("holds a section's pose away from its edges", () => {
    expect(at(3000).section).toBe("story");
    expect(at(3000).pose).toEqual(at(3500).pose);
  });

  it("blends halfway exactly on a boundary", () => {
    const mid = at(5000).pose;
    const story = at(3000).pose;
    const layouts = at(6000).pose;
    expect(mid.size).toBeCloseTo((story.size + layouts.size) / 2);
    expect(mid.ox).toBeCloseTo((story.ox + layouts.ox) / 2);
  });

  it("clamps above the top and past the end", () => {
    expect(frame(boxes, -500, VH, false, "bento").section).toBe("hero");
    const end = frame(boxes, 99999, VH, false, "bento");
    expect(end.section).toBe("footer");
    expect(end.p).toBe(1);
  });

  it("centres the TV in the half-height canvas on phones", () => {
    const pose = frame(boxes, 3000, VH, true, "bento").pose;
    expect(pose.ox).toBe(0);
    expect(pose.size).toBeGreaterThan(0.7);
  });

  it("shows the picked layout", () => {
    expect(at(6000).screen).toMatchObject({ a: "ribbon", b: "ribbon" });
  });
});

describe("screenOf story", () => {
  it("starts on stock and ends on bento", () => {
    expect(screenOf("story", 0, "bento").screen).toMatchObject({ a: "stock", b: "stock", mix: 0 });
    expect(screenOf("story", 1, "bento").screen).toMatchObject({ a: "v2", b: "bento", mix: 1 });
  });

  it("is halfway through the first wipe where chapter 2 begins", () => {
    const { screen, step } = screenOf("story", 0.25, "bento");
    expect(screen).toMatchObject({ a: "stock", b: "v1" });
    expect(screen.mix).toBeCloseTo(0.5);
    expect(step).toBe(1);
  });
});

describe("screenOf install", () => {
  it("maps progress to the six steps", () => {
    expect(screenOf("install", 0, "bento").step).toBe(0);
    expect(screenOf("install", 0.5, "bento").step).toBe(3);
    expect(screenOf("install", 1, "bento").step).toBe(5);
  });
});
