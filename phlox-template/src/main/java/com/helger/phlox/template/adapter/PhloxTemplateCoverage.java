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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;

/**
 * Statically determines which BTs a template source shows. Only a placeholder that prints a value
 * counts: {@code {{label:BT-x}}} prints the name of the term and {@code {{?BT-x}}} prints nothing.
 * The result depends on the template alone, not on the invoice or the locale.
 *
 * @author Philip Helger
 */
public final class PhloxTemplateCoverage
{
  private static final Pattern TAG = Pattern.compile ("\\{\\{(.*?)\\}\\}");
  private static final Pattern BT_PREFIX = Pattern.compile ("BT-\\d+(?:-\\d+)?");

  // The derived name "address" prints all parts of an address except for the country
  private static final String ADDRESS = "address";
  private static final Map <String, List <String>> ADDRESS_BTS = Map.of (
    "BG-5", List.of ("BT-35", "BT-36", "BT-162", "BT-37", "BT-38", "BT-39"),
    "BG-8", List.of ("BT-50", "BT-51", "BT-163", "BT-52", "BT-53", "BT-54"),
    "BG-12", List.of ("BT-64", "BT-65", "BT-164", "BT-66", "BT-67", "BT-68"),
    "BG-15", List.of ("BT-75", "BT-76", "BT-165", "BT-77", "BT-78", "BT-79"),
    "BG-38", List.of ("BT-203", "BT-204", "BT-205", "BT-206", "BT-207", "BT-208"));

  private static final String AMOUNT_MODIFIER_PREFIX = "amount:";
  private static final String PARENT_PREFIX = "..";

  private PhloxTemplateCoverage ()
  {}

  @Nullable
  private static String _getLastAddressBG (@NonNull final List <String> aPath)
  {
    for (int i = aPath.size () - 1; i >= 0; --i)
      if (ADDRESS_BTS.containsKey (aPath.get (i)))
        return aPath.get (i);
    return null;
  }

  /**
   * @return The BG of an address: the innermost one of the path, otherwise the innermost one of the
   *         enclosing sections. May be <code>null</code>.
   */
  @Nullable
  private static String _getEnclosingBG (@NonNull final List <String> aPath,
                                         @NonNull final Deque <List <String>> aSections)
  {
    String ret = _getLastAddressBG (aPath);
    if (ret == null)
      for (final List <String> aSection : aSections)
      {
        ret = _getLastAddressBG (aSection);
        if (ret != null)
          break;
      }
    return ret;
  }

  private static void _addShownBTs (@NonNull final String sTag,
                                    @NonNull final Deque <List <String>> aSections,
                                    @NonNull final ICommonsSet <String> aShown)
  {
    final List <String> aParts = StringHelper.getExploded ('|', sTag);
    String sHead = aParts.get (0).trim ();
    if (sHead.startsWith (PARENT_PREFIX))
      sHead = sHead.substring (PARENT_PREFIX.length ());

    final List <String> aPath = StringHelper.getExploded ('.', sHead);
    for (final String sSegment : aPath)
    {
      final Matcher aMatcher = BT_PREFIX.matcher (sSegment);
      // A prefix, because the derived "BT-149-if-not-one" prints BT-149
      if (aMatcher.lookingAt ())
        aShown.add (aMatcher.group ());
    }

    if (ADDRESS.equals (aPath.get (aPath.size () - 1)))
    {
      final String sBG = _getEnclosingBG (aPath, aSections);
      if (sBG != null)
        aShown.addAll (ADDRESS_BTS.get (sBG));
    }

    // "amount:BT-6" prints the currency of BT-6 with the amount
    for (final String sPart : aParts.subList (1, aParts.size ()))
    {
      final String sModifier = sPart.trim ();
      if (sModifier.startsWith (AMOUNT_MODIFIER_PREFIX))
        aShown.add (sModifier.substring (AMOUNT_MODIFIER_PREFIX.length ()));
    }
  }

  @NonNull
  private static ICommonsSet <String> _getShownBTs (@NonNull final String sSource)
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    // The model paths of the open sections, innermost first
    final Deque <List <String>> aSections = new ArrayDeque <> ();
    final Matcher aMatcher = TAG.matcher (PhloxInvoiceMustacheContext.stripComments (sSource));
    while (aMatcher.find ())
    {
      final String sTag = aMatcher.group (1).trim ();
      if (sTag.isEmpty ())
        continue;
      switch (sTag.charAt (0))
      {
        case '#' -> aSections.push (StringHelper.getExploded ('.', sTag.substring (1).trim ()));
        // These do not change the context of the enclosed placeholders
        case '?', '^' -> aSections.push (List.of ());
        case '/' -> aSections.poll ();
        default -> _addShownBTs (sTag, aSections, ret);
      }
    }
    return ret;
  }

  /**
   * Get the BTs that a template does not show.
   *
   * @param sSource
   *        The raw {@code .ptpl} source. Never {@code null}.
   * @return The names of all BTs of {@link PhloxModelRegistry#getAllBTs()} that the template does
   *         not show, in numerical order. Never {@code null}.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <String> getMissingBTs (@NonNull final String sSource)
  {
    final ICommonsList <String> ret = PhloxModelRegistry.getAllBTs ();
    ret.removeAll (_getShownBTs (sSource));
    return ret;
  }
}
