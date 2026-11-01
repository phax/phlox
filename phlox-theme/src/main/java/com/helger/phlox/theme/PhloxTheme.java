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
package com.helger.phlox.theme;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.io.resource.ClassPathResource;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * The layout parameters of the HTML, the PDF and the plain-text output, read from a JSON file with
 * {@link PhloxThemeReader}. A theme has five optional parts:
 * <ul>
 * <li><code>variables</code> - named values like CSS custom properties. The name starts with
 * <code>--</code>, a value is used with <code>var(--name)</code> or <code>var(--name, fallback)</code>
 * inside every text value of the theme. The variables are also written into the HTML page as CSS
 * custom properties, so the stylesheet can use them. The colours of the default layout are the
 * variables {@link #VAR_INK}, {@link #VAR_MUTED}, {@link #VAR_FAINT}, {@link #VAR_RULE},
 * {@link #VAR_WASH}, {@link #VAR_ACCENT} and {@link #VAR_ACCENT_WASH}.</li>
 * <li><code>common</code> - the parameters of all outputs (language, formats, template, logo,
 * watermark), see {@link PhloxThemeApplier}.</li>
 * <li><code>html</code> - the parameters of the HTML output.</li>
 * <li><code>pdf</code> - the parameters of the PDF output.</li>
 * <li><code>text</code> - the parameters of the plain-text output.</li>
 * </ul>
 * Every key that starts with <code>$</code> is a comment or other information for the reader of the
 * file and is ignored. A key that is not known is an error, so a typo is found.
 * <p>
 * A theme is trusted input: every file reference ({@link #resolveFile(String)},
 * {@link #readBinary(String)}) may leave the folder of the theme, and a <code>classpath:</code>
 * reference may name any resource. A logo reference such as <code>../../etc/hosts</code> therefore
 * puts the content of that file into the output. An application that accepts themes from its users
 * or tenants must check the file references of the theme itself; phlox has no base folder
 * restriction.
 * </p>
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxTheme
{
  /** The text colour of the default layout. */
  public static final String VAR_INK = "--phlox-ink";
  /** The colour of secondary text in the default layout. */
  public static final String VAR_MUTED = "--phlox-muted";
  /** The colour of the faintest text in the default layout. */
  public static final String VAR_FAINT = "--phlox-faint";
  /** The colour of thin lines in the default layout. */
  public static final String VAR_RULE = "--phlox-rule";
  /** The light background colour of the default layout. */
  public static final String VAR_WASH = "--phlox-wash";
  /** The accent colour of the default layout. */
  public static final String VAR_ACCENT = "--phlox-accent";
  /** The light background in the accent colour of the default layout. */
  public static final String VAR_ACCENT_WASH = "--phlox-accent-wash";

  private static final String CLASSPATH_PREFIX = "classpath:";

  private final PhloxThemeVariables m_aVariables;
  private final PhloxThemeObject m_aRoot;
  private final Path m_aBaseDir;

  PhloxTheme (@NonNull final PhloxThemeVariables aVariables,
              @NonNull final IJsonObject aRoot,
              @Nullable final Path aBaseDir)
  {
    m_aVariables = aVariables;
    m_aRoot = new PhloxThemeObject (aRoot, "", aVariables);
    m_aBaseDir = aBaseDir;
  }

  @NonNull
  private PhloxThemeObject _getPart (@NonNull final String sName)
  {
    final PhloxThemeObject ret = m_aRoot.getObject (sName);
    return ret != null ? ret : new PhloxThemeObject (new JsonObject (), sName, m_aVariables);
  }

  /**
   * @return The variables with the names including <code>--</code> and the values as written in the
   *         theme, so a value can still contain <code>var(...)</code>. Never <code>null</code>.
   */
  @NonNull
  public ICommonsOrderedMap <String, String> getVariables ()
  {
    return new CommonsLinkedHashMap <> (m_aVariables.getRaw ());
  }

  /**
   * Get the value of a variable.
   *
   * @param sName
   *        The name including <code>--</code>. May not be <code>null</code>.
   * @return The value with all references replaced or <code>null</code> if the variable is not
   *         defined.
   */
  @Nullable
  public String getVariable (@NonNull final String sName)
  {
    return m_aVariables.resolveVariable (sName);
  }

  /**
   * Get the colour of a variable.
   *
   * @param sName
   *        The name including <code>--</code>. May not be <code>null</code>.
   * @param aDefault
   *        The colour to use if the variable is not defined. May be <code>null</code>.
   * @return The colour of the variable or the default.
   * @throws PhloxThemeException
   *         If the variable is defined but is no colour.
   */
  @Nullable
  public Color getColorVariable (@NonNull final String sName, @Nullable final Color aDefault)
  {
    final String sValue = getVariable (sName);
    if (sValue == null)
      return aDefault;
    try
    {
      return PhloxThemeValues.parseColor (sValue);
    }
    catch (final IllegalArgumentException ex)
    {
      throw new PhloxThemeException ("Theme error at 'variables." + sName + "': " + ex.getMessage ());
    }
  }

  /**
   * @return The parameters that are used by all outputs. Never <code>null</code>.
   */
  @NonNull
  public PhloxThemeObject getCommon ()
  {
    return _getPart ("common");
  }

  /**
   * @return The parameters of the HTML output. Never <code>null</code>.
   */
  @NonNull
  public PhloxThemeObject getHtml ()
  {
    return _getPart ("html");
  }

  /**
   * @return The parameters of the PDF output. Never <code>null</code>.
   */
  @NonNull
  public PhloxThemeObject getPdf ()
  {
    return _getPart ("pdf");
  }

  /**
   * @return The parameters of the plain-text output. Never <code>null</code>.
   */
  @NonNull
  public PhloxThemeObject getText ()
  {
    return _getPart ("text");
  }

  /**
   * Resolve the reference to a file. The result is not restricted to the folder of the theme, see
   * the class documentation.
   *
   * @param sReference
   *        A path. A relative path is relative to the folder of the theme file, or to the current
   *        folder if the theme was not read from a file. May not be <code>null</code>.
   * @return The file. Never <code>null</code>.
   */
  @NonNull
  public File resolveFile (@NonNull final String sReference)
  {
    ValueEnforcer.notNull (sReference, "Reference");
    final Path aPath = Path.of (sReference);
    return (m_aBaseDir == null ? aPath : m_aBaseDir.resolve (aPath)).toFile ();
  }

  /**
   * Read a file that the theme refers to.
   *
   * @param sReference
   *        A path, see {@link #resolveFile(String)}, or <code>classpath:</code> and the path of a
   *        resource. May not be <code>null</code>.
   * @return The content of the file. Never <code>null</code>.
   * @throws PhloxThemeException
   *         If the file cannot be read.
   */
  public byte @NonNull [] readBinary (@NonNull final String sReference)
  {
    ValueEnforcer.notNull (sReference, "Reference");
    try
    {
      if (sReference.startsWith (CLASSPATH_PREFIX))
      {
        final String sPath = sReference.substring (CLASSPATH_PREFIX.length ());
        try (final InputStream aIS = new ClassPathResource (sPath).getInputStream ())
        {
          if (aIS == null)
            throw new PhloxThemeException ("The theme refers to the missing resource '" + sPath + "'");
          return aIS.readAllBytes ();
        }
      }
      return Files.readAllBytes (resolveFile (sReference).toPath ());
    }
    catch (final IOException ex)
    {
      throw new PhloxThemeException ("The theme refers to '" + sReference + "' which cannot be read", ex);
    }
  }
}
