/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.processor;

import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BuilderMethodModel;
import com.facebook.litho.specmodels.model.DefaultLayoutSpecGenerator;
import com.facebook.litho.specmodels.model.DelegateMethodDescriptions;
import com.facebook.litho.specmodels.model.DependencyInjectionHelper;
import com.facebook.litho.specmodels.model.LayoutSpecGenerator;
import com.facebook.litho.specmodels.model.LayoutSpecModel;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/** Factory for creating {@link LayoutSpecModel}s. */
public class LayoutSpecModelFactory implements SpecModelFactory {
  private static final List<Class<? extends Annotation>> INTER_STAGE_INPUT_ANNOTATIONS =
      new ArrayList<>();
  private static final List<Class<? extends Annotation>> DELEGATE_METHOD_ANNOTATIONS =
      new ArrayList<>();
  static {
    DELEGATE_METHOD_ANNOTATIONS.addAll(
        DelegateMethodDescriptions.LAYOUT_SPEC_DELEGATE_METHODS_MAP.keySet());
    DELEGATE_METHOD_ANNOTATIONS.add(OnCreateTreeProp.class);
    DELEGATE_METHOD_ANNOTATIONS.add(ShouldUpdate.class);
  }

  private final List<Class<? extends Annotation>> mLayoutSpecDelegateMethodAnnotations;
  private final LayoutSpecGenerator mLayoutSpecGenerator;

  public LayoutSpecModelFactory() {
    this(DELEGATE_METHOD_ANNOTATIONS, new DefaultLayoutSpecGenerator());
  }

  public LayoutSpecModelFactory(
      List<Class<? extends Annotation>> layoutSpecDelegateMethodAnnotations,
      LayoutSpecGenerator layoutSpecGenerator) {

    mLayoutSpecDelegateMethodAnnotations = layoutSpecDelegateMethodAnnotations;
    mLayoutSpecGenerator = layoutSpecGenerator;
  }

  @Override
  public Set<Element> extract(RoundEnvironment roundEnvironment) {
    return (Set<Element>) roundEnvironment.getElementsAnnotatedWith(LayoutSpec.class);
  }

  /**
   * Create a {@link LayoutSpecModel} from the given {@link TypeElement} and an optional {@link
   * DependencyInjectionHelper}.
   */
  @Override
  public LayoutSpecModel create(
      Elements elements,
      TypeElement element,
      @Nullable DependencyInjectionHelper dependencyInjectionHelper) {
    return new LayoutSpecModel(
        element.getQualifiedName().toString(),
        element.getAnnotation(LayoutSpec.class).value(),
        DelegateMethodExtractor.getDelegateMethods(
            element,
            mLayoutSpecDelegateMethodAnnotations,
            INTER_STAGE_INPUT_ANNOTATIONS,
            ImmutableList.<Class<? extends Annotation>>of(ShouldUpdate.class)),
        EventMethodExtractor.getOnEventMethods(elements, element, INTER_STAGE_INPUT_ANNOTATIONS),
        UpdateStateMethodExtractor.getOnUpdateStateMethods(element, INTER_STAGE_INPUT_ANNOTATIONS),
        ImmutableList.copyOf(TypeVariablesExtractor.getTypeVariables(element)),
        ImmutableList.copyOf(PropDefaultsExtractor.getPropDefaults(element)),
        EventDeclarationsExtractor.getEventDeclarations(elements, element, LayoutSpec.class),
        ImmutableList.<BuilderMethodModel>of(),
        JavadocExtractor.getClassJavadoc(elements, element),
        JavadocExtractor.getPropJavadocs(elements, element),
        element.getAnnotation(LayoutSpec.class).isPublic(),
        dependencyInjectionHelper,
        element.getAnnotation(LayoutSpec.class).isPureRender(),
        element,
        mLayoutSpecGenerator);
  }
}
