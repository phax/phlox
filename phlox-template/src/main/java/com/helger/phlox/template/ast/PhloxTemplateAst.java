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

import java.util.Map;

import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxInvoice;

/**
 * Output of stage 2 (Parse) — the commonmark {@link Node} tree plus two sidecar maps:
 * the per-block CSS class attached by {@code {{class:name}}} sentinels, and the resolved
 * image payload for each {@link Image} node. Carries the source invoice and the active
 * render context so emitters can read configuration without re-resolving anything.
 * <p>
 * Immutable after construction. The {@link Node} graph itself is mutable in commonmark-java
 * — callers must not mutate it after the AST is built.
 *
 * @author Philip Helger
 */
public final class PhloxTemplateAst
{
  private final Node m_aRoot;
  private final Map <Node, String> m_aBlockClassMap;
  private final Map <Image, PhloxResolvedImage> m_aImageMap;
  private final PhloxInvoice m_aInvoice;
  private final AbstractRenderContext <?> m_aContext;

  /**
   * Construct the AST artifact. Called from
   * {@link com.helger.phlox.template.PhloxLocalizedTemplate#renderAst} after stages 1 + 2 + 2a
   * have completed; not normally constructed by external callers.
   *
   * @param aRoot
   *        Post-processed commonmark root.
   * @param aBlockClassMap
   *        Block → class name sidecar populated by the {@code {{class:…}}} post-pass.
   * @param aImageMap
   *        Image node → resolved payload sidecar populated by the image URL post-pass.
   * @param aInvoice
   *        Source invoice.
   * @param aContext
   *        Render context active during this render.
   */
  public PhloxTemplateAst (@NonNull final Node aRoot,
                           @NonNull final Map <Node, String> aBlockClassMap,
                           @NonNull final Map <Image, PhloxResolvedImage> aImageMap,
                           @NonNull final PhloxInvoice aInvoice,
                           @NonNull final AbstractRenderContext <?> aContext)
  {
    m_aRoot = aRoot;
    m_aBlockClassMap = aBlockClassMap;
    m_aImageMap = aImageMap;
    m_aInvoice = aInvoice;
    m_aContext = aContext;
  }

  /**
   * Get the post-processed commonmark document tree.
   *
   * @return The post-processed commonmark document tree.
   */
  @NonNull
  public Node getRoot ()
  {
    return m_aRoot;
  }

  /**
   * Get the CSS classes attached to a block by the preceding {@code {{class:…}}} sentinels.
   *
   * @param aBlock
   *        The block node to look up. Never {@code null}.
   * @return The CSS classes separated by a space, or {@code null} if no sentinel was attached to the
   *         block.
   */
  @Nullable
  public String getBlockClass (@NonNull final Node aBlock)
  {
    return m_aBlockClassMap.get (aBlock);
  }

  /**
   * Get the single CSS classes attached to a block by the preceding {@code {{class:…}}} sentinels.
   *
   * @param aBlock
   *        The block node to look up. Never {@code null}.
   * @return The CSS classes in the order of the template. Empty if no sentinel was attached to the
   *         block. Never {@code null}.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getBlockClasses (@NonNull final Node aBlock)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    final String sClasses = m_aBlockClassMap.get (aBlock);
    if (sClasses != null)
      for (final String sClass : StringHelper.getExploded (' ', sClasses))
        if (!sClass.isEmpty ())
          ret.add (sClass);
    return ret;
  }

  /**
   * Get the resolved image payload for an {@link Image} node.
   *
   * @param aImage
   *        The image node to look up. Never {@code null}.
   * @return The resolved image payload or {@code null} when the node has no binary backing.
   */
  @Nullable
  public PhloxResolvedImage getResolvedImage (@NonNull final Image aImage)
  {
    return m_aImageMap.get (aImage);
  }

  /**
   * Get the {@link PhloxInvoice} the template was rendered from.
   *
   * @return The {@link PhloxInvoice} the template was rendered from.
   */
  @NonNull
  public PhloxInvoice getInvoice ()
  {
    return m_aInvoice;
  }

  /**
   * Get the active render context.
   *
   * @return The active render context.
   */
  @NonNull
  public AbstractRenderContext <?> getContext ()
  {
    return m_aContext;
  }
}
