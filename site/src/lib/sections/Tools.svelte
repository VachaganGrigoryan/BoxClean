<script lang="ts">
  import { TELEMETRY_LIST, TELEMETRY_PACKAGES } from "../data";
</script>

<section class="wrap" data-scene="tools">
  <div class="panel right wide">
    <h2>Two small tools</h2>
    <p class="soft">One cleans the box from your computer. The other is the home screen it leaves behind.</p>
    <div class="cols">
      <div>
        <h3>The debloat script</h3>
        <ul>
          <li><b>Disables, never uninstalls.</b> Packages are turned off for user 0 with <code>pm disable-user</code>.</li>
          <li><b>Records first.</b> Before the first change it saves every package's state and the current Home app.</li>
          <li><b>Undo is one command.</b> <code>restore</code> turns everything back on and returns Home to its old owner.</li>
          <li><b>Dry runs.</b> <code>disable</code> and <code>restore</code> show what they would do first.</li>
        </ul>
      </div>
      <div>
        <h3>BoxClean Home</h3>
        <ul>
          <li><b>Offline.</b> No network permission, no accounts, no trackers, no AndroidX.</li>
          <li><b>Your buttons.</b> Remote keys whose own app is off can open an app you pick.</li>
          <li><b>A drifting clock</b> as the screensaver instead of Google's ambient feed.</li>
          <li><b>Fast on old hardware.</b> Plain framework views, tuned for the box's GPU.</li>
        </ul>
      </div>
    </div>
    <p class="small">On screen: the {TELEMETRY_PACKAGES.length} packages in <code>{TELEMETRY_LIST}</code>, the first batch to go.</p>
  </div>
</section>

<style>
  section {
    min-height: 150svh;
    display: flex;
    align-items: center;
    padding-block: 15svh;
  }
  .cols {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 28px;
    margin: 32px 0 20px;
  }
  ul {
    margin: 0;
    padding: 0;
    list-style: none;
  }
  li {
    padding: 10px 0;
    color: var(--soft);
    font-size: 0.96rem;
    border-top: 1px solid color-mix(in srgb, var(--line) 70%, transparent);
  }
  li b {
    color: var(--text);
    font-weight: 600;
  }
  @media (max-width: 860px) {
    section {
      min-height: auto;
      padding-block: 50svh 10svh;
    }
    .cols {
      grid-template-columns: 1fr;
    }
  }
</style>
