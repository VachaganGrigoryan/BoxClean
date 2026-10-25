import { describe, expect, it } from "vitest";
import { parseList, TELEMETRY_PACKAGES } from "./data";

describe("parseList", () => {
  it("drops comments, inline comments and blank lines", () => {
    expect(parseList("# head\n\ncom.a   # why\n  com.b\n#com.c\n")).toEqual(["com.a", "com.b"]);
  });

  it("reads the real telemetry list", () => {
    expect(TELEMETRY_PACKAGES).toHaveLength(10);
    expect(TELEMETRY_PACKAGES).toContain("com.miui.tv.analytics");
    expect(TELEMETRY_PACKAGES.every((p) => !p.includes(" ") && !p.includes("#"))).toBe(true);
  });
});
