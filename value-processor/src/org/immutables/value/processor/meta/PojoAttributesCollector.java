/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.value.processor.meta;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.immutables.value.processor.encode.Instantiator;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Collects attributes by searching for getters and setters in a class definition.
 *
 * For each {@code getFoo} / {@code setFoo} method pair create attribute {@code foo}.
 * @see AccessorAttributesCollector
 */
final class PojoAttributesCollector {

  private final Proto.Protoclass protoclass;
  private final ValueType type;
  private final Reporter reporter;
  private final ProcessingEnvironment processing;
  private final Styles styles;
  public final List<ValueAttribute> attributes = Lists.newArrayList();

  PojoAttributesCollector(Proto.Protoclass protoclass, ValueType type) {
    this.protoclass = Preconditions.checkNotNull(protoclass, "protoclass");
    this.type = Preconditions.checkNotNull(type, "type");
    this.reporter = protoclass.report();
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*"));
    this.processing = protoclass.processing();
  }

  private TypeElement getTypeElement() {
    return (TypeElement) type.element;
  }

  void collect() {
    TypeElement originalType = CachingElements.getDelegate(getTypeElement());

    Map<String, ExecutableElement> methods = Maps.newLinkedHashMap();
    for (TypeElement element : traverse(originalType, new LinkedHashSet<TypeElement>())) {
      for (ExecutableElement method: ElementFilter.methodsIn(element.getEnclosedElements())) {
        if (isSetterOrGetter(method)) {
          methods.put(method.getSimpleName().toString(), method);
        }
      }
    }

    // check that both getters and setters exists: getFoo and setFoo
    for (String name: methods.keySet()) {
      Styles.UsingName.AttributeNames names = styles.forAccessor(name);
      if (methods.containsKey(names.get) && methods.containsKey(names.set())) {
        addAttribute(methods.get(name));
      }
    }

    Instantiator encodingInstantiator = protoclass.encodingInstantiator();
    @Nullable Instantiator.InstantiationCreator instantiationCreator =
            encodingInstantiator.creatorFor((Parameterizable) type.element);

    for (ValueAttribute attribute : attributes) {
      attribute.initAndValidate(instantiationCreator);
    }

    type.attributes.addAll(attributes);
  }

  private void addAttribute(ExecutableElement getter) {
    Styles.UsingName.AttributeNames names = styles.forAccessor(getter.getSimpleName().toString());

    ValueAttribute attribute = new ValueAttribute();
    attribute.reporter = reporter;
    attribute.returnType = getter.getReturnType();
    attribute.names = names;
    attribute.element = getter;
    attribute.containingType = type;
    attribute.isGenerateAbstract = true;
    attributes.add(attribute);
  }

  private static boolean isSetterOrGetter(ExecutableElement element) {
    String name = element.getSimpleName().toString();

    if (name.startsWith("get") || name.startsWith("is")) {
      return element.getParameters().isEmpty() && element.getReturnType().getKind() != TypeKind.VOID;
    }

    if (name.startsWith("set")) {
      return element.getParameters().size() == 1 && element.getReturnType().getKind() == TypeKind.VOID;
    }

    return false;
  }


  private static <C extends Collection<TypeElement>> C traverse(@Nullable TypeElement element, C collection) {

    if (element == null || isJavaLangObject(element)) {
      return collection;
    }
    for (TypeMirror implementedInterface : element.getInterfaces()) {
      traverse(toElement(implementedInterface), collection);
    }
    if (element.getKind().isClass()) {
      traverse(toElement(element.getSuperclass()), collection);
    }

    collection.add(element);
    return collection;
  }

  private static boolean isJavaLangObject(TypeElement element) {
    return element.getQualifiedName().contentEquals(Object.class.getName());
  }

  private static TypeElement toElement(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      return (TypeElement) ((DeclaredType) type).asElement();
    }
    return null;
  }

}
