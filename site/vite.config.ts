import { defineConfig } from "vite";
import { svelte } from "@sveltejs/vite-plugin-svelte";

export default defineConfig({
  plugins: [svelte()],
  // the package list lives in the repo's lists/, one level up
  server: { fs: { allow: [".."] } },
  // three.js is the lazy Scene chunk (~250 KB gz), loaded after first paint
  build: { chunkSizeWarningLimit: 900 },
});
