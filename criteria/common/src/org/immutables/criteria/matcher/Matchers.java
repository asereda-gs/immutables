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

package org.immutables.criteria.matcher;

import org.immutables.criteria.Criterias;
import org.immutables.criteria.Criterion;
import org.immutables.criteria.expression.Expression;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Util functions for matchers
 */
public final class Matchers {

  private Matchers() {}

  static List<Expression> concatFilters(Expression existing, Criterion<?> first, Criterion<?> ... rest) {
    Stream<Expression> restStream = Stream.concat(Stream.of(first), Arrays.stream(rest))
            .map(Criterias::toQuery)
            .filter(q -> q.filter().isPresent())
            .map(q -> q.filter().get());

    return Stream.concat(Stream.of(existing), restStream)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

  }

  /**
   * Extracts criteria context from an arbitrary object.
   * @see HasContext
   */
  static CriteriaContext extract(Object object) {
    Objects.requireNonNull(object, "object");

    if (object instanceof HasContext) {
      return ((HasContext) object).context();
    }

    throw new IllegalArgumentException(String.format("%s does not implement %s", object.getClass().getName(),
            HasContext.class.getSimpleName()));
  }

  static <C> UnaryOperator<Expression> toInnerExpression(CriteriaContext context, UnaryOperator<C> expr) {
    return expression -> {
      final CriteriaContext newContext = context.newChild();
      final C initial = (C) newContext.factory().createRoot();
      final C changed = expr.apply(initial);
      return Matchers.extract(changed).query().filter().orElseThrow(() -> new IllegalStateException("filter should be set"));
    };
  }

}
