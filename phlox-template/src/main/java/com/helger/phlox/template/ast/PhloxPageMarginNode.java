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

import org.commonmark.node.CustomBlock;
import org.commonmark.node.Visitor;

/**
 * Custom commonmark block node produced by the post-pass for the template tags
 * <code>{{header}}</code> or <code>{{footer}}</code> up to the matching <code>{{end}}</code>: the
 * page header or the page footer. The blocks in between become the children of this node. They are
 * not part of the flow of the document: the PDF shows them at the top or the bottom of every page,
 * the HTML of every printed page, and the plain text not at all. The block class of the node is
 * <code>page-header</code> or <code>page-footer</code>. A template has at most one of each, at the
 * top level.
 *
 * @author Philip Helger
 */
public final class PhloxPageMarginNode extends CustomBlock
{
  /** The block class of every page header. */
  public static final String BLOCK_CLASS_HEADER = "page-header";
  /** The block class of every page footer. */
  public static final String BLOCK_CLASS_FOOTER = "page-footer";

  private final boolean m_bHeader;

  /**
   * Create a new page header or footer node.
   *
   * @param bHeader
   *        <code>true</code> for the page header, <code>false</code> for the page footer.
   */
  public PhloxPageMarginNode (final boolean bHeader)
  {
    m_bHeader = bHeader;
  }

  /**
   * @return <code>true</code> for the page header, <code>false</code> for the page footer.
   */
  public boolean isHeader ()
  {
    return m_bHeader;
  }

  /**
   * @return The block class of the node: {@link #BLOCK_CLASS_HEADER} or {@link #BLOCK_CLASS_FOOTER}.
   */
  public String getBlockClass ()
  {
    return m_bHeader ? BLOCK_CLASS_HEADER : BLOCK_CLASS_FOOTER;
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
