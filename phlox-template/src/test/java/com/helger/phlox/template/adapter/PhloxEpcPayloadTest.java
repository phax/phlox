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
package com.helger.phlox.template.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.imageio.ImageIO;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.EPhloxDocumentKind;
import com.helger.phlox.api.model.PhloxCreditTransfer;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxPayee;
import com.helger.phlox.api.model.PhloxPaymentInstructions;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxTemplateAst;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test class for class {@link PhloxEpcPayload}. The expected values follow EPC069-12 v3.1.
 *
 * @author Philip Helger
 */
public final class PhloxEpcPayloadTest
{
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  private static final String IBAN = "FR1420041010050500013M02606";

  @NonNull
  private static PhloxCreditTransfer _account (@Nullable final String sCode, @Nullable final String sAccount)
  {
    return new PhloxCreditTransfer ().setPaymentMeansCode (sCode).setAccountIdentifier (sAccount);
  }

  @NonNull
  private static PhloxInvoice _invoice (@NonNull final PhloxCreditTransfer... aAccounts)
  {
    final PhloxPaymentInstructions aPayment = new PhloxPaymentInstructions ();
    for (final PhloxCreditTransfer aAccount : aAccounts)
      aPayment.creditTransfers ().add (aAccount);
    final PhloxInvoice ret = new PhloxInvoice ();
    ret.setCurrency ("EUR");
    ret.getSeller ().setName ("François D'Alsace S.A.");
    ret.getDocumentTotals ().setPayableAmount (new BigDecimal ("12.30"));
    ret.setPaymentInstructions (aPayment);
    return ret;
  }

  @NonNull
  private static String _element (@NonNull final PhloxInvoice aInvoice, final int nIndex)
  {
    return StringHelper.getExploded ('\n', PhloxEpcPayload.getPayload (aInvoice)).get (nIndex);
  }

  @NonNull
  private static PhloxInvoice _invoice ()
  {
    return _invoice (_account ("58", IBAN));
  }

  @Test (timeout = 10_000)
  public void testLongRunOfSpacesTakesLinearTime ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    aInvoice.getPaymentInstructions ().setRemittanceInformation ("a" + " ".repeat (300_000) + "b");
    assertEquals ("a" + " ".repeat (139), _element (aInvoice, 10));
  }

  @Test
  public void testLineBreaksInName ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    aInvoice.getSeller ().setName ("  a  b \r\n\t c\n\nd\f\re  ");
    assertEquals ("a  b c d e", _element (aInvoice, 5));
  }

  @Test
  public void testMinimal ()
  {
    assertEquals ("BCD\n002\n1\nSCT\n\nFrançois D'Alsace S.A.\n" + IBAN + "\nEUR12.30",
                  PhloxEpcPayload.getPayload (_invoice ()));
    assertNotNull (PhloxEpcPayload.getPayload (_invoice (_account ("30", IBAN))));
  }

  @Test
  public void testAllElements ()
  {
    final PhloxInvoice aInvoice = _invoice (_account ("30", "FR14 2004 1010 0505 0001 3M02 606").setAccountName ("Franz Mustermänn")
                                                                                               .setServiceProviderID ("BHBLDEHHXXX"));
    aInvoice.getPaymentInstructions ().setRemittanceInformation ("Client:Marie Louise La Lune");
    assertEquals ("BCD\n002\n1\nSCT\nBHBLDEHHXXX\nFranz Mustermänn\n" +
                  IBAN +
                  "\nEUR12.30\n\n\nClient:Marie Louise La Lune",
                  PhloxEpcPayload.getPayload (aInvoice));
  }

  @Test
  public void testRemittance ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    final PhloxPaymentInstructions aPayment = aInvoice.getPaymentInstructions ();

    // An ISO 11649 reference is structured, and the last element has no separator after it
    aPayment.setRemittanceInformation ("RF18 5390 0754 7034");
    assertTrue (PhloxEpcPayload.getPayload (aInvoice).endsWith ("\nEUR12.30\n\nRF18539007547034"));

    // Line breaks would add elements
    aPayment.setRemittanceInformation ("Invoice 1\r\nOrder 2");
    assertTrue (PhloxEpcPayload.getPayload (aInvoice).endsWith ("\nEUR12.30\n\n\nInvoice 1 Order 2"));

    aPayment.setRemittanceInformation ("x".repeat (200));
    assertTrue (PhloxEpcPayload.getPayload (aInvoice).endsWith ("\n\n\n" + "x".repeat (140)));
  }

  @Test
  public void testName ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    aInvoice.setPayee (new PhloxPayee ().setName ("Payee"));
    assertEquals ("Payee", _element (aInvoice, 5));

    final PhloxInvoice aNamed = _invoice (_account ("58", IBAN).setAccountName ("Account"));
    aNamed.setPayee (new PhloxPayee ().setName ("Payee"));
    assertEquals ("Account", _element (aNamed, 5));

    // Cutting the name of the beneficiary could fail the payment
    aInvoice.setPayee (new PhloxPayee ().setName ("x".repeat (70)));
    assertNotNull (PhloxEpcPayload.getPayload (aInvoice));
    aInvoice.setPayee (new PhloxPayee ().setName ("x".repeat (71)));
    assertNull (PhloxEpcPayload.getPayload (aInvoice));
  }

  @Test
  public void testBIC ()
  {
    for (final String sBIC : new String [] { "BHBLDEHH", "BHBLDEHHXXX" })
      assertEquals (sBIC, _element (_invoice (_account ("58", IBAN).setServiceProviderID (sBIC)), 4));
    // Optional, so an unusable one is left out
    assertEquals ("", _element (_invoice (_account ("58", IBAN).setServiceProviderID ("12345")), 4));
  }

  @Test
  public void testFirstAccountWithMatchingMeans ()
  {
    final String sOther = "DE71110220330123456789";
    assertTrue (PhloxEpcPayload.getPayload (_invoice (_account ("48", sOther), _account ("58", IBAN))).contains (IBAN));
    assertTrue (PhloxEpcPayload.getPayload (_invoice (_account ("30", IBAN), _account ("58", sOther))).contains (IBAN));
    // Only the first match counts, there is no fallback to the next one
    assertNull (PhloxEpcPayload.getPayload (_invoice (_account ("30", "1234567"), _account ("58", IBAN))));
    assertNull (PhloxEpcPayload.getPayload (_invoice (_account ("48", IBAN), _account ("49", IBAN))));
    assertNull (PhloxEpcPayload.getPayload (_invoice (_account (null, IBAN))));
    assertNull (PhloxEpcPayload.getPayload (_invoice (new PhloxCreditTransfer [0])));
  }

  @Test
  public void testAmount ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    for (final String [] aCase : new String [] [] { { "0.01", "EUR0.01" },
                                                    { "999999999.99", "EUR999999999.99" },
                                                    { "1200", "EUR1200.00" },
                                                    { "12.300", "EUR12.30" } })
    {
      aInvoice.getDocumentTotals ().setPayableAmount (new BigDecimal (aCase[0]));
      assertTrue (aCase[0], PhloxEpcPayload.getPayload (aInvoice).endsWith ("\n" + aCase[1]));
    }

    for (final String sAmount : new String [] { "0", "0.00", "-5.00", "1000000000.00", "12.345" })
    {
      aInvoice.getDocumentTotals ().setPayableAmount (new BigDecimal (sAmount));
      assertNull (sAmount, PhloxEpcPayload.getPayload (aInvoice));
    }

    aInvoice.getDocumentTotals ().setPayableAmount (null);
    assertNull (PhloxEpcPayload.getPayload (aInvoice));
  }

  @Test
  public void testNotPayable ()
  {
    final PhloxInvoice aUSD = _invoice ();
    aUSD.setCurrency ("USD");
    assertNull (PhloxEpcPayload.getPayload (aUSD));

    final PhloxInvoice aCreditNote = _invoice ();
    aCreditNote.setKind (EPhloxDocumentKind.CREDIT_NOTE);
    assertNull (PhloxEpcPayload.getPayload (aCreditNote));

    final PhloxInvoice aNoPayment = _invoice ();
    aNoPayment.setPaymentInstructions (null);
    assertNull (PhloxEpcPayload.getPayload (aNoPayment));

    assertNull (PhloxEpcPayload.getPayload (_invoice (_account ("58", null))));
  }

  @Test
  public void testPayloadLimit ()
  {
    final PhloxInvoice aInvoice = _invoice (_account ("58", IBAN).setAccountName ("€".repeat (70)));
    assertTrue (PhloxEpcPayload.getPayload (aInvoice).getBytes (StandardCharsets.UTF_8).length <= 331);
    aInvoice.getPaymentInstructions ().setRemittanceInformation ("€".repeat (100));
    assertNull (PhloxEpcPayload.getPayload (aInvoice));
  }

  @NonNull
  private static String _decode (final byte @NonNull [] aPng) throws IOException, ReaderException
  {
    final BufferedImage aImage = ImageIO.read (new NonBlockingByteArrayInputStream (aPng));
    final int nSize = aImage.getWidth ();
    final int [] aPixels = aImage.getRGB (0, 0, nSize, nSize, null, 0, nSize);
    final BinaryBitmap aBitmap = new BinaryBitmap (new HybridBinarizer (new RGBLuminanceSource (nSize, nSize, aPixels)));
    return new QRCodeReader ().decode (aBitmap, Map.of (DecodeHintType.PURE_BARCODE, Boolean.TRUE)).getText ();
  }

  @Nullable
  private static PhloxResolvedImage _findQrCode (@NonNull final PhloxInvoice aInvoice)
  {
    final PhloxTemplateAst aAst = PhloxTemplate.getDefault ().renderAst (aInvoice, new TestCtx ());
    final Wrapper <PhloxResolvedImage> aFound = new Wrapper <> ();
    aAst.getRoot ().accept (new AbstractVisitor ()
    {
      @Override
      public void visit (final Image aImage)
      {
        if (aImage.getDestination ().startsWith ("qrcode:"))
          aFound.set (aAst.getResolvedImage (aImage));
      }
    });
    return aFound.get ();
  }

  @Test
  public void testDefaultTemplate () throws IOException, ReaderException
  {
    final PhloxInvoice aInvoice = _invoice (_account ("58", IBAN).setAccountName ("Müller & Söhne | Łódź"));
    aInvoice.getPaymentInstructions ().setRemittanceInformation ("Invoice 1 (2)\nOrder_3");
    final PhloxResolvedImage aImage = _findQrCode (aInvoice);
    assertNotNull (aImage);
    assertEquals ("SEPA payment QR code", aImage.getAltText ());
    assertEquals (PhloxEpcPayload.getPayload (aInvoice), _decode (aImage.getBinary ()));

    aInvoice.setCurrency ("USD");
    assertNull (_findQrCode (aInvoice));
  }

  private static int _countQrCodes (@NonNull final IInvoiceReader <InputStream> aReader,
                                    @NonNull final Iterable <ClassPathResource> aFiles) throws IOException
  {
    int ret = 0;
    for (final ClassPathResource aRes : aFiles)
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PhloxInvoice aInvoice = aReader.read (aIS, new ErrorList ());
        assertNotNull (aRes.getPath (), aInvoice);
        if (PhloxEpcPayload.getPayload (aInvoice) != null)
          ret++;
      }
    return ret;
  }

  @Test
  public void testAllReadersFillThePaymentMeans () throws IOException
  {
    assertTrue (_countQrCodes (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getUbl21InvoiceFiles ()) > 0);
    assertTrue (_countQrCodes (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getUbl25InvoiceFiles ()) > 0);
    assertTrue (_countQrCodes (CiiInvoiceReader.fromD16B (), MockPhloxTestFiles.getCiiD16BFiles ()) > 0);
    assertTrue (_countQrCodes (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles ()) > 0);
    // A credit note never has one
    assertEquals (0, _countQrCodes (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getUbl21CreditNoteFiles ()));
    assertEquals (0, _countQrCodes (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getUbl25CreditNoteFiles ()));
  }
}
