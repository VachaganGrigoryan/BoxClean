<script lang="ts">
  import { REPO } from "./lib/data";
  import Footer from "./lib/sections/Footer.svelte";
  import Hero from "./lib/sections/Hero.svelte";
  import Install from "./lib/sections/Install.svelte";
  import Layouts from "./lib/sections/Layouts.svelte";
  import Rebuilt from "./lib/sections/Rebuilt.svelte";
  import Story from "./lib/sections/Story.svelte";
  import Tools from "./lib/sections/Tools.svelte";
  import Stage from "./lib/Stage.svelte";
</script>

<Stage />
<header class="top">
  <a class="brand" href="#top" aria-label="BoxClean, back to top">
    <svg viewBox="0 0 26 26" aria-hidden="true"><rect x="1.5" y="4.5" width="23" height="14" rx="3" fill="none" stroke="currentColor" stroke-width="1.6" /><path d="M8 22.5h10" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" /><path d="M8 13.5l3 -3 2.5 2.5 4.5 -4.5" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" /></svg>
    BoxClean
  </a>
  <nav aria-label="Page">
    <a href="#story">Before and after</a>
    <a href="#layouts">Layouts</a>
    <a href="#rebuilt">The idea</a>
    <a href="#install">Install</a>
    <a href={REPO}>GitHub</a>
  </nav>
</header>
<main id="top">
  <Hero />
  <Story />
  <Layouts />
  <Tools />
  <Rebuilt />
  <Install />
  <Footer />
</main>

<style>
  .top {
    position: fixed;
    inset: 0 0 auto;
    z-index: 3;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 18px var(--gutter);
    background: linear-gradient(rgb(11 16 32 / 0.95) 40%, rgb(11 16 32 / 0));
    padding-bottom: 28px;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 650;
    font-size: 1.1rem;
    letter-spacing: -0.01em;
    text-decoration: none;
  }
  .brand svg {
    width: 26px;
    height: 26px;
  }
  nav {
    display: flex;
    gap: clamp(14px, 2.6vw, 28px);
    font-size: 0.95rem;
    color: var(--muted);
  }
  nav a {
    text-decoration: none;
  }
  nav a:hover {
    color: var(--text);
  }
  @media (max-width: 860px) {
    nav a:not(:last-child) {
      display: none;
    }
  }
</style>
