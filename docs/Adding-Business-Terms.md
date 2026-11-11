# Adding Business Terms

This page lists what it takes to add a new Business Term (BT) or Business Group (BG) of EN 16931 to phlox, for
example when a new edition of the standard or a correction of the syntax binding adds one. It follows the
pipeline: model, readers, template, labels, tests, documentation.

The worked example throughout is **BT-197 Delivery note reference** (0..1, header level, EN 16931:2026), which
touches every step.

## Overview

| # | Step | Module | Main files |
|---|------|--------|------------|
| 1 | Decide where the term lives and its Java type | - | EN 16931 syntax mapping |
| 2 | Extend the intermediate model | `phlox-api` | `com.helger.phlox.api.model.*` |
| 3 | Map the syntax elements in the readers | `phlox-ubl`, `phlox-cii` | `Ubl21InvoiceReader`, `Ubl25InvoiceReader`, `CiiD16BInvoiceReader`, `CiiD25AInvoiceReader` |
| 4 | Register the term for templates | `phlox-template` | `PhloxModelRegistry`, maybe `PhloxInvoiceMustacheContext`, `PhloxTemplateCoverage` |
| 5 | Show the term in the default template | `phlox-template` | `templates/default.ptpl` |
| 6 | Add the labels | `phlox-api` | `phlox-labels_{en,de,es,fr,nl}.properties` |
| 7 | Add test data and tests | `phlox-testfiles`, all | fixtures, reader tests, masters |
| 8 | Update the documentation | wiki | `Domain-Model`, `News-and-noteworthy`, ... |

The writers (`phlox-html`, `phlox-pdf`, `phlox-text`) normally need **no change**: the emitters walk the template
AST and have no logic per field.

## 1. Analyse the term

Look the term up in the syntax mapping of the edition (`../en16931-cii2ubl/docs/en16931-2017-syntax.md` and
`en16931-2026-syntax.md`) and decide:

| Question | Consequence |
|----------|-------------|
| Which BG does it belong to? | The model class that gets the field (see the table in step 2). A new BG needs a new class. |
| Which semantic data type? | The Java type of the field, see below. |
| Cardinality in EN 16931? | 0..1 / 1..1: a single field. 0..n / 1..n: an `ICommonsList`. |
| Cardinality in the syntax? | If the XSD allows the element more than once but EN 16931 only once, every further occurrence becomes a `PhloxAdditionalTerm` (step 3). |
| Which editions? | A term only in 2026 stays empty for 2017 documents. Check each XSD separately: CII D16B already has `ram:DeliveryNoteReferencedDocument`, so `CiiD16BInvoiceReader` reads BT-197 too, while UBL 2.1 has no element for it. |
| Is it a code? | Which code list names its values (`code:<list>` modifier, step 5). |
| Does a rule forbid it? | Content that a rule of `phive-rules-en16931` (`UBL-CR`, `CII-SR`) says should not be present is out of scope. |

Semantic data type to Java type (see the wiki page `Domain-Model`, section "Semantic data types"):

| EN 16931 type | Java type |
|---------------|-----------|
| Text, Document Reference, Code | `String` |
| Identifier | `String`, or `PhloxIdentifier` when it has a scheme identifier |
| Date | `LocalDate` |
| Time | `OffsetTime` (BT-166) |
| Amount, Unit Price Amount, Percentage, Quantity | `BigDecimal` |
| Binary Object | `PhloxAttachment` |

## 2. Extend the intermediate model (`phlox-api`)

The model is the contract between readers and writers. One class per BG, one field per BT.

| BG | Class |
|----|-------|
| Document level (BT-1..19, BT-166, BT-167, BT-197, ...) | `PhloxInvoice` |
| BG-4 / BG-7 | `PhloxSeller` / `PhloxBuyer` |
| BG-5, BG-8, BG-12, BG-15, BG-38 | `PhloxPostalAddress` (shared) |
| BG-6, BG-9 | `PhloxContact` (shared) |
| BG-25 | `PhloxInvoiceLine` |
| BG-31 | `PhloxItem` |
| ... | one `Phlox*` class per BG in `com.helger.phlox.api.model` |

For a single-valued term (example `PhloxInvoice`):

```java
private String m_sDeliveryNoteReference;

/**
 * Get the delivery note reference (BT-197).
 *
 * @return Delivery note reference (BT-197). May be {@code null}.
 */
@Nullable
public String getDeliveryNoteReference ()
{
  return m_sDeliveryNoteReference;
}

/**
 * Set the delivery note reference (BT-197).
 *
 * @param sDeliveryNoteReference
 *        Delivery note reference; may be {@code null}.
 * @return this for chaining.
 */
@NonNull
public PhloxInvoice setDeliveryNoteReference (@Nullable final String sDeliveryNoteReference)
{
  m_sDeliveryNoteReference = sDeliveryNoteReference;
  return this;
}
```

Rules:

- An absent single term is `null`. A repeated term is a `final ICommonsList <...>` initialised to an empty
  `CommonsArrayList`, exposed through an accessor without `get` prefix and with `@ReturnsMutableObject` (for
  example `buyerReferences ()`, `paymentTerms ()`). Collections are never `null`.
- An optional single BG (BG-10, 11, 13, 14, 16; line BG-26, 29, 30, 31) is `null` when absent; mandatory ones are
  created in the constructor.
- A new BG gets a new class `Phlox<Name>` in `com.helger.phlox.api.model` with the Apache licence header, Javadoc,
  fluent setters returning `this`, and, if it can hold values without a field, an `additionalTerms ()` list. Add a
  unit test next to the existing ones in `phlox-api/src/test/java/com/helger/phlox/api/model/`.
- Name the term in the Javadoc of the getter and setter, and add it to the class Javadoc where the class lists its
  BTs (`PhloxInvoice` lists its header-level BTs).

## 3. Map the syntax elements (`phlox-ubl`, `phlox-cii`)

There is one reader per syntax version. The readers share no JAXB types, so each one maps the element on its own:

| Reader | Syntax | Edition |
|--------|--------|---------|
| `Ubl21InvoiceReader` | UBL 2.1 | EN 16931:2017 |
| `Ubl25InvoiceReader` | UBL 2.5 | EN 16931:2026 |
| `CiiD16BInvoiceReader` | CII D16B | EN 16931:2017 |
| `CiiD25AInvoiceReader` | CII D25A | EN 16931:2026 |

Add the mapping to every reader whose XSD has the element. UBL invoice and credit note are read by separate
methods, so check both. Mark the line with a comment naming the BT, as the surrounding code does:

```java
// UBL 2.5
// BT-197 — delivery note reference
_readDocumentReferences (aDeliveryNote, "BT-197", aModel::setDeliveryNoteReference, aModel);

// CII D25A
// BT-197 — delivery note (EN 16931:2026)
if (aHTD.getDeliveryNoteReferencedDocument () != null)
  aModel.setDeliveryNoteReference (_idValue (aHTD.getDeliveryNoteReferencedDocument ().getIssuerAssignedID ()));
```

Nothing of the XML may be lost (see "Hard constraints" in `CLAUDE.md`):

- If the syntax allows the element more than once but the model only once, use
  `PhloxReaderHelper.setAndAddRepeated (aValues, "BT-x", aSetter, aAdditionalTerms)`: the first value goes into the
  field, every other one into the `additionalTerms ()` of the invoice, seller, buyer, payment instructions or line.
  The term key (`"BT-x"`) is the label key under which the template shows the value.
- For a whole repeated group there are helpers in `PhloxReaderHelper` (`addDelivery`, `addLineDelivery`,
  `addContact`, `addPaymentCard`, `addDirectDebit`, `addPeriod`). A new group with the same problem gets a helper
  there, so that all readers behave the same.
- A syntax element without a business term of its own is stored as additional term with an `additional.*` label
  key (constants `TERM_*` in `AbstractUblInvoiceReader` / `AbstractCiiInvoiceReader`).
- Use the existing helpers of the reader for the value (`str`, `strWithoutFiller` for UBL fillers `None` / `NA`,
  `parseFormattedDate`, `_idValue`, ...). Problems that can be recovered from are warnings in `aErrors`, with the
  value passed through `PhloxLogHelper.sanitise`.
- No validation: do not check the value against business rules.

## 4. Register the term for templates (`phlox-template`)

### `PhloxModelRegistry`

Templates reach the model only through this registry. Add one line in the static block, next to the terms of the
same class:

```java
_add (PhloxInvoice.class, "BT-197", PhloxInvoice::getDeliveryNoteReference);
```

- A new BG is registered against its parent (`_add (PhloxInvoice.class, "BG-x", PhloxInvoice::getFoo)`), and its
  BTs against its own class.
- A class that is used by several BGs registers the BT aliases of every position (see `PhloxPostalAddress`:
  BT-35..40, BT-50..55, BT-64..69, BT-75..80, BT-203..209).
- A list of identifiers that the template shows as one string gets a helper such as `_joinIdentifiers`.
- If the new class has additional terms, register them under `additional_terms`.
- Every name that matches `BT-\d+(-\d+)?` counts as a real BT for the coverage check (`getAllBTs ()`); a derived
  name such as `BT-149-if-not-one` does not.
- `isEmptyRecursive` uses the registry, so a registered term automatically counts for "is this group empty".

### `PhloxInvoiceMustacheContext`

Only needed if the term belongs to a section that is shown under a synthesized condition. These booleans list
their terms explicitly and must be extended:

| Key | Method | Covers |
|-----|--------|--------|
| `references_present` | `_referencesPresent` | BT-11..19, BT-197, BG-3, additional terms of the invoice |
| `line_extras_present` | `_lineExtrasPresent` | the extra rows of the invoice lines (notes, line references, BG-26, BG-37, BG-39, BT-196, ...) |

BT-197 had to be added to `_referencesPresent`, otherwise an invoice with only a delivery note reference would
not show the references section.

### `PhloxTemplateCoverage`

Only needed for a new address BG or a new address part: the derived name `address` prints all parts of an
address except the country, and `ADDRESS_BTS` lists which BTs that covers per BG.

## 5. Show the term in the default template

`phlox-template/src/main/resources/templates/default.ptpl` must show every registered BT. `PhloxTemplateTest` and
`PhloxInvoiceMustacheContextTest` check this with `PhloxTemplate.getDefault ().getMissingBTs ()` and
`checkCoverage (EPhloxTemplateCoverageMode.REJECT)`; a BT that is registered but not printed fails the build.

1. Add the term to the comment block of its section, with cardinality and edition:
   ```
   BT-197 Delivery note reference              (0..1, EN 16931:2026)
   ```
2. Add the output, guarded by `{{?...}}` so that nothing is printed when it is absent, and with a label instead of
   literal text:
   ```
   {{?BT-197}}- **{{label:BT-197}}:** {{BT-197}}
   {{/BT-197}}
   ```
3. Pick the modifier that matches the type: `| date`, `| amount` (or `| amount:BT-6`), `| percent`,
   `| code:<list>`. A code is shown with its value and its name, for example `{{BT-3}} — {{BT-3 | code:doctype}}`.
4. A repeated term or group is a section `{{#BG-x}}...{{/BG-x}}`; inside it the names are relative.
5. If the class has additional terms, make sure its `additional_terms` loop is part of the template
   (`{{#additional_terms}}- **{{term | label}}:** {{value}}{{/additional_terms}}`).

Only a placeholder that prints the value counts as shown. `{{label:BT-x}}` and `{{?BT-x}}` do not count, nor
does anything in `{{header}}` / `{{footer}}` or behind a `| url` modifier (QR code, link).

A new section with its own `{{class:...}}`, `{{group:...}}` or `{{row:...}}` name may need a style in
`phlox-html/src/main/resources/phlox-default.tcss` and a PDF block style.

### Code lists

The `code:<list>` modifier resolves the name of a code value through `DefaultPhloxLabelResolver` (map
`CODE_LISTS`), which asks the enums of `en16931-basics` or `UnitCodes` of `ph-codelists-unit`. A new code list is
added there, and its translations belong into `en16931-basics`, not into phlox.

## 6. Add the labels (`phlox-api`)

Add the term to all five label files, in the section of its BG:

| File | BT-197 |
|------|--------|
| `phlox-labels_en.properties` | `Delivery note reference` |
| `phlox-labels_de.properties` | `Lieferscheinreferenz` |
| `phlox-labels_es.properties` | `Referencia del albarán` |
| `phlox-labels_fr.properties` | `Référence du bon de livraison` |
| `phlox-labels_nl.properties` | `Leveringsbonnummer` |

`DefaultPhloxLabelResolverBundlesTest` checks that all languages have the same BT/BG keys and that no label is
empty. Its list `NEW_2026_IDS` names the terms that EN 16931:2026 added and checks each in every language; add a
term of a new edition there (or start a similar list for a later edition). New `additional.*` keys and section
titles also go into all five files.

## 7. Test data and tests

| What | Where | Note |
|------|-------|------|
| Reader unit tests | `UblInvoiceReaderTest`, `CiiInvoiceReaderTest`, `CiiD25AInvoiceReaderTest` | assert the new getter, e.g. `assertEquals ("DELNOTE-8", aModel.getDeliveryNoteReference ())` |
| Reader edge cases | `UblInvoiceReaderSyntheticTest`, `CiiInvoiceReaderSyntheticTest` with `synthetic/*/phlox-reader-*.xml` | fillers, blanks, surrounding spaces |
| Repeated occurrences | `synthetic/*/phlox-repeated-*.xml` | every 0..1 element twice with unique values, so the second one must appear as additional term |
| Template rendering | `PhloxTemplateTest` (e.g. `testRenderEdition2026ModelContent`) | builds a model in Java and checks the output |
| Example invoices | `phlox-testfiles/src/main/resources/external/en16931/{2017,2026}/{ubl,cii}/` | a fixture that contains the element |
| All data shown | `PdfAllDataShownTest` (`phlox-pdf`) | every value of every fixture must appear in the PDF, and each XPath is probed once with a changed value |
| Reference files | `HtmlMastersTest`, `PdfMastersTest`, `TextMastersTest` | rewrite with `-Dphlox.updateMasters=true` and review the `git diff` |

Prefer adding the element to an XML fixture over writing a new Java test class.

`PdfAllDataShownTest` is the final proof that nothing was dropped. If it fails for the new element, a reader, the
registry or the template loses the value. Do not add the XPath to its `ALLOWED` list unless the value really is no
data of the invoice. If the new element is a code, add its element name to the test's own `CODE_LISTS`, so that the
value is also found by its name.

After rewriting the masters, restore the licence headers of the HTML masters and the PDFs whose layout text did not
change (see `CLAUDE.md`, section "Tests").

Run at least:

```bash
mvn -pl phlox-api,phlox-ubl,phlox-cii,phlox-template test
mvn -pl phlox-pdf test -Dtest=PdfAllDataShownTest
```

and finally a full `mvn clean install`.

## 8. Documentation

| Where | What |
|-------|------|
| `../phlox.wiki/Domain-Model.md` | the term in the tree of the "Top-level shape" and in the table of its BG (type, cardinality, field; mark 2026-only terms with *2026*) |
| `../phlox.wiki/Template-Processing.md` | when a synthesized key such as `references_present` covers the term |
| `../phlox.wiki/Source-Readers.md` | when the mapping is special (filler, changed XPath between editions, warning) |
| `../phlox.wiki/News-and-noteworthy.md` | a bullet in the entry of the upcoming version |
| `CLAUDE.md` | only if a statement there becomes wrong (e.g. a new BG with an optional `null` object) |

## Checklist

- [ ] Model field or list with Javadoc naming the BT (`phlox-api`)
- [ ] New BG: new model class plus unit test
- [ ] Mapping in every reader whose XSD has the element, invoice and credit note
- [ ] Repeated occurrences go to `additionalTerms ()`
- [ ] `PhloxModelRegistry` entry
- [ ] `_referencesPresent` / `_lineExtrasPresent` if the section is guarded by them
- [ ] `ADDRESS_BTS` in `PhloxTemplateCoverage` for an address term
- [ ] Comment block and output in `default.ptpl`
- [ ] Label in all five `phlox-labels_*.properties` (and `NEW_2026_IDS` of `DefaultPhloxLabelResolverBundlesTest` for a 2026 term)
- [ ] Code list in `DefaultPhloxLabelResolver` (and `PdfAllDataShownTest`) for a new code
- [ ] Reader tests and fixtures, including `phlox-repeated-*.xml`
- [ ] `PdfAllDataShownTest` green without new `ALLOWED` entries
- [ ] Masters rewritten and reviewed
- [ ] Wiki: `Domain-Model`, `News-and-noteworthy`
