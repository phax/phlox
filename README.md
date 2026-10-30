# phlox

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.phlox/phlox-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.phlox/phlox-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.phlox/phlox-api/javadoc.svg)](https://javadoc.io/doc/com.helger.phlox/phlox-api)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

![Logo](https://github.com/phax/phlox/blob/main/docs/logo/phlox-logo-644-180.png)

Visualizer for EN 16931 electronic invoices.

Reads invoices in **UBL** or **CII** syntax, lifts them into a syntax-neutral intermediate model, and renders the result to **HTML**, **PDF** (optionally as **PDF/A**, **PDF/UA** or **ZUGFeRD / Factur-X** hybrid invoice) or **plain text** in Dutch, English, French, German or Spanish.

```
┌────────────────┐     ┌──────────────────┐     ┌────────────────┐
│ UBL 2.1 / 2.5  │ ──▶ │                  │ ──▶ │ HTML output    │
│ Invoice and    │     │   PhloxInvoice   │     │                │
│ CreditNote     │     │  (EN 16931 BGs)  │ ──▶ │ PDF output     │
│ CII D16B / D25A│ ──▶ │                  │     │                │
│                │     │                  │ ──▶ │ PDF/A output   │
│                │     │                  │     │ (+ PDF/UA,     │
│                │     │                  │     │ ZUGFeRD)       │
│                │     │                  │     │                │
│                │     │                  │ ──▶ │ Plain text     │
└────────────────┘     └──────────────────┘     └────────────────┘
  Source readers        Intermediate model            Writers
```

## Features

- **Both editions of the standard:** EN 16931:2017 (UBL 2.1, CII D16B) and EN 16931:2026 (UBL 2.5, CII D25A). The edition is detected from BT-24 or chosen explicitly.
- **Complete model:** every EN 16931 business group and business term is represented; terms that only exist in 2026 stay empty for 2017 documents.
- **Template driven output:** HTML, PDF and plain text are rendered from a `.ptpl` template (JMustache plus CommonMark). The default template ships with phlox; your own replaces it without writing Java.
- **Themes:** colours, fonts, page layout, block styles, language, logo and watermark come from one JSON file. See [Styling](https://github.com/phax/phlox/wiki/Styling).
- **Any script in PDF:** embed TTF or OTF fonts (for example Roboto for Cyrillic and Greek) in HTML and PDF with `--font`, the theme or the API. The standard PDF fonts only contain Western European characters.
- **Watermarks:** a user watermark such as `DRAFT`, plus the program watermark.
- **PDF/A, PDF/UA and ZUGFeRD:** archivable PDF/A-2b, -2u, -3a (tagged), -3b and -3u, accessible PDF/UA-1, optionally with the source XML and the attached documents embedded, and ZUGFeRD / Factur-X hybrid invoices from CII invoices of EN 16931:2017. The output is checked with veraPDF in the tests. See [PDF/A and ZUGFeRD](https://github.com/phax/phlox/wiki/PDF-A).
- **Command line:** `phlox-cli` renders an invoice file to HTML, PDF (also PDF/A, PDF/UA and ZUGFeRD) or plain text.
- **No validation:** phlox only visualizes. Use `phive-rules-en16931` to check business rules.

## Modules

| Module | Purpose |
|--------|---------|
| `phlox-api` | Intermediate model, SPIs and localization |
| `phlox-ubl` | UBL 2.1 and 2.5 reader |
| `phlox-cii` | CII D16B and D25A reader |
| `phlox-template` | Template engine and the default template |
| `phlox-theme` | JSON theme reader |
| `phlox-html` | HTML5 writer |
| `phlox-pdf` | PDF writer, including PDF/A, PDF/UA and ZUGFeRD / Factur-X |
| `phlox-text` | Plain-text writer |
| `phlox-cli` | Command line tool |
| `phlox-testfiles` | Shared example invoices for tests |

Readers and writers depend only on `phlox-api` (and `phlox-template`), never on each other, so you can depend on, for example, the HTML writer without pulling in the PDF libraries.

## Build

```bash
mvn clean package
```

Requires Java 17+ and Maven 3.

The command line tool is `phlox-cli/target/phlox-cli-full.jar` (the shaded jar):

```bash
java -jar phlox-cli/target/phlox-cli-full.jar -i invoice.xml -f PDF -o invoice.pdf
```

A ZUGFeRD / Factur-X hybrid invoice from a CII invoice, as tagged PDF/A-3a and PDF/UA-1 (PDF/A and PDF/UA need an embedded font):

```bash
java -jar phlox-cli/target/phlox-cli-full.jar -i invoice-cii.xml -f PDF -o invoice.pdf --zugferd --pdfa 3a --pdfua --font Roboto-Regular.ttf --font-bold Roboto-Bold.ttf
```

| Option | Meaning |
|--------|---------|
| `-i`, `--in` | Input invoice XML file (stdin if omitted) |
| `-o`, `--out` | Output file (stdout if omitted, HTML and TEXT only) |
| `-f`, `--format` | `HTML` (default), `PDF` or `TEXT` |
| `-s`, `--syntax` | `AUTO` (default), `UBL` or `CII` |
| `--en-version` | `auto` (default), `2017` or `2026` |
| `-l`, `--lang` | Output language, `en`, `de`, `es`, `fr` or `nl` |
| `-t`, `--template` | `default`, `classpath:<path>` or the path of a `.ptpl` file |
| `--template-coverage` | `WARN` (default) logs the BTs a template does not show, `REJECT` refuses such a template |
| `--theme` | JSON theme file |
| `--logo` | Logo image (PNG, JPEG or GIF) |
| `--font` | Regular font file (TTF or OTF) for HTML and PDF; also `--font-bold`, `--font-italic` and `--font-bold-italic` |
| `--watermark` | User watermark text; see also `--watermark-opacity` and `--watermark-rotation` |
| `--pdfa` | PDF/A level of the PDF: `2b`, `2u`, `3a` (tagged), `3b` or `3u`; needs `--font` (or fonts in the theme) |
| `--pdfua` | Create a PDF/UA-1 (accessible, tagged PDF); can be combined with `--pdfa`; needs `--font` (or fonts in the theme) |
| `--embed-source` | Embed the input XML into the PDF (PDF/A-3, `3b` unless `--pdfa` is given) |
| `--embed-attachments` | Embed the attached documents of the invoice (BG-24) into the PDF (PDF/A-3, `3b` unless `--pdfa` is given) |
| `--zugferd` | Create a ZUGFeRD / Factur-X hybrid invoice from a CII invoice of EN 16931:2017 (PDF/A-3); `--zugferd-profile` overrides the profile derived from BT-24 |

## Where to find things

The default template, its model and the stylesheet:

| What | Location |
|------|----------|
| Default template | [`default.ptpl`](phlox-template/src/main/resources/templates/default.ptpl) |
| Names usable in the template (BTs and BGs) | [`PhloxModelRegistry`](phlox-template/src/main/java/com/helger/phlox/template/adapter/PhloxModelRegistry.java) |
| Default HTML stylesheet (CSS template, class prefix `{p}`) | [`phlox-default.tcss`](phlox-html/src/main/resources/phlox-default.tcss) |

The reference files (golden masters) show the current output for each example invoice in English and German.
`HtmlMastersTest`, `PdfMastersTest` and `TextMastersTest` compare the output with them:

| Output | Reference files | Test |
|--------|-----------------|------|
| HTML | [`phlox-html/src/test/resources/masters`](phlox-html/src/test/resources/masters) | [`HtmlMastersTest`](phlox-html/src/test/java/com/helger/phlox/html/HtmlMastersTest.java) |
| PDF (layout description `.txt` plus `.pdf`) | [`phlox-pdf/src/test/resources/masters`](phlox-pdf/src/test/resources/masters) | [`PdfMastersTest`](phlox-pdf/src/test/java/com/helger/phlox/pdf/PdfMastersTest.java) |
| Plain text | [`phlox-text/src/test/resources/masters`](phlox-text/src/test/resources/masters) | [`TextMastersTest`](phlox-text/src/test/java/com/helger/phlox/text/TextMastersTest.java) |

The example invoices are in [`phlox-testfiles/src/main/resources/external/en16931`](phlox-testfiles/src/main/resources/external/en16931) (`2017`, `2026` and `synthetic`); the selection used for the reference files is in [`MockPhloxTestFiles`](phlox-testfiles/src/main/java/com/helger/phlox/testfiles/MockPhloxTestFiles.java).
After an intended change of the output, write the reference files again with `-Dphlox.updateMasters=true` and review the `git diff`:

```bash
mvn -pl phlox-html test -Dtest=HtmlMastersTest -Dphlox.updateMasters=true
mvn -pl phlox-pdf test -Dtest=PdfMastersTest -Dphlox.updateMasters=true
mvn -pl phlox-text test -Dtest=TextMastersTest -Dphlox.updateMasters=true
```

## Documentation

See the [wiki](https://github.com/phax/phlox/wiki/Home) for the full documentation, including the [domain model](https://github.com/phax/phlox/wiki/Domain-Model), the [template language](https://github.com/phax/phlox/wiki/Template-Language), [themes](https://github.com/phax/phlox/wiki/Themes), [PDF/A and ZUGFeRD](https://github.com/phax/phlox/wiki/PDF-A), the [maven module structure](https://github.com/phax/phlox/wiki/Maven-Module-Structure), the [roadmap](https://github.com/phax/phlox/wiki/Roadmap) and the [news and noteworthy](https://github.com/phax/phlox/wiki/News-and-noteworthy).

## License

Apache License 2.0, see [LICENSE](LICENSE). Third-party notes are in [NOTICE](NOTICE).

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.