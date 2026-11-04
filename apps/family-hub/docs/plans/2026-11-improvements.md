<!-- Implementation plan of the November 2026 family-hub improvements, followed phase by phase and removed once done. -->

# family-hub improvements: plan for implementation by a cheaper model

## Context

The family asked for ten improvements to `apps/family-hub` (Spring Boot 4.1 + SQLite service, React 19 UI in `web/`):

| # | Request | Current state (verified in code) |
|---|---|---|
| 1 | Rename shopping lists | Backend already supports it (`PUT /api/shopping/lists/{id}`, `ShoppingService.update`); the UI does not expose it |
| 2 | When adding a recipe to a list, choose which ingredients to buy | `ShoppingService.addRecipe` inserts every ingredient |
| 3 | Bought quantity/unit may differ from the recipe (4 pinches of salt, 1 pack bought) and be priced | `ShoppingRows.estimate` converts to the item unit; PINCH cannot become KG, so the row is unpriced |
| 4 | Shopping rows editable (override) | Only add, remove and check exist |
| 5 | Clicking outside a dialog must not close it | `web/src/components/Dialog.tsx` closes on backdrop click (`onClick` on `<dialog>`) |
| 6 | Feed the budget from bank statements (photo), finding or creating categories | Nothing |
| 7 | Feed shopping lists (new or existing) from a photographed list, inferring units | Nothing |
| 8 | Easy export/import of the database; an API to set up a fresh instance | Export exists (`GET /api/admin/backup`, `VACUUM INTO`); no restore |
| 9 | To-dos: optional start date besides the due date; dated to-dos appear in the planning, priority visible | `todo_item` has only `due_at`; the planning shows planning entries only |
| 10 | Import to-dos from a photo or pasted text (one line = one task), same engine as item 7 | Nothing |

Decisions taken with the user:

- **100 % local, no AI, nothing leaves the NAS.** Photos are read by Tesseract (CLI in the image); PDFs by PDFBox (text layer, OCR fallback for scanned PDFs); bank exports OFX and CSV (Crédit Agricole Centre France) parsed exactly. Categories come from the family's own history, then from a merchant keyword table; unknown lines are categorised by hand in a review screen, where a new category can be typed. Accepted trade-off: handwritten lists are read poorly; a "paste text" input uses the same parser as a fallback.
- **Restore = the SQLite backup file**, not a JSON export: the backup already holds the whole configuration, Flyway upgrades an older backup. A fresh instance accepts an unauthenticated restore while it has no member (same trust model as `/api/setup`), which makes automation a single `curl`.
- Escape and the ✕ button still close dialogs; only the backdrop click is removed.
- To-dos without a time keep today's default times (start 09:00, due 18:00).

## How to run this plan

- Implement with **Claude Sonnet 5.5**. Haiku is not recommended: Spring Modulith boundaries, Flyway on SQLite and the parsers need more judgement. Use high effort for phases 6 to 8.
- **One prompt per phase**: "Implement phase N of the plan in apps/family-hub/docs/plans/2026-11-improvements.md", then review the diff. Phases are ordered by risk and each ends green (`make ci`) with one commit.
- **Before phase 6**, the user provides the real samples listed in "Test samples" below. Without them the parsers are written from the generic formats described here and will likely need a second pass.

## Rules every phase follows (implementer checklist)

- Work in `apps/family-hub`, on branch `claude/festive-brahmagupta-k99uf8`. Commit per phase with Conventional Commits, scope `family-hub` (e.g. `feat(family-hub): rename shopping lists`). **Do not push without asking the user.**
- Read a file before editing it. Never edit applied migrations `V1`–`V7`; add `V8`, `V9`, `V10`.
- Every new file starts with a one-line comment stating its purpose (not JSON). New Java packages get a `package-info.java` with `@NullMarked`. No comments narrating the change ("added", "now", "new").
- Code, comments and docs in English; UI text in French.
- Spring Modulith (`ModularityTest`): a module may use only the root package of another module, never `internal`. `shared.web` is internal to `shared`. No dependency cycle.
- After any API change: `make api` (rewrites `web/src/api/openapi.json`, regenerates `schema.d.ts`); otherwise `OpenApiContractTest` fails.
- Tests: deterministic, no network. Integration tests extend `IntegrationTest` (shared context and database, fixed clock `NOW = 2026-11-03T10:00`): create your own data with unique names, never assume an empty database. Pure logic gets plain JUnit / Vitest tests.
- Jackson 3 (`tools.jackson.*`) is the runtime; annotations stay `com.fasterxml.jackson.annotation.*`. Nullable fields use `org.jspecify.annotations.Nullable`.
- Checkstyle: 120 columns. New Maven dependencies get a version property in `pom.xml`.
- End of phase: `make ci` must pass (`make audit` needs network; if it fails for network reasons only, say so). Update `README.md` (features, configuration, endpoints, data tables) and `docs/architecture.md` when modules, links or tables change.
- If the same failure resists two fix attempts, stop and report instead of improvising.

---

## Phase 0: keep the plan in the repository (done)

This file, committed as `docs(family-hub): add the improvement plan`. The last phase deletes it.

## Phase 1: dialogs stay open on outside click, rename a shopping list (items 5, 1)

**Dialog** `web/src/components/Dialog.tsx`: remove the `onClick` handler of `<dialog>` (the `event.target === ref.current` close). Add one comment: clicking the backdrop does not close the dialog so that a stray click never discards a form being filled in. Escape (`cancel` event) and the ✕ button keep closing.

Test in `web/src/components/components.test.tsx`: render `<Dialog title="Test" onClose={spy}>…</Dialog>`, `fireEvent.click(container.querySelector('dialog')!)` → `spy` not called; click `screen.getByRole('button', { name: 'Fermer', hidden: true })` → called once (jsdom has no `showModal`, so the dialog is not "open" and queries need `hidden: true`).

**Rename**: new `web/src/features/shopping/ListDialog.tsx`: `Dialog` "Modifier la liste" with `Nom` (required, maxLength 80) and `Prévue le` (date), `PUT /api/shopping/lists/{id}` with `{ name, plannedFor: plannedFor || null }`, `useAction` with `refresh: ['shopping', 'dashboard', 'planning']`, `inline: true`, `success: 'Liste modifiée'`. In `ShoppingListPage.tsx`, add a `Modifier` button in the `PageHeader` actions (before `Supprimer`) opening it.

Backend test (new `src/test/java/.../shopping/ShoppingFlowsTest.java extends IntegrationTest`, reused by phases 2 and 3): create a list, `PUT` a new name and date, `GET` returns them.

## Phase 2: choose the ingredients of a recipe to buy (item 2)

Backend:
- `ShoppingController.ListRecipeRequest`: add `@Nullable @Size(max = 100) List<Long> skippedItemIds` (javadoc: ingredients the family already has).
- `ShoppingService.addRecipe(long listId, long recipeId, int servings, Collection<Long> skippedItemIds)`: insert a line only for ingredients whose `itemId` is not skipped; the recipe entry is always recorded. Controller passes `skippedItemIds == null ? List.of() : skippedItemIds`.
- `PlanningService` line ~157 (meal shopping): pass `List.of()`.

UI `ShoppingListPage.tsx` → `AddRecipe`:
- When `recipeId` is set and `servings` is a number from 1 to 50: `useQuery({ queryKey: ['recipes', 'detail', recipeId, servings], queryFn: GET /api/recipes/{id}?servings= })` (endpoint exists, scales quantities).
- Show the ingredients as checkboxes (checked by default), label `${itemName} · ${formatQuantity(quantity)}`, hint "Décochez ce que vous avez déjà." State `skipped: Set<number>`, reset when the recipe changes.
- Body: `{ recipeId, servings, skippedItemIds: [...skipped] }`.

Test (`ShoppingFlowsTest`): add "Crêpes" (seeded recipe, look it up by name like `CrossModuleFlowsTest.recipeId`) skipping the item id of "Farine" → rows have no Farine, `recipes` contains Crêpes; other ingredients present.

## Phase 3: override what is bought for a row (items 3, 4)

Model: a row (item + member) keeps its **needed** quantities (sum of manual and recipe lines, unchanged) and may carry a **purchase**: what will actually be bought, with an optional estimated price. The purchase drives the displayed quantity and the estimate.

Migration `src/main/resources/db/migration/V8__shopping_purchase.sql`:
```sql
-- What the family will actually buy for a row of a list when it differs from what the lines add up
-- to (4 pinches of salt needed, 1 pack bought). A row is an item for a member, or for the household.
CREATE TABLE shopping_purchase (
    id          INTEGER PRIMARY KEY,
    list_id     INTEGER NOT NULL REFERENCES shopping_list (id) ON DELETE CASCADE,
    item_id     INTEGER NOT NULL REFERENCES item (id),
    member_id   INTEGER REFERENCES member (id) ON DELETE CASCADE,
    quantity    REAL    NOT NULL CHECK (quantity > 0),
    unit        TEXT    NOT NULL,
    price_cents INTEGER CHECK (price_cents >= 0),
    note        TEXT
);
CREATE UNIQUE INDEX shopping_purchase_row ON shopping_purchase (list_id, item_id, IFNULL(member_id, 0));
```

Backend:
- New public record `shopping/Purchase.java`: `Purchase(Quantity quantity, @Nullable Long priceCents, @Nullable String note)`.
- `ShoppingRow`: add `@Nullable Purchase purchase` right after `quantities`; javadoc: `quantities` = needed, `purchase` = what will be bought, `estimatedCents` uses the purchase when present.
- `ShoppingRows`: make `RowKey` a `public record`; `build(lines, items, recipeNames, Map<RowKey, Purchase> purchases)`. Estimate: purchase present → `priceCents` if set, else `item.estimateCents(purchase.quantity())` (null when not convertible); no purchase → current logic. Update `ShoppingRowsTest` calls (`Map.of()`).
- `ShoppingRepository`: `Map<ShoppingRows.RowKey, Purchase> findPurchases(long listId)`; `void savePurchase(long listId, long itemId, @Nullable Long memberId, Purchase p)` (DELETE `… AND member_id IS ?` then INSERT); `int deletePurchase(listId, itemId, memberId)`; `void deleteOrphanPurchases(long listId)`:
  ```sql
  DELETE FROM shopping_purchase WHERE list_id = ? AND NOT EXISTS (SELECT 1 FROM shopping_line l
    WHERE l.list_id = shopping_purchase.list_id AND l.item_id = shopping_purchase.item_id
      AND l.member_id IS shopping_purchase.member_id)
  ```
- `ShoppingService`: `get` passes purchases to `build`; `setPurchase(listId, itemId, memberId, Purchase)` (requireOpen; NotFound "Article absent de la liste." when the row has no line; trims note to null); `clearPurchase(...)`; `removeRow` also deletes the purchase; `removeRecipe` calls `deleteOrphanPurchases`.
- `ShoppingController`: `record PurchaseRequest(@Positive double quantity, @NotNull Unit unit, @Nullable @PositiveOrZero Long priceCents, @Nullable @Size(max = 120) String note)`; `PUT /{id}/items/{itemId}/purchase` and `DELETE /{id}/items/{itemId}/purchase`, both with `@RequestParam(required = false) @Nullable Long memberId`, returning `ShoppingList`.

UI:
- New `web/src/features/shopping/RowDialog.tsx` "Modifier l'article": shows `Besoin : …` (needed quantities joined by " + ", and the recipe names); fields `Quantité à acheter`, `Unité` (`Select` over `UNITS`), `Prix estimé (€)` optional (`parseMoney` / `centsToInput` from `lib/format.ts`), `Note`. Footer: `Revenir au calcul` (DELETE, shown only when a purchase exists) and `Enregistrer` (PUT). Defaults: purchase values, else first needed quantity.
- `ShoppingListPage.tsx`: on open lists, the row's text block becomes a button opening `RowDialog`. Displayed quantity: `row.purchase ? formatQuantity(row.purchase.quantity) : row.quantities.map(formatQuantity).join(' + ')`; when a purchase exists, details add `besoin ${needed}` and the purchase note.

Tests: `ShoppingRowsTest` (purchase with price replaces the estimate; purchase without price converts through the item unit; purchase in a unit of another dimension without price → null). `ShoppingFlowsTest`: add "Sel" manually as 4 PINCH (no seeded recipe uses it; catalog unit KG) → row unpriced; PUT purchase `1 PACK, 120` → `purchase` set, `estimatedCents` 120, `unpricedRows` decreases; DELETE → back; removing the Sel row then re-adding Sel shows no purchase; a purchase on "Beurre" coming only from the "Crêpes" recipe disappears when the recipe is removed (re-adding Beurre by hand shows no purchase).

## Phase 4: to-do start date, to-dos in the planning (item 9)

Migration `V9__todo_start.sql`: `ALTER TABLE todo_item ADD COLUMN start_at TEXT;` with a comment (optional start of the work, same text format as `due_at`).

Backend (`todo` module only; the planning page merges both sources in the UI, so no planning→todo dependency):
- `TodoItem`: add `@Nullable LocalDateTime startAt` before `dueAt`. `TodoService.TodoItemData` and `TodoController.TodoItemRequest`: same. Update every constructor call (`TodoController.toData`, `TodoService.setDone`/`validated`, `TodoRepository.map`/`insertItem`/`updateItem`, column `start_at`).
- `validated`: truncate `startAt` to the minute; both set and start after due → `InvalidRequestException("Le début doit précéder l'échéance.")`.
- `setDone` on a repeating item: `nextDue = repeat.next(dueAt, now)`; `nextStart = startAt == null ? null : nextDue.minus(Duration.between(startAt, dueAt))`.
- `TodoRepository.findScheduled(LocalDateTime begin, LocalDateTime end)`, done items included:
  ```sql
  SELECT * FROM todo_item
  WHERE COALESCE(start_at, due_at) IS NOT NULL
    AND COALESCE(start_at, due_at) < :end AND COALESCE(due_at, start_at) >= :begin
  ORDER BY COALESCE(start_at, due_at), id
  ```
- `TodoService.scheduled(LocalDate from, LocalDate to)`: `to` before `from` or more than 400 days → `InvalidRequestException("Période invalide (400 jours au plus).")`; begin `from.atStartOfDay()`, end `to.plusDays(1).atStartOfDay()`.
- `GET /api/todo/scheduled?from=&to=` → `List<TodoItem>`.

UI:
- Move `ItemDialog` from `TodoPage.tsx` to `web/src/features/todo/ItemDialog.tsx` (exported). Add `Début` (date + time, default time 09:00) before `Échéance`. Footer: when editing, `Marquer faite` / `Rouvrir` (PUT `/done`). Refresh areas `['todo', 'dashboard']`.
- `TodoPage.tsx`: list shows `du {start} au {due}` when `startAt` is set.
- `web/src/lib/calendar.ts`: replace the `Occurrence` parameter type with
  ```ts
  export type CalendarItem =
    | { kind: 'entry', key: string, start: string, end: string, allDay: boolean, occurrence: Occurrence }
    | { kind: 'task', key: string, start: string, end: string, allDay: false, task: TodoItem }
  export function entryItem(o: Occurrence): CalendarItem      // key `entry-${o.entry.id}-${o.start}`, allDay from o.entry
  export function taskItem(t: TodoItem): CalendarItem | null  // start = startAt ?? dueAt, end = dueAt ?? startAt; null when neither; key `task-${t.id}`
  ```
  `touchesDay`, `isBanner` (reads `item.allDay`), `layoutDay`, `dayAgenda` take `CalendarItem`; `Placement.occurrence` becomes `Placement.item`. A task with start and due on different days is a banner; a due-only task is an instant (20-minute minimum height already applies).
- `labels.ts`: `TASK_COLORS: Labels<Schemas['Priority']>` on a white/surface background with a coloured left border (HIGH red-600 + bold, NORMAL slate-500, LOW slate-300 + muted text) so tasks differ from filled planning chips; `taskColor(priority)`.
- `WeekView.tsx` / `MonthView.tsx`: props `items: CalendarItem[]`, `onOpen(item)`. Task chip: `☐`/`☑` prefix, `!` prefix for HIGH, line-through when done, `title`/`aria-label` "Tâche · priorité {PRIORITIES[p]}".
- `PlanningPage.tsx`: second query `['todo', 'scheduled', from, to]`; `items = [...occurrences.map(entryItem), ...tasks.map(taskItem).filter(Boolean)]`; member filter: entries by `memberIds`, tasks by `assigneeId`; `Modal` gains `{ task }` → `ItemDialog` (lists from `['todo', 'lists']`).

Tests: `calendar.test.ts` (existing cases through `entryItem`; `taskItem` for due-only, same-day span, multi-day banner, none → null). New `todo/TodoFlowsTest extends IntegrationTest`: create with start and due, `scheduled` window includes and excludes it; start after due → 400; completing a weekly item with a start creates the next one with both dates shifted by a week.

## Phase 5: restore a backup, automate a fresh instance (item 8)

Backend, module `admin` (may now depend on `family` for `MemberService.setupRequired()`):
- New `admin/internal/RestoreService.java` (not `@Transactional`), `@WithSpan`, injects `AppProperties` (data dir), `JdbcTemplate`, `org.flywaydb.core.Flyway`, `MemberService`, `Clock`:
  1. Stream the body to `dataDir/restore-<uuid>.db`, at most 200 MB (count bytes; above → `InvalidRequestException("Fichier trop volumineux.")`). Always delete it in `finally`.
  2. Validate with a separate `DriverManager.getConnection("jdbc:sqlite:" + file)`: `PRAGMA integrity_check` = `ok`; `SELECT version FROM flyway_schema_history WHERE success = 1` → max as integer; `SELECT COUNT(*) FROM member WHERE role = 'PARENT' AND username IS NOT NULL AND password_hash IS NOT NULL` ≥ 1. Any `SQLException` or failed check → `InvalidRequestException("Ce fichier n'est pas une sauvegarde family-hub valide.")`. Backup version above `flyway.info().current().getVersion()` → `ConflictException("Sauvegarde d'une version plus récente : mettez family-hub à jour avant de restaurer.")`.
  3. Replace the live database through SQLite's online backup API on the pool's single connection: `jdbcTemplate.execute((ConnectionCallback<Void>) c -> { try (Statement s = c.createStatement()) { s.executeUpdate("restore from '" + file + "'"); } return null; })` (sqlite-jdbc extension; refuse a path containing `'`).
  4. `flyway.migrate()` (brings an older backup to the current schema). Log `backup.bytes`.
- New `admin/internal/RestoreController.java`:
  - `POST /api/admin/restore` (parents, via the existing `/api/admin/**` rule), `consumes = application/octet-stream`, reads `HttpServletRequest.getInputStream()`, 204. Document the body for springdoc with `@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))`.
  - `POST /api/setup/restore`, same body; only while `members.setupRequired()`, else `ConflictException("La famille est déjà configurée : connectez-vous pour restaurer.")`.
- `family/internal/SecurityConfig`: add `/api/setup/restore` to the `permitAll` matchers.

UI:
- `FamilyPage.tsx` → `Backup`: `Restaurer une sauvegarde` (file input `.db`), `confirm` "Toutes les données seront remplacées par celles de la sauvegarde, y compris les comptes et mots de passe.", then `api().POST('/api/admin/restore', { body: file as never, bodySerializer: (b) => b, headers: { 'Content-Type': 'application/octet-stream' } })`; on success notify "Sauvegarde restaurée : reconnectez-vous." and `refresh('session')` (the restored database may not know the current token).
- `AccessPages.tsx` (first-run setup screen): "Ou restaurer une sauvegarde" with the same upload to `/api/setup/restore`, then `refresh('session')` → login screen.

Tests:
- `admin/RestoreTest extends IntegrationTest`: parent token, download backup bytes, create a to-do list with a unique name, POST restore with the bytes → 204, log in again, the list is gone; garbage bytes → 400; child → 403; `/api/setup/restore` with members present → 409.
- `admin/RestoreOnFreshInstanceTest extends IntegrationTest` with `@TestPropertySource(properties = "spring.application.name=family-hub-fresh")` (forces its own context, so a fresh database): build a backup in a temp file with `Flyway.configure().dataSource("jdbc:sqlite:" + f, null, null).locations("classpath:db/migration").load().migrate()` and insert a parent (`INSERT INTO member (name, color, role, username, password_hash, created_at)` with `PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(...)`); POST it to `/api/setup/restore` without token → 204; `/api/session` → `setupRequired=false`; login with that parent → 200.

Docs: README Operations table (both endpoints), a "Backup, restore and automation" section with:
```bash
TOKEN=$(curl -fsS -X POST https://nas.example/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"alex","password":"…"}' | jq -r .token)
curl -fsS -H "Authorization: Bearer $TOKEN" https://nas.example/api/admin/backup -o family-hub.db
curl -fsS -X POST --data-binary @family-hub.db -H 'Content-Type: application/octet-stream' \
  https://nas.example/api/setup/restore   # fresh instance only, before exposing it
```
and a note that every other setting can be scripted through the documented API (`/v3/api-docs`). `docs/architecture.md`: admin module row, security section (restore on a fresh instance).

## Test samples (provided by the user before phase 6)

Real inputs make the parsers right; OCR output varies with the Tesseract version and the photo, so tests are split in two levels.

| Sample | How many | Where | In git |
|---|---|---|---|
| Shopping list photos: handwritten, printed or typed, phone notes screenshot | 3 to 5, each with `<name>.expected.txt` (one `quantity;UNIT;name` per line, as the family reads it, e.g. `2;KG;Pommes`) | `src/test/resources/samples/shopping/` | Yes (no personal data) |
| To-do list photos: handwritten, whiteboard or paper, phone notes screenshot | 3 to 5, each with `<name>.expected.txt` (one `title;dd/MM or empty;priority or empty` per line, e.g. `Appeler le dentiste;12/11;HIGH`) | `src/test/resources/samples/todo/` | Yes, if they hold nothing private |
| Crédit Agricole OFX and CSV exports (one month) | 1 each | `src/test/resources/samples/private/statements/` | **No** (git-ignored) |
| Crédit Agricole PDF statement, photo of a paper statement | 1 each, each with `<name>.expected.csv` (`day;amountCents;EXPENSE|INCOME;label fragment`, a few lines are enough) | same private folder | **No** |

Implementation of the test levels (phase 6 for the infrastructure and shopping, phase 7 for statements, phase 8 for to-dos):
- `apps/family-hub/.gitignore`: add `src/test/resources/samples/private/`. Bank data never enters git, even anonymised: the implementer derives **synthetic** fixtures from the private files (same layout, columns, encoding and header lines; invented merchants, amounts and account numbers) under `src/test/resources/samples/statements/`.
- `make ocr-fixtures` (needs `make image`): for each committed photo in `shopping/` and `todo/`, `docker run --rm -v $(CURDIR)/src/test/resources/samples:/samples:ro --entrypoint tesseract $(IMAGE) /samples/<folder>/<name>.jpg stdout -l fra --psm 4 tsv > …/<name>.tsv` (psm 6 for statements). The captured `.tsv` files are committed: they are Tesseract's real output, replayed deterministically.
- **Level 1, in `make ci` (deterministic):** `ShoppingSamplesTest` reads each committed `.tsv` through `TsvParser` then `ListTextParser` and compares with `.expected.txt`; statement parsers run on the synthetic fixtures. Tolerance: the first line of an expected file may be `# min-recall: 0.6` (share of expected lines found with the same item, quantity and unit, or the same day, amount and direction); default 1.0 for typed text, OFX and CSV.
- **Level 2, `make test-ocr` (local, needs `tesseract` and French data):** `OcrAcceptanceTest` with `@EnabledIfEnvironmentVariable(named = "FAMILYHUB_OCR_TESTS", matches = "1")` runs the full `DocumentReader` pipeline on every photo and PDF in `samples/` and in `samples/private/` when it exists, prints the recall per file and fails below `min-recall`. Target: `FAMILYHUB_OCR_TESTS=1 $(MVN) test -Dtest='OcrAcceptanceTest' -Dsurefire.failIfNoSpecifiedTests=false`. Skipped in CI, where Tesseract is absent.
- README "Usage": document `make ocr-fixtures` and `make test-ocr`, and the private samples folder.

## Phase 6: local OCR module and shopping list import (item 7)

### Runtime and build
- `Dockerfile` runtime stage: extend the existing `RUN apk upgrade …` with `apk add --no-cache tesseract-ocr tesseract-ocr-data-fra` (comment: local text recognition of photos, French data).
- `Makefile` `smoke`: also check `docker run --rm --entrypoint tesseract $(IMAGE) --list-langs | grep -qx fra`.
- `pom.xml`: `org.apache.pdfbox:pdfbox`, latest 3.0.x, version property `pdfbox.version`.
- `application.yml`: `app.ocr.command: ${APP_OCR_COMMAND:tesseract}`, `app.ocr.language: ${APP_OCR_LANGUAGE:fra}`, `app.ocr.timeout: ${APP_OCR_TIMEOUT:90s}`.
- `docker-compose.yml`: default `MEM_LIMIT` 768m (Tesseract runs beside the JVM); `.env.example` and README configuration table updated. README usage: local `make run` needs `tesseract` + French data installed, otherwise photo reading answers 503 and the rest works.

### `shared` additions
- `shared/Upload.java`: `record Upload(String fileName, byte[] bytes)` with `static Upload fromBase64(String fileName, String base64, int maxBytes)` → `InvalidRequestException("Fichier illisible.")` / `("Fichier trop volumineux (10 Mo au plus).")`; `boolean isPdf()` (starts with `%PDF`), `boolean isImage()` (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF….WEBP`). Detection by content, never by the declared type.
- `shared/UnavailableException.java` + mapping to 503 in `shared/web/ApiExceptionHandler`.

### Module `ocr` (new; depends on `shared` only)
Public API (root package):
- `Word(String text, double left, double right)`: horizontal extent (pixels for photos, points for PDFs).
- `TextLine(int page, List<Word> words)` with `String text()` (words joined by a space).
- `Layout { TABLE, LIST }`: TABLE keeps rows across columns (Tesseract `--psm 6`), LIST one entry per line (`--psm 4`).
- `DocumentReader` (`@Service` class): `boolean photosSupported()`; `List<TextLine> read(Upload upload, Layout layout)`: PDF → text layer; PDF without text → render pages (at most 10, else `InvalidRequestException`) at 300 dpi grey and OCR each; image → OCR; anything else → `InvalidRequestException("Format non pris en charge : photo (JPEG, PNG, WebP) ou PDF.")`. Counter `familyhub.ocr.reads{source=photo|pdf_text|pdf_scan, outcome}` and a log with the duration (never the text).

Internals (`ocr/internal`):
- `OcrProperties` record, `@ConfigurationProperties("app.ocr")` (scanned by `@ConfigurationPropertiesScan`).
- `Tesseract` (`@Component`): `available()` runs `<command> --list-langs` once (10 s timeout), true when the language is listed. `recognize(byte[] image, Layout)`: temp dir, write the image, `new ProcessBuilder(command, in, outBase, "-l", language, "--psm", "6"|"4", "tsv")` (argument list, never a shell), `environment().put("OMP_THREAD_LIMIT", "1")`, stdout/stderr `Redirect.DISCARD`, `waitFor(timeout)` else `destroyForcibly` + `UnavailableException("Lecture trop longue : essayez une photo plus nette ou plus petite.")`; read `outBase.tsv`; one OCR at a time (`Semaphore(1)`); delete the temp dir. Unavailable → `UnavailableException("La lecture de photos n'est pas disponible sur ce serveur (Tesseract absent).")`.
- `TsvParser` (pure): skip the header; keep rows with `level == 5`, `conf != -1`, non-blank text → `Word(text, left, left + width)`; group by `(page_num, block_num, par_num, line_num)` in order.
- `PdfText`: PDFBox 3 `Loader.loadPDF(bytes)`; subclass `PDFTextStripper` with `setSortByPosition(true)`, override `writeString(String, List<TextPosition>)` to split positions into words at blank characters (left `getXDirAdj()`, right `getXDirAdj() + getWidthDirAdj()`, y `getYDirAdj()`, current page); then `LineGrouper`. `renderPages(bytes)` with `PDFRenderer.renderImageWithDPI(i, 300, ImageType.GRAY)` → PNG bytes.
- `LineGrouper` (pure): words with page and y → lines sorted by page, y, then left; a word joins a line when its y is within 2 points.
- `OcrController`: `GET /api/ocr/status` → `record OcrStatus(boolean photos)` (authenticated).

### Shopping import (`shopping` now depends on `ocr`)
- `CatalogService.findByName(String name)` → `Optional<Item>` (column is `COLLATE NOCASE`).
- Public records: `ProposedLine(String text, @Nullable Long itemId, String name, double quantity, Unit unit, @Nullable CatalogService.ItemData newItem)`.
- `shopping/internal/ListTextParser` (pure): `List<ProposedLine> parse(List<String> lines, List<Item> catalog)`:
  1. Strip leading bullets `^[-*•·□☐>]+\s*` and numbering `^\d+[.)]\s+`; skip lines with fewer than 2 letters.
  2. Quantity and unit: leading `^(\d+(?:[.,]\d+)?)\s*(unitWord)?\s+(.+)$`, else trailing `^(.+?)\s+[x×]?\s*(\d+(?:[.,]\d+)?)\s*(unitWord)?$`. Unit words (case- and accent-insensitive, optional plural): g, gr, gramme → G; kg, kilo → KG; ml → ML; cl → CL; l, litre → L; paquet, pqt, boîte, boite, sachet, pack → PACK; botte → BUNCH; pincée → PINCH; pièce, pc → PIECE; `x` or none → the item's default unit. Missing quantity → 1.
  3. Name key: lowercase, accents removed (`Normalizer` NFD, drop `\p{M}`), œ→oe, æ→ae, non-letters → space, final `s`/`x` dropped on words longer than 3 letters. Exact key match → catalog item; else best Dice bigram similarity ≥ 0.75; else `newItem = ItemData(capitalised name, GOODS, SUPERMARKET, OTHER, parsed unit or PIECE, null)`.
- `ShoppingService.readList(@Nullable Upload photo, @Nullable String text)`: exactly one; photo → `documents.read(photo, Layout.LIST)` → line texts; text → split on newlines (at most 200 lines).
- `ShoppingService.importLines(long listId, List<ImportLine> lines)` (`@Transactional`, requireOpen): `itemId` → `catalog.get`; `newItem` → `catalog.findByName` or `catalog.create`; `repository.insertLine(listId, itemId, null, null, quantity, null)`.
- Controller: `POST /api/shopping/imports/read` body `ReadListRequest(@Nullable @Valid UploadRequest photo, @Nullable @Size(max = 10000) String text)` → `List<ProposedLine>`; `POST /api/shopping/lists/{id}/import` body `ImportRequest(@NotEmpty @Size(max = 200) List<@Valid ImportLineRequest> lines)`, `ImportLineRequest(@Nullable Long itemId, @Nullable @Valid NewItemRequest newItem, @Positive double quantity, @NotNull Unit unit)` (exactly one of itemId/newItem, else 400), `NewItemRequest` with the same constraints as `ItemController.CatalogItemRequest` (name `@NotBlank @Size(max = 80)`). `UploadRequest(@NotBlank @Size(max = 255) String fileName, @NotBlank @Size(max = 14_000_000) String data)` is declared in each controller that needs it (records of `internal` packages cannot be shared).

UI:
- `web/src/lib/photo.ts`: `fitWithin(width, height, max)` (pure, tested) and `encodeUpload(file)`: images → `createImageBitmap` → canvas at most 2500 px on the long edge → `toBlob('image/jpeg', 0.9)` (also converts iPhone HEIC in Safari); other files as is; base64 via `FileReader.readAsDataURL` without the prefix; over 10 MB → `Error('Fichier trop volumineux (10 Mo au plus).')`.
- `web/src/features/shopping/ImportDialog.tsx` (`wide`, props `listId`, `onDone`): step 1 `Photo` (file input `accept="image/*,application/pdf"`, hidden when `GET /api/ocr/status` says `photos: false`) or `Texte` (textarea "Une ligne par article"), button `Lire`; step 2 one row per proposal: include checkbox, original text (small), item (`ItemPicker` prefilled; when no match, "Nouvel article" with kind, store type and aisle selects), quantity, unit; `Ajouter N articles` → import → `refresh('shopping', 'catalog', 'dashboard')`.
- `ShoppingListPage.tsx`: `Importer (photo ou texte)` in the "Ajouter un article" card. `ShoppingListsPage.tsx`: `Créer depuis une photo ou un texte` creates the list (name from the form or `Courses du ${formatDate(today())}`), opens `ImportDialog` for it, then navigates to the list.

Tests: `TsvParserTest` (fixture string), `LineGrouperTest`, `PdfTextTest` (build a two-line PDF with PDFBox `PDPageContentStream` and Standard 14 Helvetica, assert words and left positions), `ListTextParserTest` ("2 kg pommes" → Pommes 2 KG; "6 oeufs" → Œufs 6 PIECE; "lait" → Lait 1 L; "- Beurre 250g" → Beurre 250 G; "Papier toilette x2" → 2 PACK; "Tomate" → Tomates; "1. Riz" → Riz; "Lessive" → new item), `photo.test.ts` (`fitWithin`), `ShoppingSamplesTest` (level 1 on `samples/shopping/`). `ShoppingImportTest extends IntegrationTest` with `@MockitoBean DocumentReader`: photo read → proposals; text read without OCR; import with one existing and one new item → rows and catalog updated; invalid base64 → 400; reader throwing `UnavailableException` → 503.

Docs: architecture (module `ocr`, dependencies `shopping → ocr`, links table), README (features, configuration `APP_OCR_*`, `MEM_LIMIT`, endpoints).

## Phase 7: bank statement import (item 6)

Prerequisite: the private statement samples (see "Test samples"). Derive the synthetic fixtures from them first, then write the parsers against those fixtures, and finish with `make test-ocr` on the private PDF and photo.

Migration `V10__accounting_import.sql`: rebuild `ledger_transaction` to allow `source_type = 'IMPORT'` (SQLite cannot alter a CHECK; no table references `ledger_transaction`): `CREATE TABLE ledger_transaction_new (…same columns as V7, CHECK with 'IMPORT'…)`, `INSERT INTO ledger_transaction_new (id, day, label, amount_cents, category_code, account_id, status, source_type, source_ref, supplier, created_by, approved_by, approved_at, created_at) SELECT <same columns> FROM ledger_transaction`, `DROP TABLE ledger_transaction`, `ALTER TABLE ledger_transaction_new RENAME TO ledger_transaction`, recreate `ledger_transaction_day`. `SourceType.IMPORT`; UI `SOURCE_TYPES.IMPORT = 'Relevé'`.

Parsers in `accounting/internal` (pure, one test class each), all producing `StatementLine(LocalDate day, String label, long amountCents, Direction direction, @Nullable String fitId)`:
- `OfxParser.parse(byte[])`: decode windows-1252 when the header says `CHARSET:1252` or the bytes are not valid UTF-8; each `<STMTTRN>` block up to `</STMTTRN>` or the next `<STMTTRN>`; tags `<(DTPOSTED|TRNAMT|FITID|NAME|MEMO)>([^<\r\n]*)` (OFX 1.x leaves tags unclosed); day = first 8 digits of DTPOSTED; amount `-45.20` or `-45,20` → `BigDecimal` → absolute cents, negative = EXPENSE; label = NAME plus MEMO when not already contained; XML entities decoded.
- `CsvStatementParser.parse(byte[])`: decode UTF-8 strictly, else windows-1252; separator `;`, `,` or tab (most frequent in the first lines); quoted cells with `""` and line breaks; header row = first row with a cell starting with `date` and either `débit`/`crédit` cells or a `montant` cell; prefer "Date" / "Date opération" over "Date valeur"; label column `libell|description|intitul`; dates `dd/MM/yyyy`, `dd/MM/yy`, `yyyy-MM-dd`; French amounts (spaces and non-breaking spaces removed, comma decimal); debit column → EXPENSE, credit → INCOME, single signed amount column → by sign; rows without a valid date and amount are skipped; whitespace in labels collapsed.
- `PrintedStatementParser.parse(List<TextLine>, LocalDate today)` (PDF text layer or photo, `Layout.TABLE`): statement end = latest `dd.MM.yyyy` or `dd/MM/yyyy` found in the document, else today; header line with words `débit` and `crédit` gives the column centres; an operation starts with a `dd.MM` / `dd/MM` word (a second date word, the value date, is skipped); amount = last word matching `\d{1,3}(?:[ . ]?\d{3})*,\d{2}`, merged with a preceding `\d{1,3}` word (split thousands); label = words in between; direction = credit when the amount centre is nearer the credit column, else debit (debit when no header); lines without date or amount extend the previous label; lines starting with SOLDE, TOTAL, REPORT, ANCIEN or NOUVEAU are skipped and end the continuation; year = end year, minus one when the date falls more than 7 days after the end.
- `LabelKeys.merchant(String label)`: upper-case ASCII; remove `PAIEMENT PAR CARTE`, `CARTE X\d+`, `CB`, `PRLV SEPA`, `PRELEVEMENT`, `VIR SEPA`, `VIREMENT`, `RETRAIT DAB`, dates, `X\d{4}` card numbers, digit runs of 3 or more; collapse spaces; keep the first 3 words.
- `Categorizer`: (1) history: approved transactions of the last 18 months, `merchant(label)` → most frequent category; (2) keyword table (constant list, applied only when the code exists with the line's direction): CARREFOUR, LECLERC, AUCHAN, INTERMARCHE, LIDL, ALDI, MONOPRIX, FRANPRIX, CASINO, SUPER U, HYPER U → `courses`; EDF, ENGIE, TOTALENERGIES → `energie`; ORANGE, SFR, BOUYGUES, FREE → `telecom`; SNCF, ESSO, SHELL, BP, AUTOROUTE, APRR, PEAGE → `transport`; PHARMACIE, DOCTOLIB, MUTUELLE → `sante`; DGFIP, IMPOT → `impots`; LEROY MERLIN, CASTORAMA, BRICOMARCHE, JARDILAND, TRUFFAUT, IKEA → `maison`; DECATHLON, INTERSPORT → `sport`; NETFLIX, SPOTIFY, DISNEY, CINEMA, FNAC → `loisirs`; KIABI, ZARA, H&M, VERTBAUDET → `vetements`; ASSURANCE, MAIF, MACIF, MAAF, AXA, GROUPAMA, PACIFICA → `assurances`; LOYER, PRET IMMO → `logement`; income: CAF → `allocations`, SALAIRE → `salaires`; (3) none → null.

Service and API:
- Public records: `StatementProposal(String format /* OFX, CSV, PDF, PHOTO */, List<ProposedTransaction> lines)`; `ProposedTransaction(LocalDate day, String label, long amountCents, Direction direction, @Nullable String categoryCode, String sourceRef, boolean alreadyImported, @Nullable Long possibleDuplicateId)`.
- `AccountingService.readStatement(Upload file, @Nullable Long accountId)` (`accounting` now depends on `ocr`): format by content (`OFXHEADER` or `<OFX>` → OFX; PDF → `documents.read(file, TABLE)` → printed, format PDF; image → same, PHOTO; else CSV; nothing parsed → `InvalidRequestException("Aucune opération reconnue : OFX, CSV, PDF ou photo.")`). `sourceRef` = `ofx:<accountId or 0>:<FITID>` when present, else `line:` + SHA-256 hex of `day|amount|direction|merchant key|n` (n = rank of identical keys within the file). `alreadyImported` = `findBySource(IMPORT, ref)` present; `possibleDuplicateId` = a non-IMPORT transaction with the same amount and direction within 3 days.
- `AccountingService.importStatement(ImportData)` (`@Transactional`) → `ImportResult(int created, int skipped, List<Category> categoriesCreated)`: status PENDING or APPROVED only; account validated; `newCategory(name, direction, group)` reuses a category with the same name (case-insensitive) and direction, else `createCategory` once per name; line already imported → skipped; else `insertTransaction(TransactionRow(…, SourceType.IMPORT, sourceRef, supplier = merchant key or null, createdBy = current member))`, then `approve` when APPROVED (as `create` does).
- Controller (parents only through `/api/accounting/**`): `POST /api/accounting/imports/read` body `{ file: UploadRequest, accountId }` → `StatementProposal`; `POST /api/accounting/imports` body `{ accountId, status, lines: [{ day, label, amountCents, categoryCode | newCategory { name, direction, group }, sourceRef }] }` (max 500 lines, same field limits as `TransactionRequest`).

UI `web/src/features/accounting/StatementImportDialog.tsx` (`wide`), opened by `Importer un relevé` next to `Nouvelle opération` in `TransactionsTab.tsx`: step 1 account select and file input `accept=".ofx,.csv,.pdf,image/*"` with the hint "Crédit Agricole : téléchargez vos opérations au format OFX (le plus fiable) ou CSV ; un relevé PDF ou une photo marche aussi."; step 2 summary and one row per line: include checkbox (checked unless already imported or possible duplicate), date, label, amount, `Dépense`/`Revenu` toggle, category select filtered by direction with `Nouvelle catégorie…` (name + group for expenses), badges `Déjà importée` (disabled) and `Doublon possible`; status select `Approuvée` (default) / `À approuver`; checked lines without category block the submit with "Choisissez une catégorie pour chaque ligne cochée."; success notifies `${created} opérations importées` and refreshes `ACCOUNTING_AREAS`.

Tests: `OfxParserTest`, `CsvStatementParserTest` (Crédit Agricole layout: preamble lines, `;`, debit/credit columns, windows-1252 accents), `PrintedStatementParserTest` (header columns, continuation, split thousands, skipped SOLDE lines, December/January year), `LabelKeysTest`, `CategorizerTest` (history beats keyword; keyword only with matching direction; unknown → null). `StatementImportTest extends IntegrationTest` with `@MockitoBean DocumentReader`: OFX read → proposals with categories; import APPROVED → transactions with source `IMPORT`; reading the same file again → `alreadyImported`; importing again → skipped; new category created; photo through the mocked reader → proposal; child → 403.

Docs: README (features, endpoints, `ledger_transaction` source types), architecture (links `accounting → ocr`, roadmap phase 2 statement import done; bank synchronisation stays a non-goal).

## Phase 8: import to-dos from a photo or pasted text (item 10)

Reuses phase 6 (`ocr` module, `Upload`, `encodeUpload`) and phase 4 (`TodoItem` fields). `todo` now depends on `ocr`.

Backend:
- Public record `todo/ProposedTask(String text, String title, @Nullable LocalDateTime dueAt, Priority priority)`.
- `todo/internal/TaskTextParser` (pure, takes `LocalDate today`): `List<ProposedTask> parse(List<String> lines, LocalDate today)`:
  1. Strip leading bullets and boxes `^[-*•·□☐>]+\s*`, `^\[\s?\]\s*` and numbering `^\d+[.)]\s+`; skip lines with fewer than 2 letters; skip lines marked done (`^\[[xX]\]`, `☑`, `✓`, `✔` at the start).
  2. Priority HIGH when the line starts or ends with `!` or contains the word `urgent` (case-insensitive); the markers are removed from the title. Otherwise NORMAL.
  3. Due date, removed from the title with its introducing words (`avant le`, `pour le`, `le`, `pour`, `:` and `-` around it): `dd/MM` or `dd/MM/yyyy` (`.` also accepted; without a year, the next such date on or after today); `aujourd'hui`; `demain`; a weekday name (`lundi` … `dimanche`), meaning the next such day after today. Due time 18:00, the UI default.
  4. Title: whitespace collapsed, first letter upper-case, at most 200 characters.
- `TodoService.readTasks(@Nullable Upload photo, @Nullable String text)`: exactly one; photo → `documents.read(photo, Layout.LIST)`; text split on newlines, at most 100 lines.
- `TodoService.importTasks(long listId, List<TodoItemData> tasks)` (`@Transactional`): validates each through the existing `validated` and inserts it; returns the created items.
- `TodoController`: `POST /api/todo/imports/read` body `ReadTasksRequest(@Nullable @Valid UploadRequest photo, @Nullable @Size(max = 10000) String text)` → `List<ProposedTask>`; `POST /api/todo/lists/{id}/import` body `ImportTasksRequest(@NotEmpty @Size(max = 100) List<@Valid ImportTaskRequest> tasks)` with `ImportTaskRequest(@NotBlank @Size(max = 200) String title, @Nullable LocalDateTime dueAt, @Nullable Priority priority, @Nullable Long assigneeId)` → `List<TodoItem>`.

UI:
- `web/src/features/todo/TaskImportDialog.tsx` (`wide`, props `listId`, `onClose`): step 1 `Photo` (file input `accept="image/*,application/pdf"`, hidden when `GET /api/ocr/status` says `photos: false`) or `Texte` (textarea "Une ligne par tâche"), button `Lire`; step 2 one row per proposal: include checkbox, original text (small), title (editable), due date and time, priority select, assignee select (`members` from `useSession`); `Ajouter N tâches` → import → `refresh('todo', 'dashboard', 'planning')`, success `${n} tâches ajoutées`.
- `TodoPage.tsx`: `Importer (photo ou texte)` next to `Tâche détaillée`.

Tests: `TaskTextParserTest` with `today = 2026-11-03` (a Tuesday): "- Appeler le dentiste avant le 12/11" → "Appeler le dentiste", 2026-11-12T18:00; "Payer la cantine !" → HIGH; "demain : sortir les poubelles" → 2026-11-04T18:00; "Réserver le train lundi" → 2026-11-09T18:00; "05/01 renouveler l'assurance" → 2027-01-05T18:00; "[x] déjà fait" skipped; "~" skipped. `TodoSamplesTest` (level 1, committed `.tsv` of `samples/todo/`) and the `todo/` folder in `OcrAcceptanceTest`. `TodoImportTest extends IntegrationTest` with `@MockitoBean DocumentReader`: photo read → proposals; text read without OCR; import → items in the list with due dates and priorities; unknown assignee → 400; empty list → 400.

Docs: README (to-do features, endpoints), architecture (link `todo → ocr`). Last commit removes this file.

---

## Verification

Per phase: `cd apps/family-hub && make ci` (lint, Vitest, JUnit including `ModularityTest` and `OpenApiContractTest`, audit, build). End to end, with `make run` (and `make dev-web`):

| Item | Manual check |
|---|---|
| 1, 5 | Rename a list from its page; click outside an open dialog: it stays open, Escape and ✕ close it |
| 2 | Add "Crêpes" while unchecking Farine: no Farine row, recipe badge present |
| 3, 4 | Add 4 pinches of salt, edit the row to 1 pack at 1,20 €: row and estimate updated; "Revenir au calcul" restores |
| 9 | Task with start Monday 9:00 and due Wednesday 18:00 shows as a banner in the week; a HIGH due-only task shows as a red-bordered chip; clicking opens the task dialog |
| 8 | Download a backup, change data, restore it from *Famille*: data back, sign-in requested. On a fresh `./data`, restore from the setup screen, then log in with the backup's account |
| 7 | `make image && make smoke` (checks Tesseract); in `make image-run`, import a photographed printed list and a pasted text list into a new list |
| 6 | Import a Crédit Agricole OFX, then the same file again (all "Déjà importée"); CSV and PDF statements give the same lines |
| 10 | Paste "Appeler le dentiste avant le 12/11" and "Payer la cantine !" into a to-do list: due date and priority detected; import a photographed to-do list; the dated tasks appear in the planning |
| Samples | `make ocr-fixtures`, then `make ci` (replayed OCR output) and `make test-ocr` (live Tesseract, recall per sample file) |
