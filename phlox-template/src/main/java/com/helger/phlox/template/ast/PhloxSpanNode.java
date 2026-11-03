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
import org.jspecify.annotations.Nullable;

/**
 * Custom commonmark inline node produced by the post-pass for the template tags
 * <code>{{color:#rrggbb}}</code> up to <code>{{endcolor}}</code> and <code>{{bg:#rrggbb}}</code> up
 * to <code>{{endbg}}</code>. The inline nodes in between become the children of this node.
 *
 * @author Philip Helger
 */
public final class PhloxSpanNode extends CustomNode
{
  private final String m_sTextColor;
  private final String m_sBackgroundColor;

  /**
   * Create a new span.
   *
   * @param sTextColor
   *        The text colour as six hex digits without a leading hash. May be <code>null</code>.
   * @param sBackgroundColor
   *        The background colour of the text as six hex digits without a leading hash. May be
   *        <code>null</code>.
   */
  public PhloxSpanNode (@Nullable final String sTextColor, @Nullable final String sBackgroundColor)
  {
    m_sTextColor = sTextColor;
    m_sBackgroundColor = sBackgroundColor;
  }

  /**
   * @return The text colour as six hex digits without a leading hash, or <code>null</code> to keep
   *         the colour of the surrounding text.
   */
  @Nullable
  public String getTextColor ()
  {
    return m_sTextColor;
  }

  /**
   * @return The background colour of the text as six hex digits without a leading hash, or
   *         <code>null</code> to keep the background of the surrounding text.
   */
  @Nullable
  public String getBackgroundColor ()
  {
    return m_sBackgroundColor;
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
