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

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.NonNull;

import com.helger.base.wrapper.Wrapper;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.adapter.PhloxInvoiceMustacheContext;
import com.helger.phlox.template.ast.PhloxTemplateAst;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

/**
 * Locale-baked, JMustache-compiled template — the artifact produced by stages 0 and 0a of the
 * pipeline (see the wiki page Template-Processing, section "Pipeline overview"). One
 * {@link PhloxLocalizedTemplate} is reusable across many invoices that share the same
 * {@code (template, locale, label-resolver, annotation)} key.
 * <p>
 * Immutable after construction and safe to share across threads. Held by {@link PhloxTemplate}
 * inside a ph-cache instance; callers who want explicit control can obtain a localized variant via
 * {@link PhloxTemplate#localize}.
 *
 * @author Philip Helger
 */
public final class PhloxLocalizedTemplate
{
  private final String m_sLocaleBakedSource;
  private final Template m_aMustache;
  private final Parser m_aParser;

  /** Package-private — constructed only by {@link PhloxTemplate}. */
  PhloxLocalizedTemplate (@NonNull final String sLocaleBakedSource,
                          @NonNull final Template aMustache,
                          @NonNull final Parser aParser)
  {
    m_sLocaleBakedSource = sLocaleBakedSource;
    m_aMustache = aMustache;
    m_aParser = aParser;
  }

  /**
   * Get the locale-baked source — the {@code .ptpl} text after stage 0.
   *
   * @return The locale-baked source — the {@code .ptpl} text after stage 0. Kept for diagnostics /
   *         snapshot tests; never the runtime render path.
   */
  @NonNull
  public String getLocaleBakedSource ()
  {
    return m_sLocaleBakedSource;
  }

  /**
   * Run stage 1 in isolation — substitute the invoice into the compiled template and return the
   * CommonMark string without parsing it. Mainly useful for snapshot testing.
   *
   * @param aInvoice
   *        The invoice to substitute. Never {@code null}.
   * @param aCtx
   *        The render context. Never {@code null}.
   * @return The substituted CommonMark source. Never {@code null}.
   */
  @NonNull
  public String renderSubstituted (@NonNull final PhloxInvoice aInvoice, @NonNull final AbstractRenderContext <?> aCtx)
  {
    final Wrapper <String> aOut = new Wrapper <> ();
    PhloxInvoiceMustacheContext.withEnv (aInvoice, aCtx, () -> {
      try
      {
        aOut.set (m_aMustache.execute (aInvoice));
      }
      catch (final MustacheException ex)
      {
        Throwable aCause = ex.getCause ();
        while (aCause != null)
        {
          if (aCause instanceof final PhloxTemplateException aPhloxCause)
            throw aPhloxCause;
          aCause = aCause.getCause ();
        }
        throw new PhloxTemplateException (EStage.SUBSTITUTE, null, "JMustache execute failed: " + ex.getMessage (), ex);
      }
    });
    return aOut.get ();
  }

  /**
   * Run stages 1 + 2 + 2a — substitute the invoice into the compiled template, parse the result as
   * CommonMark, then run the sentinel + image post-pass.
   *
   * @param aInvoice
   *        Source invoice.
   * @param aCtx
   *        Render context.
   * @return Populated {@link PhloxTemplateAst} ready for an emitter.
   */
  @NonNull
  public PhloxTemplateAst renderAst (@NonNull final PhloxInvoice aInvoice,
                                     @NonNull final AbstractRenderContext <?> aCtx)
  {
    final String sSubstituted = renderSubstituted (aInvoice, aCtx);

    final Node aRoot;
    try
    {
      aRoot = m_aParser.parse (sSubstituted);
    }
    catch (final RuntimeException ex)
    {
      throw new PhloxTemplateException (EStage.PARSE, null, "commonmark parse failed: " + ex.getMessage (), ex);
    }

    final PhloxTemplatePostParser aPostParser = new PhloxTemplatePostParser (aInvoice, aCtx);
    aPostParser.process (aRoot);

    return new PhloxTemplateAst (aRoot, aPostParser.getBlockClassMap (), aPostParser.getImageMap (), aInvoice, aCtx);
  }
}
