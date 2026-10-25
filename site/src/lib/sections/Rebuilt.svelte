<script lang="ts">
  import { CLEANYOURTV, REPO } from "../data";
</script>

<section id="rebuilt" class="wrap" data-scene="rebuilt">
  <div class="panel right wide">
    <h2>Rebuilt, not copied</h2>
    <div class="essay">
      <p>BoxClean exists because of <a href={CLEANYOURTV}>CleanYourTV</a>, Gathin's open-source project that showed a Google TV box can lose its ads without root.</p>
      <p>Forking it would have been the quick route. Instead, BoxClean was rebuilt from an empty folder with Claude Code, one decision at a time, so every package it turns off and every view it draws is understood. The first version, from the debloat script to the sixth layout, landed in eight commits on 21 October 2026.</p>
      <p>Where BoxClean does borrow, it says so. Those parts keep their MIT license and are credited in <a href="{REPO}/blob/main/NOTICE">NOTICE</a>.</p>
    </div>
    <div class="ledger">
      <div>
        <h3>Adapted from CleanYourTV</h3>
        <ul>
          <li>The Classic layout's look and motion</li>
          <li>The colour glow from the focused app</li>
          <li>The gliding glass highlight</li>
          <li>The clock screensaver</li>
          <li>The remote-key service</li>
          <li>The first Mi Box S debloat list</li>
        </ul>
      </div>
      <div class="new">
        <h3>Written for BoxClean</h3>
        <ul>
          <li>Five more layouts and the picker</li>
          <li>A debloat tool that records state and restores it</li>
          <li>Dry runs, probe, backup and an ADB-closed check</li>
          <li>Tests, CI and ready-built APK releases</li>
        </ul>
      </div>
    </div>
    <p class="small">On screen: the Classic look wiping into Bento, then the eight commits.</p>
  </div>
</section>

<style>
  section {
    min-height: 170svh;
    display: flex;
    align-items: center;
    padding-block: 15svh;
  }
  .essay p {
    font-size: clamp(1.06rem, 1.3vw, 1.2rem);
    line-height: 1.55;
    color: #d6dae6;
  }
  .ledger {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 28px;
    margin: 28px 0 20px;
  }
  .ledger h3 {
    font-size: 1rem;
    color: var(--muted);
    font-weight: 500;
  }
  ul {
    margin: 0;
    padding: 0;
    list-style: none;
  }
  li {
    padding: 7px 0 7px 20px;
    position: relative;
    color: #d6dae6;
    font-size: 0.95rem;
  }
  li::before {
    content: "";
    position: absolute;
    left: 0;
    top: 1.05em;
    width: 10px;
    height: 1px;
    background: var(--muted);
  }
  .new li::before {
    background: var(--text);
    width: 6px;
    height: 6px;
    border-radius: 50%;
    top: 0.85em;
  }
  @media (max-width: 860px) {
    section {
      min-height: auto;
      padding-block: 50svh 10svh;
    }
    .ledger {
      grid-template-columns: 1fr;
    }
  }
</style>
