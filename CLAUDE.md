# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**phlox** is a visualiser for EN 16931 electronic invoices (the EU structured e-invoicing standard). It reads UBL (2.1 / 2.5) or CII (D16B / D25A) XML into a syntax-neutral intermediate model, then writes HTML, PDF or plain text in EN, DE, ES, FR or NL.

Both editions of the standard are supported: EN 16931:2017 (UBL 2.1, CII D16B) and EN 16931:2026 (UBL 2.5, CII D25A). The model is a superset of both; terms that only exist in 2026 stay empty for 2017 documents. The edition is detected from BT-24 (`EEN16931Edition` from `com.helger:en16931-basics`) or chosen explicitly via the reader factories (`UblInvoiceReader.fromUbl21()/fromUbl25()/autoDetect()`, `CiiInvoiceReader.fromD16B()/fromD25A()/autoDetect()`).

## Architecture (read this before changing code)

Three-stage pipeline: **Read** (source XML → intermediate model) → **Hold** (in-memory model) → **Write** (model → HTML / PDF / text).

Module dependency rules are strict:
- Readers depend **only** on `phlox-api`. Writers depend on `phlox-api`, `phlox-template` and `phlox-theme`, and only their tests use the readers. Readers and writers never depend on each other.
- `phlox-cli` is the only module that wires readers and writers together.
- Do not add cross-module deps to enable shortcuts — consumers must be able to depend on, e.g., HTML-for-UBL without pulling PDF libs.

Modules:
- `phlox-parent-pom` — root BOM, no sources
- `phlox-api` — intermediate model + SPIs + I18N (and the `IPhloxTemplate` marker); depends on `en16931-basics` for `EEN16931Edition`
- `phlox-testfiles` — shared EN 16931 example invoices; regular `jar` (not `test-jar`), consumed with `<scope>test</scope>`
- `phlox-ubl` — UBL 2.1 (EN 2017) **and** 2.5 (EN 2026) reader; public facade `UblInvoiceReader` delegating to package-private `Ubl21InvoiceReader` / `Ubl25InvoiceReader` (factories: `fromUbl21()`, `fromUbl25()`, `autoDetect()`)
- `phlox-cii` — CII D16B (EN 2017) and D25A (EN 2026) reader; public facade `CiiInvoiceReader` delegating to package-private `CiiD16BInvoiceReader` / `CiiD25AInvoiceReader` (factories: `fromD16B()`, `fromD25A()`, `autoDetect()`)
- `phlox-template` — JMustache + commonmark-java parser, the `PhloxTemplate` factories, and the default `.ptpl` resource. All output is template driven; without `RenderContext.setTemplate (...)` the writers use `PhloxTemplate.getDefault ()`. `phlox-html`: `HtmlTemplateEmitter` walks the AST (stylesheet `phlox-default.tcss`, a CSS template with the class prefix `{p}`). `phlox-pdf`: `PdfTemplateEmitter` (inline styling through `ph-pdf-layout-richtext`, block styles through `PdfRenderContext.putBlockStyle`). When adding a BT/BG, extend `default.ptpl` / `PhloxModelRegistry`; a test enforces that the default template shows every registered BT (`PhloxTemplate.getMissingBTs ()`, `EPhloxTemplateCoverageMode`). A value without a field of the model, such as the second occurrence of a 0..1 term, is a `PhloxAdditionalTerm` in `additionalTerms ()` of the invoice, seller, buyer, payment or line (`PhloxReaderHelper.setAndAddRepeated`). Layout tags `{{group:}}`, `{{row:}}`, `{{end}}`, the page header and footer `{{header}}`/`{{footer}}`...`{{end}}` with `{{page}}`/`{{pages}}` (`PhloxPageMarginNode`, PDF page header/footer and HTML `@page` margin boxes, on by default, the header from the second page on; `setShowPageHeader`/`Footer` and `...OnFirstPage`), inline colours `{{color:#rrggbb}}`...`{{endcolor}}` and `{{bg:#rrggbb}}`...`{{endbg}}` (`PhloxSpanNode`). See the wiki page `Template-Language` (in `../phlox.wiki`).
- `phlox-theme` — reads the JSON theme (`PhloxThemeReader`, `PhloxTheme`) that defines colours (CSS-like `--variables`, referenced with `var(--name)`), fonts, page layout, block styles and the defaults of the render context. `HtmlRenderContext.applyTheme` / `PdfRenderContext.applyTheme` apply it; `--theme` in the CLI. Depends on `phlox-api` and `phlox-template`. See the wiki page `Themes`. Keys starting with `$` are comments; unknown keys are errors.
- `phlox-html` — HTML5 writer
- `phlox-pdf` — PDF writer (PDFBox via ph-pdf-layout, rich text via ph-pdf-layout-richtext)
- `phlox-text` — plain-text writer: `TextTemplateEmitter` walks the AST (styling stripped, tables in aligned columns, groups stacked), `TextRenderContext.setLineWidth` (default 80, 0 = no wrapping); `applyTheme` applies only the `common` part
- `phlox-cli` — picocli CLI; produces `phlox-cli-full.jar` (shaded fat JAR, main class `PhloxCli`)

## Hard constraints

- **All data of the XML appears in the output.** This covers content that is valid under the EN 16931 rules (the Schematron in `../phive-rules/phive-rules-en16931`), not everything the XSD allows. An element that a rule says should not be present (`UBL-CR`, `CII-SR`, even with the flag `warning`) is out of scope. A reader, the model, the default template or a writer must not drop a value of the invoice: every occurrence of a repeated element is shown, and the PDF is the reference. If a value cannot be mapped, show it rather than lose it. Only the fillers `None` and `NA` of the UBL binding count as no data. The one accepted gap is a character the font does not have, which the PDF shows as `?`: it is reported through `PdfRenderContext.setMissingCodePointCallback`, by default as a warning in the log. `PdfAllDataShownTest` (phlox-pdf) enforces it over all fixtures; its short list `ALLOWED` names the exceptions with a reason.
- **No validation in phlox.** Business-rule checks (e.g. VAT consistency) belong in `phive-rules-en16931`. Do not add them here.
- **Stateless library.** No config files, no environment variables, no system properties at startup. All tuning flows through typed `RenderContext` parameters. Never reach for `System.getProperty(...)` or `System.getenv(...)`.
- **Intermediate model is the contract.** Collections are never `null`, only empty. Optional single BGs (BG-10, 11, 13, 14, 16; line BG-26, 29, 30, 31) and absent BTs are `null`; the template decides with `{{?...}}`. Emitters have no logic per field.
- **The invoice is untrusted input.** Templates, themes and CLI arguments are trusted. A value must never become Markdown, a layout tag or a link: every substituted value goes through the escaping in `PhloxInvoiceMustacheContext`, which also strips `SENTINEL_MARKER` (U+FDD0), so layout sentinels cannot be forged. Log values through `PhloxLogHelper.sanitise`. No regex with quadratic time on invoice text.
- **`RenderContext` type hierarchy is load-bearing.** `HtmlRenderContext`, `PdfRenderContext` and `TextRenderContext` all extend `AbstractRenderContext`. Writers accept their specific subclass — this is intentional, do not widen to the base type.
- **JSpecify annotations** (`@NonNull` / `@Nullable`) on public types.
- **forbiddenapis** plugin runs at compile time using `ph-forbidden-apis` signatures. If it fails, fix the call site — do not exclude the rule. Only `phlox-cli` skips the plugin, see the comment in its POM.

## Build / test

Standard Maven (Java 17+, Maven 3.x). The compiler release is 17 through `java.version` in `pom.xml`, so Java 21+ language features (e.g. pattern-matching `switch`) do not compile. Worth knowing:
- Single module: `mvn -pl phlox-ubl test`
- Single test method: `mvn -Dtest=PdfInvoiceWriterTest#testRenderUblToPdf test`
- CLI deliverable is `phlox-cli/target/phlox-cli-full.jar` (shaded), **not** `phlox-cli.jar`.
- CI is `.github/workflows/maven.yml` with Java 17, 21 and 25. No checkstyle, no formatter config — style comes from `~/.claude/rules/` (Hungarian notation, spaces before parens, Apache licence header, ph-commons collections, etc.).
- `ph-css` is imported in `pom.xml` before `ph-oton`, which brings an older one.
- When a dependency is a `-SNAPSHOT` version (e.g. `ph-pdf-layout` during development), install it from the sibling folder first, built with JDK 21 or older. Classes built by javac 25 cannot be read by javac 21 (`RuntimeInvisibleParameterAnnotations`, invalid class file), and a stale `target` keeps them, so use `clean install`.

## Tests

- Test fixtures live in `phlox-testfiles/src/main/resources/external/en16931/{2017,2026}/{ubl,cii}/...`, invented edge cases in `.../en16931/synthetic/`.
- Adding regression coverage usually means adding an XML fixture there, not a new Java test class.
- `PdfAllDataShownTest` (phlox-pdf) checks every fixture under `external/en16931`: all values of the XML must be in the text of the PDF (normalised: amounts, dates, codes by their label, also the number of occurrences), and each XPath is proven once with a changed value. Its reports are `target/all-data-shown.tsv` and `target/all-data-probed.tsv`. `phlox-repeated-*.xml` in `synthetic/` hold each 0..1 element twice.
- JUnit 4.
- Reference files: `HtmlMastersTest` (phlox-html), `PdfMastersTest` (phlox-pdf) and `TextMastersTest` (phlox-text) compare the output of `MockPhloxTestFiles.getMasterFiles ()` with `src/test/resources/masters`. After an intended change of the output write them again with `-Dphlox.updateMasters=true` and review the `git diff`. The PDF master is a text description of the layout plus a PDF that is compared with pdfcompare. The PDF masters of the ZUGFeRD samples (`.../2017/cii/zugferd/`) are real Factur-X invoices (PDF/A-3b, Roboto, the XML as `factur-x.xml`), checked with kaltblut, so other tools can use them. The update strips the licence header of the HTML masters and rewrites every PDF, so restore the headers and the PDFs whose layout text did not change.

## Localisation

- Labels in `phlox-api/src/main/resources/phlox-labels_{en,de,es,fr,nl}.properties`.
- Names of code list values (VAT categories, payment means, countries, units, ...) are not kept here: `DefaultPhloxLabelResolver` asks the `en16931-basics` enums (`getDisplayText (Locale)`) and `UnitCodes` of `ph-codelists-unit`, which fall back to English. Add translations there, not in phlox.
- Default resolver: `DefaultPhloxLabelResolver`. Callers override via `RenderContext.setLabelResolver(...)`.
- Adding a language = adding a `phlox-labels_<lang>.properties` file; no code changes.

## Other

- `PhloxProgramWatermark.setInstance(...)` is a singleton set once at JVM startup (used for attribution in OSS vs. commercial builds) — not a per-render option.
- Shade plugin in `phlox-cli` uses `ServicesResourceTransformer` to merge SPI registries; preserve that when touching the CLI build.
