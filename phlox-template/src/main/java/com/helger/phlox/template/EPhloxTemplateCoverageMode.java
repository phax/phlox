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

/**
 * What to do when a template that is loaded does not show all BTs of the model. An invoice that
 * carries such a BT is rendered without it, so the output is incomplete.
 *
 * @author Philip Helger
 */
public enum EPhloxTemplateCoverageMode
{
  /** Log a warning that lists the missing BTs and use the template anyway. */
  WARN,
  /** Refuse the template with a {@link PhloxTemplateException}. */
  REJECT
}
