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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.io.resource.IReadableResource;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.serialize.JsonReader;

/**
 * Reads a {@link PhloxTheme} from JSON.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxThemeReader
{
  private PhloxThemeReader ()
  {}

  @NonNull
  private static PhloxTheme _create (@Nullable final IJson aJson,
                                     @NonNull final Wrapper <String> aError,
                                     @Nullable final Path aBaseDir)
  {
    if (aJson == null)
      throw new PhloxThemeException ("The theme is no valid JSON" +
                                     (aError.get () != null ? ": " + aError.get () : ""));
    if (!aJson.isObject ())
      throw new PhloxThemeException ("The theme must be a JSON object");
    final IJsonObject aRoot = aJson.getAsObject ();

    // The variables come first, because every other value may refer to them
    final ICommonsOrderedMap <String, String> aRaw = new CommonsLinkedHashMap <> ();
    final IJson aVariablesJson = aRoot.get ("variables");
    if (aVariablesJson != null)
    {
      if (!aVariablesJson.isObject ())
        throw new PhloxThemeException ("Theme error at 'variables': an object is expected");
      aVariablesJson.getAsObject ().forEach ((sName, aValue) -> {
        if (sName.startsWith ("$"))
          return;
        if (!sName.startsWith ("--"))
          throw new PhloxThemeException ("Theme error at 'variables." +
                                         sName +
                                         "': the name of a variable starts with --");
        if (!aValue.isValue () || aValue.getAsValue ().getValue () == null)
          throw new PhloxThemeException ("Theme error at 'variables." + sName + "': a text or number is expected");
        aRaw.put (sName, aValue.getAsValue ().getValue ().toString ());
      });
    }

    final PhloxThemeVariables aVariables = new PhloxThemeVariables (aRaw);
    final PhloxTheme ret = new PhloxTheme (aVariables, aRoot, aBaseDir);
    new PhloxThemeObject (aRoot, "", aVariables).checkKeys ("variables", "common", "html", "pdf", "text");
    // Fail now and not only when the part is used: the values of the variables must be valid
    for (final String sName : aRaw.keySet ())
      ret.getVariable (sName);
    return ret;
  }

  /**
   * Read a theme from a file. Relative paths in the theme are relative to the folder of the file.
   *
   * @param aFile
   *        The JSON file. May not be <code>null</code>.
   * @return The theme. Never <code>null</code>.
   * @throws PhloxThemeException
   *         If the file cannot be read or has an invalid content.
   */
  @NonNull
  public static PhloxTheme read (@NonNull final Path aFile)
  {
    ValueEnforcer.notNull (aFile, "File");
    try (final InputStream aIS = Files.newInputStream (aFile))
    {
      final Path aParent = aFile.toAbsolutePath ().getParent ();
      return read (aIS, aParent);
    }
    catch (final IOException ex)
    {
      throw new PhloxThemeException ("Failed to read the theme '" + aFile + "'", ex);
    }
  }

  /**
   * Read a theme from a resource.
   *
   * @param aRes
   *        The JSON file. May not be <code>null</code>.
   * @return The theme. Never <code>null</code>.
   * @throws PhloxThemeException
   *         If the file cannot be read or has an invalid content.
   */
  @NonNull
  public static PhloxTheme read (@NonNull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Resource");
    try (final InputStream aIS = aRes.getBufferedInputStream ())
    {
      return read (aIS, null);
    }
    catch (final IOException ex)
    {
      throw new PhloxThemeException ("Failed to read the theme '" + aRes.getPath () + "'", ex);
    }
  }

  /**
   * Read a theme from a stream. The stream is closed.
   *
   * @param aIS
   *        The stream with the UTF-8 encoded JSON. May not be <code>null</code>.
   * @param aBaseDir
   *        The folder that relative paths in the theme are relative to. May be <code>null</code>
   *        for the current folder.
   * @return The theme. Never <code>null</code>.
   * @throws PhloxThemeException
   *         If the content is invalid.
   */
  @NonNull
  public static PhloxTheme read (@NonNull final InputStream aIS, @Nullable final Path aBaseDir)
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    final Wrapper <String> aError = new Wrapper <> ();
    final IJson aJson = JsonReader.builder ()
                                  .source (aIS, StandardCharsets.UTF_8)
                                  .customExceptionCallback (ex -> aError.set (ex.getMessage ()))
                                  .read ();
    return _create (aJson, aError, aBaseDir);
  }

  /**
   * Read a theme from a string.
   *
   * @param sJson
   *        The JSON. May not be <code>null</code>.
   * @param aBaseDir
   *        The folder that relative paths in the theme are relative to. May be <code>null</code>
   *        for the current folder.
   * @return The theme. Never <code>null</code>.
   * @throws PhloxThemeException
   *         If the content is invalid.
   */
  @NonNull
  public static PhloxTheme readFromString (@NonNull final String sJson, @Nullable final Path aBaseDir)
  {
    ValueEnforcer.notNull (sJson, "Json");
    final Wrapper <String> aError = new Wrapper <> ();
    final IJson aJson = JsonReader.builder ()
                                  .source (sJson)
                                  .customExceptionCallback (ex -> aError.set (ex.getMessage ()))
                                  .read ();
    return _create (aJson, aError, aBaseDir);
  }
}
