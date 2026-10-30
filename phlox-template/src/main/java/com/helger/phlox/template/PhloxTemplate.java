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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.cache.impl.ProviderCache;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.i18n.IPhloxLabelResolver;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.template.IPhloxTemplate;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.adapter.PhloxInvoiceMustacheContext;
import com.helger.phlox.template.adapter.PhloxTemplateCoverage;
import com.helger.phlox.template.ast.PhloxTemplateAst;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;

/**
 * Compiled phlox template — the public facade implementing {@link IPhloxTemplate}. Created via one
 * of the static factories ({@link #fromString(String)}, {@link #fromClasspath(String)},
 * {@link #fromFile(File)}, {@link #getDefault()}); each instance holds the raw {@code .ptpl} source
 * plus a shared commonmark-java {@link Parser}.
 * <p>
 * Every factory checks at load time whether the template shows all BTs of the model. What happens
 * if it does not is defined by {@link EPhloxTemplateCoverageMode}; the factories without a mode use
 * {@link #DEFAULT_COVERAGE_MODE}.
 * <p>
 * The locale-dependent work (label substitution + JMustache compile) is deferred to
 * {@link #localize(Locale, IPhloxLabelResolver)}, which is backed by a ph-cache keyed by
 * {@code (locale, resolver identity)}. The cache makes repeat renders in the same locale a single
 * map lookup — see {@code wiki/Template-Processing.md} § Threading and caching.
 * <p>
 * Instances are immutable after construction and safe to use concurrently from many threads.
 *
 * @author Philip Helger
 */
public final class PhloxTemplate implements IPhloxTemplate
{
  /**
   * Cache key for the localize step. Locale uses value equality (so {@code Locale.GERMAN} and
   * {@code new Locale("de")} hit the same entry); the resolver uses reference identity, so callers
   * must reuse the same resolver instance to hit the cache. The default resolver
   * ({@link com.helger.phlox.api.i18n.DefaultPhloxLabelResolver#INSTANCE}) is process-wide, so
   * default usage naturally hits.
   */
  private static final class CacheKey
  {
    private final Locale m_aLocale;
    private final IPhloxLabelResolver m_aResolver;

    CacheKey (@NonNull final Locale aLocale, @NonNull final IPhloxLabelResolver aResolver)
    {
      m_aLocale = aLocale;
      m_aResolver = aResolver;
    }

    @NonNull
    Locale locale ()
    {
      return m_aLocale;
    }

    @NonNull
    IPhloxLabelResolver resolver ()
    {
      return m_aResolver;
    }

    @Override
    public boolean equals (final Object o)
    {
      if (this == o)
        return true;
      if (o == null || !getClass ().equals (o.getClass ()))
        return false;
      final CacheKey rhs = (CacheKey) o;
      // Locale uses value equality; resolver is compared by identity on purpose so that
      // callers swapping in a fresh resolver instance get a fresh cache entry.
      return EqualsHelper.equals (m_aLocale, rhs.m_aLocale) && m_aResolver == rhs.m_aResolver;
    }

    @Override
    public int hashCode ()
    {
      return new HashCodeGenerator (this).append (m_aLocale)
                                         .append (System.identityHashCode (m_aResolver))
                                         .getHashCode ();
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (null).append ("Locale", m_aLocale).append ("Resolver", m_aResolver).getToString ();
    }
  }

  /** Classpath path of the default template shipped in this module. */
  public static final String DEFAULT_CLASSPATH_PATH = "templates/default.ptpl";

  /** The coverage mode of the factories without an explicit mode. */
  public static final EPhloxTemplateCoverageMode DEFAULT_COVERAGE_MODE = EPhloxTemplateCoverageMode.WARN;

  private static final Logger LOGGER = LoggerFactory.getLogger (PhloxTemplate.class);

  private static final List <Extension> COMMONMARK_EXTENSIONS = List.of (TablesExtension.create (),
                                                                         StrikethroughExtension.create ());

  // Lazily initialised singleton — see getDefault().
  private static volatile PhloxTemplate s_aDefault;

  // Monotonically increasing per-JVM template id — used to disambiguate cache names so two
  // templates built from the same .ptpl source get distinct entries in JMX / ph-cache logs.
  private static final AtomicInteger NEXT_ID = new AtomicInteger (0);

  private final int m_nID = NEXT_ID.incrementAndGet ();
  private final String m_sSource;
  private final String m_sDisplayName;
  private final ICommonsList <String> m_aMissingBTs;
  private final Parser m_aCMParser;
  private final ProviderCache <CacheKey, PhloxLocalizedTemplate> m_aLocalizeCache;

  @NonNull
  private PhloxLocalizedTemplate _buildLocalized (@NonNull final CacheKey aKey)
  {
    final String sBaked = PhloxInvoiceMustacheContext.preprocess (m_sSource, aKey.locale (), aKey.resolver ());
    final Template aTemplate;
    try
    {
      aTemplate = Mustache.compiler ()
                          .escapeHTML (false)
                          .defaultValue ("")
                          .nullValue ("")
                          .emptyStringIsFalse (true)
                          .zeroIsFalse (true)
                          .withCollector (PhloxInvoiceMustacheContext.getCollector ())
                          .compile (sBaked);
    }
    catch (final MustacheException ex)
    {
      throw new PhloxTemplateException (EStage.SUBSTITUTE, null, "JMustache compile failed: " + ex.getMessage (), ex);
    }
    return new PhloxLocalizedTemplate (sBaked, aTemplate, m_aCMParser);
  }

  private PhloxTemplate (@NonNull final String sSource,
                         @Nullable final String sDisplayName,
                         @NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    m_sSource = sSource;
    m_sDisplayName = sDisplayName;
    m_aMissingBTs = PhloxTemplateCoverage.getMissingBTs (sSource);
    m_aCMParser = Parser.builder ().extensions (COMMONMARK_EXTENSIONS).build ();
    m_aLocalizeCache = ProviderCache.<CacheKey, PhloxLocalizedTemplate> builder ()
                                    .valueProvider (this::_buildLocalized)
                                    .name ("phlox-template-" + m_nID)
                                    .maxSize (1_000)
                                    .build ();
    checkCoverage (eCoverageMode);
  }

  /**
   * Build a template from a raw {@code .ptpl} source string. The source is held as-is; no label
   * substitution or JMustache compile happens until {@link #localize} is called. Uses
   * {@link #DEFAULT_COVERAGE_MODE}.
   *
   * @param sSource
   *        Raw template text. Must be valid {@code .ptpl} per the grammar in
   *        {@code wiki/Template-Language.md}.
   * @return A fresh {@link PhloxTemplate} ready to be localized.
   */
  @NonNull
  public static PhloxTemplate fromString (@NonNull final String sSource)
  {
    return fromString (sSource, DEFAULT_COVERAGE_MODE);
  }

  /**
   * Build a template from a raw {@code .ptpl} source string.
   *
   * @param sSource
   *        Raw template text. Must be valid {@code .ptpl} per the grammar in
   *        {@code wiki/Template-Language.md}.
   * @param eCoverageMode
   *        What to do if the template does not show all BTs.
   * @return A fresh {@link PhloxTemplate} ready to be localized.
   * @throws PhloxTemplateException
   *         when the mode is {@link EPhloxTemplateCoverageMode#REJECT} and BTs are missing.
   */
  @NonNull
  public static PhloxTemplate fromString (@NonNull final String sSource,
                                          @NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    return new PhloxTemplate (sSource, null, eCoverageMode);
  }

  /**
   * Build a template from a classpath resource (e.g. {@code "templates/foo.ptpl"}). The resource is
   * read as UTF-8. Uses {@link #DEFAULT_COVERAGE_MODE}.
   *
   * @param sPath
   *        Classpath path, relative to the classloader's root.
   * @return A fresh {@link PhloxTemplate} built from the resource contents.
   * @throws PhloxTemplateException
   *         when the resource does not exist or cannot be read.
   */
  @NonNull
  public static PhloxTemplate fromClasspath (@NonNull final String sPath)
  {
    return fromClasspath (sPath, DEFAULT_COVERAGE_MODE);
  }

  /**
   * Build a template from a classpath resource (e.g. {@code "templates/foo.ptpl"}). The resource is
   * read as UTF-8.
   *
   * @param sPath
   *        Classpath path, relative to the classloader's root.
   * @param eCoverageMode
   *        What to do if the template does not show all BTs.
   * @return A fresh {@link PhloxTemplate} built from the resource contents.
   * @throws PhloxTemplateException
   *         when the resource does not exist or cannot be read, or when the mode is
   *         {@link EPhloxTemplateCoverageMode#REJECT} and BTs are missing.
   */
  @NonNull
  public static PhloxTemplate fromClasspath (@NonNull final String sPath,
                                             @NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    final ClassPathResource aRes = new ClassPathResource (sPath);
    if (!aRes.exists ())
      throw new PhloxTemplateException (EStage.SUBSTITUTE, sPath, "classpath template not found: " + sPath);

    try (final InputStream aIS = aRes.getInputStream ())
    {
      if (aIS == null)
        throw new PhloxTemplateException (EStage.SUBSTITUTE, sPath, "classpath template has no stream: " + sPath);
      return new PhloxTemplate (new String (aIS.readAllBytes (), StandardCharsets.UTF_8), sPath, eCoverageMode);
    }
    catch (final IOException ex)
    {
      throw new PhloxTemplateException (EStage.SUBSTITUTE, sPath, "failed to read classpath template", ex);
    }
  }

  /**
   * Build a template from a filesystem path. The file is read as UTF-8. Uses
   * {@link #DEFAULT_COVERAGE_MODE}.
   *
   * @param aFile
   *        Source file; must exist and be readable.
   * @return A fresh {@link PhloxTemplate} built from the file's contents.
   * @throws PhloxTemplateException
   *         when the file cannot be opened or read.
   */
  @NonNull
  public static PhloxTemplate fromFile (@NonNull final File aFile)
  {
    return fromFile (aFile, DEFAULT_COVERAGE_MODE);
  }

  /**
   * Build a template from a filesystem path. The file is read as UTF-8.
   *
   * @param aFile
   *        Source file; must exist and be readable.
   * @param eCoverageMode
   *        What to do if the template does not show all BTs.
   * @return A fresh {@link PhloxTemplate} built from the file's contents.
   * @throws PhloxTemplateException
   *         when the file cannot be opened or read, or when the mode is
   *         {@link EPhloxTemplateCoverageMode#REJECT} and BTs are missing.
   */
  @NonNull
  public static PhloxTemplate fromFile (@NonNull final File aFile,
                                        @NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    try
    {
      return new PhloxTemplate (Files.readString (aFile.toPath (), StandardCharsets.UTF_8), aFile.getPath (), eCoverageMode);
    }
    catch (final IOException ex)
    {
      throw new PhloxTemplateException (EStage.SUBSTITUTE, aFile.getPath (), "failed to read template file", ex);
    }
  }

  /**
   * Get the default template shipped in this module — a single language-independent.
   *
   * @return The default template shipped in this module — a single language-independent
   *         {@code default.ptpl}. The writers use it when the render context has no template.
   *         Returned instance is a lazily created singleton.
   */
  @NonNull
  public static PhloxTemplate getDefault ()
  {
    PhloxTemplate aLocal = s_aDefault;
    if (aLocal == null)
    {
      synchronized (PhloxTemplate.class)
      {
        aLocal = s_aDefault;
        if (aLocal == null)
        {
          aLocal = fromClasspath (DEFAULT_CLASSPATH_PATH);
          s_aDefault = aLocal;
        }
      }
    }
    return aLocal;
  }

  /**
   * Return a locale-baked, JMustache-compiled variant of this template. Subsequent calls with the
   * same {@code (locale, resolver instance)} pair return the cached instance. The cache keys the
   * resolver by reference identity, so callers using a custom resolver must reuse the same instance
   * across calls to hit the cache.
   *
   * @param aLocale
   *        Locale used for label lookups in stage 0.
   * @param aResolver
   *        Label resolver used for label lookups in stage 0.
   * @return The cached or freshly built {@link PhloxLocalizedTemplate} for the given pair.
   */
  @NonNull
  public PhloxLocalizedTemplate localize (@NonNull final Locale aLocale, @NonNull final IPhloxLabelResolver aResolver)
  {
    return m_aLocalizeCache.getFromCache (new CacheKey (aLocale, aResolver));
  }

  /**
   * Convenience overload — calls {@link #localize(Locale, IPhloxLabelResolver)} with the locale and
   * label resolver pulled from {@code aCtx}.
   *
   * @param aCtx
   *        Render context whose locale and label resolver drive the localization.
   * @return The cached or freshly built {@link PhloxLocalizedTemplate}.
   */
  @NonNull
  public PhloxLocalizedTemplate localize (@NonNull final AbstractRenderContext <?> aCtx)
  {
    return localize (aCtx.getLocale (), aCtx.getLabelResolver ());
  }

  /**
   * Run the full pipeline (stages 0 / 0a / 1 / 2 / 2a) and return the parsed AST + sidecar maps.
   * The localize step is cached, so repeat renders in the same locale pay only stages 1 + 2 + 2a.
   *
   * @param aInvoice
   *        Source invoice to substitute into the template.
   * @param aCtx
   *        Render context driving locale, label resolver, format options and image resolution.
   * @return The post-processed {@link PhloxTemplateAst} ready for an emi1tter.
   * @throws PhloxTemplateException
   *         when any stage fails (compile, execute, parse, post-pass).
   */
  @NonNull
  public PhloxTemplateAst renderAst (@NonNull final PhloxInvoice aInvoice,
                                     @NonNull final AbstractRenderContext <?> aCtx)
  {
    return localize (aCtx).renderAst (aInvoice, aCtx);
  }

  /**
   * Convenience that returns just the substituted CommonMark text without parsing. Useful for
   * snapshot testing stage 1 in isolation.
   *
   * @param aInvoice
   *        Source invoice to substitute into the template.
   * @param aCtx
   *        Render context driving locale, label resolver, and format options.
   * @return The CommonMark text produced by stage 1 (post-substitution, pre-parse).
   * @throws PhloxTemplateException
   *         when the template fails to substitute.
   */
  @NonNull
  public String renderSubstituted (@NonNull final PhloxInvoice aInvoice, @NonNull final AbstractRenderContext <?> aCtx)
  {
    return localize (aCtx).renderSubstituted (aInvoice, aCtx);
  }

  /**
   * Get the BTs that this template does not show.
   *
   * @return The names of the missing BTs in numerical order, empty if the template shows all of
   *         them. Never {@code null}.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getMissingBTs ()
  {
    return new CommonsArrayList <> (m_aMissingBTs);
  }

  /**
   * Apply a coverage mode to this template. The factories already do this at load time, so this is
   * only needed to apply a stricter mode to a template that was loaded elsewhere, e.g. by a theme.
   *
   * @param eCoverageMode
   *        What to do if the template does not show all BTs.
   * @throws PhloxTemplateException
   *         when the mode is {@link EPhloxTemplateCoverageMode#REJECT} and BTs are missing.
   */
  public void checkCoverage (@NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    if (m_aMissingBTs.isEmpty ())
      return;

    final String sMessage = "Template " +
                            (m_sDisplayName == null ? "" : "'" + m_sDisplayName + "' ") +
                            "does not show " +
                            m_aMissingBTs.size () +
                            " BT(s): " +
                            String.join (", ", m_aMissingBTs);
    if (eCoverageMode == EPhloxTemplateCoverageMode.REJECT)
      throw new PhloxTemplateException (EStage.LOAD, null, sMessage);
    LOGGER.warn (sMessage);
  }

  /**
   * Get the raw template source — the {@code .ptpl} text the instance was built from.
   *
   * @return The raw template source — the {@code .ptpl} text the instance was built from. Intended
   *         for diagnostics (error messages, debug dumps); never the runtime render path, since
   *         stage 0 has not yet been applied.
   */
  @NonNull
  public String getSourceForDebug ()
  {
    return m_sSource;
  }
}
