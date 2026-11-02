## Sporran 1.0.6

One fix.

- **Fixed: spark's mod-sources view.** The spark profiler logged "Multiple entries with same key" on every start with
  Sporran, and its profiles had no per-mod "Sources" view. The cause was a path that Sporran's `neoforge` container
  shares with Sporran itself; spark now skips the repeated entry and the view works again.
