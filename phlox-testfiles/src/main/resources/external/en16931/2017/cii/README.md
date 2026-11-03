# EN 16931:2017 CII D16B test corpus

This directory holds the curated set of CII D16B `CrossIndustryInvoice` instances used by `phlox-cii` reader tests and by HTML/PDF writer regression tests.

## Layout

```
2017/cii/
├── README.md                          ← this file
├── CII_*.xml                          ← core hand-curated examples (15 files; in repo from inception)
├── XRechnung-O.xml                    ← XRechnung "type O" sample
├── xrechnung/
│   ├── 1.2.2/                         ← XRechnung 1.2.2 corpus (21 files)
│   ├── 2.0.0/                         ← XRechnung 2.0.0 corpus (19 files)
│   ├── 3.0.2/                         ← XRechnung 3.0.2 corpus (33 files)
│   └── technical/                     ← comprehensive + minimal stress tests (6 files)
├── zugferd/
│   └── 2.5.2/en16931/                 ← ZUGFeRD 2.5.2 samples of the profile EN 16931 (3 files)
└── issues/                            ← bug-regression fixtures (10 files)
```

Total: 107 files.

## Provenance

Everything under `xrechnung/` and `issues/` was imported from `en16931-cii2ubl` at `../en16931-cii2ubl/en16931-cii2ubl/src/test/resources/external/cii/`, preserving the original subdirectory structure and filenames. The license of that project is Apache 2.0 (matching this project), and the files are EN 16931-conformant CII D16B `CrossIndustryInvoice` instances — they happen to be packaged as XRechnung tests because XRechnung is a EN 16931 binding profile, but the CII semantics they exercise are EN 16931 itself.

The files under `zugferd/` were imported from `phive-rules-zugferd` at `../phive-rules/phive-rules-zugferd/src/test/resources/external/test-files/2.5.2/en16931/`, keeping their filenames. They are samples of the ZUGFeRD package of FeRD (https://www.ferd-net.de/standards/paket-downloads/index.html); the usage rights of FeRD are in the comment at the top of each file.

The `CII_*.xml` files and `XRechnung-O.xml` in this directory's root were already in `phlox-testfiles` from inception and originated from various publicly available EN 16931 example sets.

## Filename conventions

A few `issues/*-bad.xml` filenames (e.g. `issue38-bad.xml`) keep the upstream naming from `en16931-cii2ubl`. The "-bad" suffix there flags files that expose a CII→UBL **conversion** bug in that project — the underlying CII XML is still well-formed and parses cleanly through `CiiInvoiceReader`.

## Java lookups

Test code retrieves typed lists via `MockPhloxTestFiles`:

| Method | Files |
|---|---|
| `getCiiD16BCoreFiles()` | 15 (core) |
| `getCiiD16BXRechnung122Files()` | 21 (XRechnung 1.2.2) |
| `getCiiD16BXRechnung200Files()` | 19 (XRechnung 2.0.0) |
| `getCiiD16BXRechnung302Files()` | 33 (XRechnung 3.0.2) |
| `getCiiD16BTechnicalFiles()` | 6 (stress tests) |
| `getCiiD16BIssueFiles()` | 10 (issue regressions) |
| `getCiiD16BZugferdFiles()` | 3 (ZUGFeRD 2.5.2) |
| `getCiiD16BFiles()` | 107 (everything) |
