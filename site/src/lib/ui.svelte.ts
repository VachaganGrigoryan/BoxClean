import type { LayoutId } from "./data";
import type { SectionId } from "./timeline";

/** What the stage reports back to the page: the section and step in view, and the picked layout. */
export const ui = $state({ section: "hero" as SectionId, step: 0, layout: "bento" as LayoutId });
