import telemetryList from "../../../lists/1-telemetry-ads.txt?raw";

export const REPO = "https://github.com/VachaganGrigoryan/BoxClean";
export const RELEASES = `${REPO}/releases/latest`;
export const CLEANYOURTV = "https://github.com/Gathin23/cleanyourtv";

/** Package names from a lists/*.txt file: one per line, `#` starts a comment. */
export function parseList(text: string): string[] {
  return text
    .split("\n")
    .map((line) => line.replace(/#.*/, "").trim())
    .filter(Boolean);
}

export const TELEMETRY_LIST = "lists/1-telemetry-ads.txt";
export const TELEMETRY_PACKAGES = parseList(telemetryList);

export type ShotKey = "stock" | "v1" | "v2" | LayoutId;
export type LayoutId = "bento" | "ribbon" | "spotlight" | "editorial" | "rail" | "classic";

export const CHAPTERS: { shot: ShotKey; title: string; caption: string; body: string }[] = [
  {
    shot: "stock",
    title: "Google TV, as it ships",
    caption: "Google TV home",
    body: 'The stock home opens on Google TV\'s own tabs and a "Discover more apps" banner. Behind it run dozens of services you never asked for: telemetry, promos and apps that install themselves.',
  },
  {
    shot: "v1",
    title: "A home that stays offline",
    caption: "BoxClean Home, first version",
    body: "With the clutter switched off, the box needs a home screen that doesn't phone home. The first BoxClean Home was a plain grid: a clock, your apps and Settings. It has no network permission at all.",
  },
  {
    shot: "v2",
    title: "One polished look",
    caption: "BoxClean Home, one look",
    body: "Next came the Outfit type, a colour glow sampled from the focused app, and a glass highlight that glides between tiles. This look, adapted from CleanYourTV, lives on as the Classic layout.",
  },
  {
    shot: "bento",
    title: "Six layouts, picked from the remote",
    caption: "BoxClean Home, Bento layout",
    body: "Today the launcher builds its home from the layout you choose: Bento, Ribbon, Spotlight, Editorial, Side rail or Classic. Press Layout, pick one, and it stays picked after a reboot.",
  },
];

export const LAYOUTS: { id: LayoutId; name: string; desc: string }[] = [
  { id: "bento", name: "Bento", desc: "The default. A board of rounded cells with a clock, four big apps and Wi-Fi, Settings and Layout; every other app is in a grid below." },
  { id: "ribbon", name: "Ribbon", desc: "LG webOS style. The focused app's banner, blurred, fills the screen above one ribbon of cards." },
  { id: "spotlight", name: "Spotlight", desc: "One app at a time, big. Left and right flip through the apps, Down opens a strip of all of them." },
  { id: "editorial", name: "Editorial", desc: "A magazine contents page, with numbered app names and the focused one's banner framed beside them." },
  { id: "rail", name: "Side rail", desc: "Round app icons down the left edge next to a big clock." },
  { id: "classic", name: "Classic", desc: "Apple TV style, with a large app name over a dock, a gliding glass highlight and a grid." },
];

export const STEPS: { title: string; body: string; cmd?: string }[] = [
  {
    title: "Turn on ADB on the box",
    body: 'Settings → System → About, press OK on Build seven times. Then Developer options → USB debugging on. Note the IP address under Settings → Network and accept "Allow debugging?" on the TV.',
  },
  {
    title: "Look first, and back up",
    body: "Neither command changes anything.",
    cmd: "TV_HOST=<ip> uv run tools/boxclean.py probe\nTV_HOST=<ip> uv run tools/boxclean.py backup",
  },
  {
    title: "Debloat in batches",
    body: "Dry-run each list, then apply it. Repeat with lists/2-personal.txt after editing it to suit you.",
    cmd: "TV_HOST=<ip> uv run tools/boxclean.py disable lists/1-telemetry-ads.txt --dry-run\nTV_HOST=<ip> uv run tools/boxclean.py disable lists/1-telemetry-ads.txt",
  },
  {
    title: "Install BoxClean Home",
    body: "Build and install it, then switch off the stock launcher. Or install the APK from Releases instead of building.",
    cmd: "TV_HOST=<ip> launcher/build-install.sh\nTV_HOST=<ip> uv run tools/boxclean.py disable lists/3-launcher.txt",
  },
  {
    title: "Pick a layout",
    body: "Press Layout on the home screen and choose one. For the clock screensaver:",
    cmd: "adb shell settings put secure screensaver_components com.boxclean.home/.ClockDream",
  },
  {
    title: "Close ADB again",
    body: "Turn USB debugging off, revoke the authorisations and reboot. Then check that the port is shut; it must say \"refused\".",
    cmd: "TV_HOST=<ip> uv run tools/boxclean.py closed",
  },
];

/** The first version's history, oldest first, as `git log --format='%h %s'` printed it. */
export const COMMITS = [
  "010c4aa feat: BoxClean debloat tool and offline launcher for Mi Box S 2nd gen",
  "5565470 feat(launcher): Apple TV style home, remote keys, clock screensaver",
  "0515948 feat(launcher): bento home layout with Layout switch",
  "07e1d52 refactor(launcher): layouts behind HomeLayout, picker panel for Layout",
  "5675654 feat(launcher): LG webOS style Ribbon layout",
  "fbbd857 feat(launcher): Spotlight, Editorial and Side rail layouts",
  "a7a971c docs: README with screenshots, demo GIF and install guide",
  "7fd8abf chore: Apache-2.0 license, CI, issue templates",
];
