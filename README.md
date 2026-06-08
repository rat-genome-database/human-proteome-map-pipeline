# human-proteome-map-pipeline

Generates and maintains external database identifiers linking active human genes in RGD to the
[Human Proteome Map](http://www.humanproteomemap.org/) (HPM).

## How It Works

Like the cosmic-pipeline, this pipeline does **not** import data from an external file. Instead it
derives Human Proteome Map identifiers directly from the symbols of active human genes already in RGD —
the HPM website renders a page per gene symbol, so the gene symbol itself is sufficient to construct the
link. (As a result, an HPM cross-reference is maintained for every active human gene; it is symbol-based
rather than a check of actual HPM membership.)

Each run performs three steps:

1. **Retrieve** — fetches all active human genes from RGD and all existing Human Proteome Map
   cross-references (`XDB_KEY = 56`, `SRC_PIPELINE = 'Human Proteome Map'`).
2. **Compare** — a three-way set comparison between the incoming and existing ids determines the records
   to insert (new genes), delete (no longer active), and leave matching (still active — modification date
   refreshed for audit tracking).
3. **Sync** — inserts new ids, deletes obsolete ones, and refreshes modification dates on matching rows.

This incremental approach avoids drop-and-reload, preserving creation dates and enabling change tracking
over time.

### Stale-delete safeguard

Deletion is capped by the configurable `staleXdbDeleteThreshold` (`AppConfigure.xml`, default `5%`): if
more than that fraction of the existing ids would be deleted in a single run, the deletion is aborted and
logged, guarding against accidental mass-removal when upstream data is incomplete.

## Output

For every active human gene an `RGD_ACC_XDB` row is maintained with:

- `XDB_KEY` = 56 (Human Proteome Map)
- `SRC_PIPELINE` = `Human Proteome Map`
- `ACC_ID` = the gene symbol (appended to the HPM URL to form the link)
- `RGD_ID` = the gene's RGD ID

## Build and run

Requires Java 17. Built with Gradle:

```
./gradlew clean createDistro
```

Run (loads/refreshes all Human Proteome Map cross-references for human genes):

```
./run.sh
```
