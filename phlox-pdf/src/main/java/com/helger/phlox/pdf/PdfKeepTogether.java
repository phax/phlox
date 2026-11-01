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
package com.helger.phlox.pdf;

import org.jspecify.annotations.NonNull;

import com.helger.collection.commons.ICommonsList;
import com.helger.pdflayout.element.hbox.PLHBox;
import com.helger.pdflayout.element.table.PLTableRow;
import com.helger.pdflayout.element.vbox.PLVBox;
import com.helger.pdflayout.render.PreparationContext;
import com.helger.pdflayout.richtext.element.PLRichText;
import com.helger.pdflayout.richtext.run.PLRichTextRun;
import com.helger.pdflayout.spec.SizeSpec;

/**
 * Elements that are moved to the next page as a whole, unless they are higher than a page.
 * <p>
 * ph-pdf-layout cannot lay out an element that is not vertically splittable and higher than a page:
 * the rendering fails with an internal error. How high an element is depends on its text, and that
 * comes from the invoice, so one long value must not make the whole PDF fail. The height is only
 * known once an element is prepared, so these elements decide then if they can be split.
 *
 * @author Philip Helger
 */
final class PdfKeepTogether
{
  /** A stacked group. */
  static final class VBox extends PLVBox
  {
    private final float m_fMaxHeight;

    VBox (final float fMaxHeight)
    {
      m_fMaxHeight = fMaxHeight;
      setVertSplittable (false);
    }

    @Override
    protected SizeSpec onPrepare (@NonNull final PreparationContext aCtx)
    {
      final SizeSpec ret = super.onPrepare (aCtx);
      setVertSplittable (ret.getHeight () > m_fMaxHeight);
      return ret;
    }
  }

  /** A row of a template. */
  static final class HBox extends PLHBox
  {
    private final float m_fMaxHeight;

    HBox (final float fMaxHeight)
    {
      m_fMaxHeight = fMaxHeight;
      setVertSplittable (false);
    }

    @Override
    protected SizeSpec onPrepare (@NonNull final PreparationContext aCtx)
    {
      final SizeSpec ret = super.onPrepare (aCtx);
      setVertSplittable (ret.getHeight () > m_fMaxHeight);
      return ret;
    }
  }

  /** A row of a table. */
  static final class TableRow extends PLTableRow
  {
    private final float m_fMaxHeight;

    TableRow (final float fMaxHeight)
    {
      m_fMaxHeight = fMaxHeight;
      setVertSplittable (false);
    }

    @Override
    protected SizeSpec onPrepare (@NonNull final PreparationContext aCtx)
    {
      final SizeSpec ret = super.onPrepare (aCtx);
      setVertSplittable (ret.getHeight () > m_fMaxHeight);
      return ret;
    }
  }

  /** The text of a heading. */
  static final class RichText extends PLRichText
  {
    private final float m_fMaxHeight;

    RichText (@NonNull final ICommonsList <PLRichTextRun> aRuns, final float fMaxHeight)
    {
      super (aRuns);
      m_fMaxHeight = fMaxHeight;
      setVertSplittable (false);
    }

    @Override
    protected SizeSpec onPrepare (@NonNull final PreparationContext aCtx)
    {
      final SizeSpec ret = super.onPrepare (aCtx);
      setVertSplittable (ret.getHeight () > m_fMaxHeight);
      return ret;
    }
  }

  private PdfKeepTogether ()
  {}
}
