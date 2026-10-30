# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**phlox** is a visualizer for EN 16931 electronic invoices (the EU structured e-invoicing standard). It reads UBL (2.1 / 2.5) or CII (D16B / D25A) XML into a syntax-neutral intermediate model, then writes HTML, PDF or plain text in EN, DE, ES, FR or NL.

Both editions of the standard are supported: EN 16931:2017 (UBL 2.1, CII D16B) and EN 16931:2026 (UBL 2.5, CII D25A). The model is a superset of both; terms that only exist in 2026 stay empty for 2017 documents. The edition is detected from BT-24 (`EEN16931Edition` from `com.helger:en16931-basics`) or chosen explicitly via the reader factories (`UblInvoiceReader.fromUbl21()/fromUbl25()/autoDetect()`, `CiiInvoiceReader.fromD16B()/fromD25A()/autoDetect()`).

## Architecture (read this before changing code)

Three-stage pipeline: **Read** (source XML → intermediate model) → **Hold** (in-memory model) → **Write** (model → HTML / PDF / text).

Module dependency rules are strict:
- Readers and writers depend **only** on `phlox-api`, never on each other.
- `phlox-cli` is the only module that wires readers and writers together.
- Do not add cross-module deps to enable shortcuts — consumers must be able to depend on, e.g., HTML-for-UBL without pulling PDF libs.

Modules:
- `phlox-parent-pom` — root BOM, no sources
- `phlox-api` — intermediate model + SPIs + I18N (and the `IPhloxTemplate` marker); depends on `en16931-basics` for `EEN16931Edition`
- `phlox-testfiles` — shared EN 16931 example invoices; regular `jar` (not `test-jar`), consumed with `<scope>test</scope>`
- `phlox-ubl` — UBL 2.1 (EN 2017) **and** 2.5 (EN 2026) reader; public facade `UblInvoiceReader` delegating to package-private `Ubl21InvoiceReader` / `Ubl25InvoiceReader` (factories: `fromUbl21()`, `fromUbl25()`, `autoDetect()`)
- `phlox-cii` — CII D16B (EN 2017) and D25A (EN 2026) reader; public facade `CiiInvoiceReader` delegating to package-private `CiiD16BInvoiceReader` / `CiiD25AInvoiceReader` (factories: `fromD16B()`, `fromD25A()`, `autoDetect()`)
- `phlox-template` — JMustache + commonmark-java parser, the `PhloxTemplate` factories, and the default `.ptpl` resource. All output is template driven; without `RenderContext.setTemplate (...)` the writers use `PhloxTemplate.getDefault ()`. `phlox-html`: `HtmlTemplateEmitter` walks the AST (stylesheet `phlox-default.tcss`, a CSS template with the class prefix `{p}`). `phlox-pdf`: `PdfTemplateEmitter` (inline styling through `ph-pdf-layout-richtext`, block styles through `PdfRenderContext.putBlockStyle`). When adding a BT/BG, extend `default.ptpl` / `PhloxModelRegistry`; a test enforces that the default template shows every registered BT (`PhloxTemplate.getMissingBTs ()`, `EPhloxTemplateCoverageMode`). Layout tags `{{group:}}`, `{{row:}}`, `{{end}}`. See the wiki page `Template-Language` (in `../phlox.wiki`).
- `phlox-theme` — reads the JSON theme (`PhloxThemeReader`, `PhloxTheme`) that defines colours (CSS-like `--variables`, referenced with `var(--name)`), fonts, page layout, block styles and the defaults of the render context. `HtmlRenderContext.applyTheme` / `PdfRenderContext.applyTheme` apply it; `--theme` in the CLI. Depends on `phlox-api` and `phlox-template`. See the wiki page `Themes`. Keys starting with `$` are comments; unknown keys are errors.
- `phlox-html` — HTML5 writer
- `phlox-pdf` — PDF writer (PDFBox via ph-pdf-layout, rich text via ph-pdf-layout-richtext)
- `phlox-text` — plain-text writer: `TextTemplateEmitter` walks the AST (styling stripped, tables in aligned columns, groups stacked), `TextRenderContext.setLineWidth` (default 80, 0 = no wrapping); `applyTheme` applies only the `common` part
- `phlox-cli` — picocli CLI; produces `phlox-cli-full.jar` (shaded fat JAR, main class `PhloxCli`)

## Hard constraints

- **No validation in phlox.** Business-rule checks (e.g. VAT consistency) belong in `phive-rules-en16931`. Do not add them here.
- **Stateless library.** No config files, no environment variables, no system properties at startup. All tuning flows through typed `RenderContext` parameters. Never reach for `System.getProperty(...)` or `System.getenv(...)`.
- **Intermediate model is the contract.** Every EN 16931 Business Group (BG) and Business Term (BT) is always populated. Use empty collections, never `null`. Writers must not have "field not present" branches.
- **`RenderContext` type hierarchy is load-bearing.** `HtmlRenderContext`, `PdfRenderContext` and `TextRenderContext` all extend `AbstractRenderContext`. Writers accept their specific subclass — this is intentional, do not widen to the base type.
- **JSpecify annotations** (`@NonNull` / `@Nullable`) on public types — used in 47 of 51 Java files.
- **forbiddenapis** plugin runs at compile time using `ph-forbidden-apis` signatures. If it fails, fix the call site — do not exclude the rule.

## Build / test

Standard Maven (Java 17+, Maven 3.x). The effective compiler release is 17: the parent POM pins it, so the `maven.compiler.release` property in `pom.xml` has no effect and Java 21+ language features (e.g. pattern-matching `switch`) do not compile. Worth knowing:
- Single module: `mvn -pl phlox-ubl test`
- Single test method: `mvn -Dtest=PdfInvoiceWriterTest#testHelloWorld test`
- CLI deliverable is `phlox-cli/target/phlox-cli-full.jar` (shaded), **not** `phlox-cli.jar`.
- No CI yet. No checkstyle, no formatter config — style comes from `~/.claude/rules/` (Hungarian notation, spaces before parens, Apache license header, ph-commons collections, etc.).

## Tests

- Test fixtures live in `phlox-testfiles/src/main/resources/external/en16931/{2017,2026}/{ubl,cii}/...`.
- Adding regression coverage usually means adding an XML fixture there, not a new Java test class.
- JUnit 4 + AssertJ.
- Reference files: `HtmlMastersTest` (phlox-html), `PdfMastersTest` (phlox-pdf) and `TextMastersTest` (phlox-text) compare the output of `MockPhloxTestFiles.getMasterFiles ()` with `src/test/resources/masters`. After an intended change of the output write them again with `-Dphlox.updateMasters=true` and review the `git diff`. The PDF master is a text description of the layout plus a PDF that is compared with pdfcompare.

## Localization

- Labels in `phlox-api/src/main/resources/phlox-labels_{en,de}.properties`.
- Names of code list values (VAT categories, payment means, countries, units, ...) are not kept here: `DefaultPhloxLabelResolver` asks the `en16931-basics` enums (`getDisplayText (Locale)`) and `UnitCodes` of `ph-codelists-unit`, which fall back to English. Add translations there, not in phlox.
- Default resolver: `DefaultPhloxLabelResolver`. Callers override via `RenderContext.setLabelResolver(...)`.
- Adding a language = adding the `.properties` files plus a resource-bundle provider.

## Other

- `PhloxProgramWatermark.setInstance(...)` is a singleton set once at JVM startup (used for attribution in OSS vs. commercial builds) — not a per-render option.
- Shade plugin in `phlox-cli` uses `ServicesResourceTransformer` to merge SPI registries; preserve that when touching the CLI build.
