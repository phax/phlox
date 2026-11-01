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
package com.helger.phlox.html;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.html.hc.config.HCConversionSettings;
import com.helger.html.hc.config.HCSettings;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.render.HCRenderer;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.template.IPhloxTemplate;
import com.helger.phlox.api.write.IInvoiceWriter;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.xml.serialize.write.EXMLSerializeDocType;

/**
 * Renders a {@link PhloxInvoice} as an HTML5 document. Built on {@code ph-oton-html}. The content
 * comes from a template; if the render context has none, the default template is used.
 * <p>
 * phlox sets no limit on the size of its input. The time and the memory a render needs grow with
 * the size of the invoice, so an application that renders invoices from untrusted sources must cap
 * the size of the XML before it reads it.
 *
 * @author Philip Helger
 */
public class HtmlInvoiceWriter implements IInvoiceWriter <HtmlRenderContext>
{
  /** Create a new HTML invoice writer. */
  public HtmlInvoiceWriter ()
  {}

  /**
   * Build the HTML tree as an {@link HCHtml} object without serializing.
   *
   * @param aInvoice
   *        The invoice to render.
   * @param aCtx
   *        The context.
   * @return The populated {@link HCHtml}.
   */
  @NonNull
  public HCHtml writeAsHcHtml (@NonNull final PhloxInvoice aInvoice, @NonNull final HtmlRenderContext aCtx)
  {
    final IPhloxTemplate aTemplate = aCtx.getTemplate () != null ? aCtx.getTemplate () : PhloxTemplate.getDefault ();
    if (!(aTemplate instanceof final PhloxTemplate aPhloxTemplate))
      throw new PhloxRuntimeException ("Unsupported template implementation " + aTemplate.getClass ().getName ());
    return new HtmlTemplateEmitter (aCtx).emit (aPhloxTemplate.renderAst (aInvoice, aCtx));
  }

  /**
   * Render the invoice and return the HTML markup as a {@code String}. Equivalent to
   * {@link #writeTo(PhloxInvoice, HtmlRenderContext, OutputStream)} but keeps the result in
   * memory.
   *
   * @param aInvoice
   *        The invoice.
   * @param aCtx
   *        The HTML render context.
   * @return The HTML5 document as a {@code String}.
   */
  @NonNull
  public String writeAsString (@NonNull final PhloxInvoice aInvoice, @NonNull final HtmlRenderContext aCtx)
  {
    final HCHtml aHtml = writeAsHcHtml (aInvoice, aCtx);
    if (aCtx.isEmitDoctype ())
      return HCRenderer.getAsHTMLString (aHtml);

    final HCConversionSettings aSettings = new HCConversionSettings (HCSettings.getConversionSettings ());
    aSettings.setXMLWriterSettings (aSettings.getMutableXMLWriterSettings ()
                                             .setSerializeDocType (EXMLSerializeDocType.IGNORE));
    return HCRenderer.getAsHTMLString (aHtml, aSettings);
  }

  /**
   * Render the invoice and write it as UTF-8 HTML5 to the given stream. The stream is closed
   * by the writer.
   *
   * @param aInvoice
   *        The invoice.
   * @param aCtx
   *        The HTML render context.
   * @param aOut
   *        The output stream.
   */
  public void writeTo (@NonNull final PhloxInvoice aInvoice,
                       @NonNull final HtmlRenderContext aCtx,
                       @NonNull final OutputStream aOut)
  {
    final String sHtml = writeAsString (aInvoice, aCtx);
    try (final OutputStream aClosing = aOut)
    {
      aClosing.write (sHtml.getBytes (StandardCharsets.UTF_8));
    }
    catch (final IOException ex)
    {
      throw new UncheckedIOException (ex);
    }
  }
}
