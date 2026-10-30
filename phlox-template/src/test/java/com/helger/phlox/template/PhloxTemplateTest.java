/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phlox.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.i18n.IPhloxLabelResolver;
import com.helger.phlox.api.model.PhloxEarlyPaymentDiscount;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxItem;
import com.helger.phlox.api.model.PhloxLatePaymentPenalty;
import com.helger.phlox.api.model.PhloxLineDelivery;
import com.helger.phlox.api.model.PhloxLineVat;
import com.helger.phlox.api.model.PhloxPaymentTerms;
import com.helger.phlox.api.model.PhloxPriceDetails;
import com.helger.phlox.api.model.PhloxVatBreakdown;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxTemplateAst;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Smoke test: compile {@code default.ptpl}, render a minimal invoice through the full substitute →
 * parse → post-pass pipeline, and assert the result is structurally sane.
 */
public final class PhloxTemplateTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PhloxTemplateTest.class);

  /** Concrete subclass purely so the abstract base can be instantiated in this test. */
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  /** Spy resolver that counts label lookups. */
  private static final class CountingResolver implements IPhloxLabelResolver
  {
    private final AtomicInteger m_aLabelCount = new AtomicInteger ();
    private final AtomicInteger m_aCodeCount = new AtomicInteger ();

    public String getLabel (final String sKey, final Locale aLocale)
    {
      m_aLabelCount.incrementAndGet ();
      return "L(" + sKey + ")";
    }

    public String getCodeLabel (final String sListName, final String sCode, final Locale aLocale)
    {
      m_aCodeCount.incrementAndGet ();
      return sCode;
    }

    int count ()
    {
      return m_aLabelCount.get ();
    }
  }

  @NonNull
  private static PhloxInvoice _buildMinimalInvoice ()
  {
    final PhloxInvoice aInv = new PhloxInvoice ();
    aInv.setNumber ("INV-001");
    aInv.setIssueDate (LocalDate.of (2026, 3, 15));
    aInv.setTypeCode ("380");
    aInv.setCurrency ("EUR");

    aInv.getSeller ().setName ("ACME Sellers Inc.");
    aInv.getBuyer ().setName ("Smith Co.");

    aInv.getDocumentTotals ().setLineNetTotal (new BigDecimal ("100.00"));
    aInv.getDocumentTotals ().setTaxExclusiveTotal (new BigDecimal ("100.00"));
    aInv.getDocumentTotals ().setTaxTotal (new BigDecimal ("19.00"));
    aInv.getDocumentTotals ().setTaxInclusiveTotal (new BigDecimal ("119.00"));
    aInv.getDocumentTotals ().setPayableAmount (new BigDecimal ("119.00"));

    final PhloxVatBreakdown aVat = new PhloxVatBreakdown ();
    aVat.setCategoryCode ("S");
    aVat.setRate (new BigDecimal ("19"));
    aVat.setTaxableAmount (new BigDecimal ("100.00"));
    aVat.setTaxAmount (new BigDecimal ("19.00"));
    aInv.vatBreakdowns ().add (aVat);

    final PhloxInvoiceLine aLine = new PhloxInvoiceLine ();
    aLine.setID ("1");
    aLine.setQuantity (new BigDecimal ("2"));
    aLine.setQuantityUnit ("EA");
    aLine.setNetAmount (new BigDecimal ("100.00"));
    final PhloxItem aItem = new PhloxItem ();
    aItem.setName ("Widget");
    aLine.setItem (aItem);
    final PhloxPriceDetails aPrice = new PhloxPriceDetails ();
    aPrice.setNetPrice (new BigDecimal ("50.00"));
    aLine.setPrice (aPrice);
    final PhloxLineVat aLineVat = new PhloxLineVat ();
    aLineVat.setCategoryCode ("S");
    aLineVat.setRate (new BigDecimal ("19"));
    aLine.setLineVat (aLineVat);
    aInv.lines ().add (aLine);

    return aInv;
  }

  private static void _renderFixtures (final IInvoiceReader <InputStream> aReader,
                                       final Iterable <ClassPathResource> aFiles,
                                       final int nMinCount)
  {
    int nCount = 0;
    for (final ClassPathResource aRes : aFiles)
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PhloxInvoice aInvoice = aReader.read (aIS, new ErrorList ());
        assertNotNull ("Failed to parse " + aRes.getPath (), aInvoice);
        for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
        {
          final TestCtx aCtx = new TestCtx ();
          aCtx.setLocale (aLocale);
          assertNotNull ("No AST for " + aRes.getPath (), PhloxTemplate.getDefault ().renderAst (aInvoice, aCtx));
        }
        nCount++;
      }
      catch (final IOException ex)
      {
        throw new IllegalStateException ("Failed to open " + aRes.getPath (), ex);
      }
    }
    assertTrue ("Expected at least " + nMinCount + " documents rendered", nCount >= nMinCount);
  }

  private static Heading _findHeading (final Node aParent)
  {
    for (Node aChild = aParent.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      if (aChild instanceof final Heading aHeading)
        return aHeading;
      final Heading aNested = _findHeading (aChild);
      if (aNested != null)
        return aNested;
    }
    return null;
  }

  @Test
  public void testRenderAstAgainstDefaultTemplate ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxTemplate aTpl = PhloxTemplate.getDefault ();
    final PhloxTemplateAst aAst = aTpl.renderAst (aInvoice, aCtx);

    assertNotNull (aAst);
    assertNotNull (aAst.getRoot ());

    // The default template opens with a head row, so the first block is a row group
    // that contains the level-1 Heading with the document number
    assertTrue (aAst.getRoot ().getFirstChild () instanceof PhloxGroupNode);
    assertTrue (((PhloxGroupNode) aAst.getRoot ().getFirstChild ()).isRow ());
    final Heading aFirstHeading = _findHeading (aAst.getRoot ());
    assertNotNull ("expected at least one Heading in the rendered AST", aFirstHeading);
    assertEquals (1, aFirstHeading.getLevel ());
  }

  @Test
  public void testRenderEdition2026ModelContent ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    aInvoice.buyerReferences ().add (new PhloxIdentifier ("BR-1", "ABE"));
    aInvoice.buyerReferences ().add (new PhloxIdentifier ("BR-2", null));
    aInvoice.setIssueTime (OffsetTime.of (12, 5, 3, 0, ZoneOffset.ofHours (1)));
    aInvoice.setDeliveryNoteReference ("DN-77");
    final PhloxPaymentTerms aTerms = new PhloxPaymentTerms ().setText ("Net 30 days");
    aTerms.earlyDiscounts ()
          .add (new PhloxEarlyPaymentDiscount ().setEndDate (LocalDate.of (2026, 3, 25))
                                                .setPercentage (new BigDecimal ("2")));
    aTerms.latePenalties ()
          .add (new PhloxLatePaymentPenalty ().setStartDate (LocalDate.of (2026, 5, 1))
                                              .setAmount (new BigDecimal ("12.50")));
    aInvoice.paymentTerms ().add (aTerms);
    aInvoice.lines ()
            .get (0)
            .setDelivery (new PhloxLineDelivery ().setPartyName ("Line Depot")
                                                  .setLocationIdentifier (new PhloxIdentifier ("4035813333333",
                                                                                               "0088")));

    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);
    final String sCommonMark = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, aCtx);

    assertTrue (sCommonMark.contains ("BR-1 (ABE), BR-2"));
    assertTrue (sCommonMark.contains ("12:05:03+01:00"));
    assertTrue (sCommonMark.contains ("DN-77"));
    assertTrue (sCommonMark.contains ("Net 30 days"));
    assertTrue (sCommonMark.contains ("12.50") || sCommonMark.contains ("12,50"));
    assertTrue (sCommonMark.contains ("Line Depot"));
    assertTrue (sCommonMark.contains ("0088:4035813333333"));
    assertNotNull (PhloxTemplate.getDefault ().renderAst (aInvoice, aCtx));
  }

  @Test
  public void testRenderUbl25FixturesAgainstDefaultTemplate ()
  {
    _renderFixtures (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getAllUbl25Files (), 20);
  }

  @Test
  public void testRenderCiiD25AFixturesAgainstDefaultTemplate ()
  {
    _renderFixtures (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles (), 10);
  }

  @Test
  public void testSubstitutionContainsInvoiceNumberAndSellerName ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxTemplate aTpl = PhloxTemplate.getDefault ();
    final String sCommonMark = aTpl.renderSubstituted (aInvoice, aCtx);
    LOGGER.debug ("Substituted CommonMark (" + sCommonMark.length () + " chars)");

    assertTrue ("expected INV-001 in output", sCommonMark.contains ("INV-001"));
    assertTrue ("expected seller name in output", sCommonMark.contains ("ACME Sellers Inc."));
    assertTrue ("expected buyer name in output", sCommonMark.contains ("Smith Co."));
    // The synthesized boolean `logo` is false → the attachment:logo line must be absent.
    assertTrue ("expected no attachment:logo when no logo set", !sCommonMark.contains ("attachment:logo"));
  }

  @Test
  public void testFromStringSimpleTemplateRoundTrips ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxTemplate aTpl = PhloxTemplate.fromString ("# {{BT-1}}\n\n**Seller:** {{BG-4.BT-27}}\n");
    final String sOut = aTpl.renderSubstituted (aInvoice, aCtx);
    assertTrue ("expected '# INV-001' in: " + sOut, sOut.contains ("# INV-001"));
    assertTrue ("expected seller line in: " + sOut, sOut.contains ("**Seller:** ACME Sellers Inc."));
  }

  @Test
  public void testInvoiceDataIsMarkdownEscaped ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    aInvoice.setNumber ("1. a | *b*\n- ![x](http://evil/x.png)");
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxTemplate aTpl = PhloxTemplate.fromString ("{{BT-1}}");
    assertEquals ("1\\. a \\| \\*b\\*<br>\\- !\\[x\\](http://evil/x.png)",
                  aTpl.renderSubstituted (aInvoice, aCtx));
  }

  @Test
  public void testPresenceCheckSkipsEmptyOptional ()
  {
    // BT-10 (buyer reference) is NOT set on the minimal invoice.
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxTemplate aTpl = PhloxTemplate.fromString ("Before{{?BT-10}}HIT{{/BT-10}}After");
    assertEquals ("BeforeAfter", aTpl.renderSubstituted (aInvoice, aCtx));

    final PhloxTemplate aTpl2 = PhloxTemplate.fromString ("Before{{?BT-1}}HIT{{/BT-1}}After");
    assertEquals ("BeforeHITAfter", aTpl2.renderSubstituted (aInvoice, aCtx));
  }

  @Test
  public void testAmountModifierUsesInvoiceCurrency ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.US);

    final PhloxTemplate aTpl = PhloxTemplate.fromString ("Due: {{BG-22.BT-115 | amount}}");
    final String sOut = aTpl.renderSubstituted (aInvoice, aCtx);
    // EUR + US locale → "€119.00" or "EUR119.00" depending on JVM tables. Either prefix
    // is acceptable; assert just that the numeric portion appears.
    assertTrue ("expected '119.00' in: " + sOut, sOut.contains ("119.00"));
  }

  @Test
  public void testLocalizeCachesAcrossRenders ()
  {
    // A counting resolver records every label lookup. With caching working, all label lookups
    // happen exactly once per locale at the first localize(...) call; subsequent renders in
    // the same locale must not touch the resolver.
    final CountingResolver aSpy = new CountingResolver ();
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);
    aCtx.setLabelResolver (aSpy);

    final PhloxTemplate aTpl = PhloxTemplate.fromString ("# {{label:section.header.title.invoice}}: {{BT-1}}");
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();

    aTpl.renderSubstituted (aInvoice, aCtx);
    final int nAfterFirst = aSpy.count ();
    assertTrue ("expected at least one label lookup on first render", nAfterFirst >= 1);

    aTpl.renderSubstituted (aInvoice, aCtx);
    aTpl.renderSubstituted (aInvoice, aCtx);
    assertEquals ("label resolver must not be invoked on cache hit", nAfterFirst, aSpy.count ());
  }

  @Test
  public void testLocalizeIsSameInstanceForSameKey ()
  {
    final PhloxTemplate aTpl = PhloxTemplate.fromString ("# {{label:section.header.title.invoice}}: {{BT-1}}");
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);

    final PhloxLocalizedTemplate aFirst = aTpl.localize (aCtx);
    final PhloxLocalizedTemplate aSecond = aTpl.localize (aCtx);
    assertSame ("cache should return identical PhloxLocalizedTemplate for same key", aFirst, aSecond);
  }

  @Test
  public void testLocalizeBakedSourceHasLabelsResolved ()
  {
    // Labels are pre-substituted in stage 0 — the locale-baked source contains no {{label:...}}.
    final PhloxTemplate aTpl = PhloxTemplate.fromString ("# {{label:section.header.title.invoice}}: {{BT-1}}");
    final TestCtx aCtx = new TestCtx ();
    aCtx.setLocale (Locale.ENGLISH);
    final PhloxLocalizedTemplate aLocalized = aTpl.localize (aCtx);
    assertTrue ("locale-baked source must not contain any {{label:...}} markers",
                !aLocalized.getLocaleBakedSource ().contains ("{{label:"));
  }

  @Test
  public void testDefaultTemplateShowsAllBTs ()
  {
    assertEquals ("[]", PhloxTemplate.getDefault ().getMissingBTs ().toString ());
    // Passes in the strict mode as well
    PhloxTemplate.getDefault ().checkCoverage (EPhloxTemplateCoverageMode.REJECT);
  }

  @Test
  public void testCoverageWarnKeepsTemplate ()
  {
    final PhloxTemplate aTemplate = PhloxTemplate.fromString ("# {{BT-1}}");
    assertTrue (aTemplate.getMissingBTs ().contains ("BT-2"));
    assertFalse (aTemplate.getMissingBTs ().contains ("BT-1"));
  }

  @Test
  public void testCoverageReject ()
  {
    final PhloxTemplateException ex = assertThrows (PhloxTemplateException.class,
                                                    () -> PhloxTemplate.fromString ("# {{BT-1}}",
                                                                                    EPhloxTemplateCoverageMode.REJECT));
    assertEquals (PhloxTemplateException.EStage.LOAD, ex.getStage ());
    assertTrue (ex.getMessage (), ex.getMessage ().contains ("BT-2,"));

    // A complete template is accepted, also from the classpath
    assertNotNull (PhloxTemplate.fromClasspath (PhloxTemplate.DEFAULT_CLASSPATH_PATH,
                                                EPhloxTemplateCoverageMode.REJECT));
  }

  @Test
  public void testLineExtrasShowLineOnlyTerms ()
  {
    final PhloxInvoice aInvoice = _buildMinimalInvoice ();
    final PhloxInvoiceLine aLine = aInvoice.lines ().get (0);
    aLine.setObjectIdentifier (new PhloxIdentifier ("OBJ-1", "AWV"));
    aLine.setPurchaseOrderLineRef ("POL-7");
    aLine.setAccountingCost ("COST-3");

    final String sOut = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sOut, sOut.contains ("AWV:OBJ-1"));
    assertTrue (sOut, sOut.contains ("POL-7"));
    assertTrue (sOut, sOut.contains ("COST-3"));
  }
}
