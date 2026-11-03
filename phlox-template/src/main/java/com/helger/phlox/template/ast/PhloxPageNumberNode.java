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
package com.helger.phlox.template.ast;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Visitor;

/**
 * Custom commonmark inline node produced by the post-pass for the template tags
 * <code>{{page}}</code> (the number of the current page) and <code>{{pages}}</code> (the number
 * of pages). It only exists in a {@link PhloxPageMarginNode}, because only there the page is known.
 *
 * @author Philip Helger
 */
public final class PhloxPageNumberNode extends CustomNode
{
  private final boolean m_bPageCount;

  /**
   * Create a new page number node.
   *
   * @param bPageCount
   *        <code>true</code> for the number of pages, <code>false</code> for the number of the
   *        current page.
   */
  public PhloxPageNumberNode (final boolean bPageCount)
  {
    m_bPageCount = bPageCount;
  }

  /**
   * @return <code>true</code> for the number of pages, <code>false</code> for the number of the
   *         current page.
   */
  public boolean isPageCount ()
  {
    return m_bPageCount;
  }

  /**
   * Default commonmark dispatch - emitters instance-check for this type.
   */
  @Override
  public void accept (final Visitor aVisitor)
  {
    aVisitor.visit (this);
  }
}
