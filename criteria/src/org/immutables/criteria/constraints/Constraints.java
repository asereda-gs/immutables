/*
   Copyright 2013-2018 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.criteria.constraints;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for constraints
 */
public final class Constraints {

  private Constraints() {}

  /**
   * This "host" could accepts {@link Visitor}s. Allows evaluation of criterias.
   */
  public interface Visitable {
    <R> R accept(Visitor<R> visitor);
  }

  public interface Visitor<V> {

    V visit(String name, Operation<?> operation);

    V disjunction();
  }

  public static Constraint isEqualTo(Constraint tail, String name, @Nullable Object value) {
    return new ConsConstraint(tail, name, )
  }

  public static Constraint isNotEqualTo(Constraint tail, String name, @Nullable Object value) {
    return new EqualToConstraint(tail, name, true, value);
  }


  public static Constraint in(Constraint tail, String name, Iterable<?> values) {
    return new InConstraint(tail, name,false, values);
  }

  public static Constraint notIn(Constraint tail, String name, Iterable<?> values) {
    return new InConstraint(tail, name, true, values);
  }


  private static final class DisjunctionConstraint extends Constraint {
    private final Constraint tail;

    DisjunctionConstraint(Constraint tail) {
      this.tail = tail;
    }

    @Override
    public <R> R accept(Visitor<R> visitor) {
      throw new UnsupportedOperationException();
    }
  }

  private static final Constraint NIL = new Constraint() {
    @Override
    public <R> R accept(Visitor<R> visitor) {
      return visitor.disjunction();
    }

    @Override
    public Constraint visit(String name, Operation<?> operation) {
      return this;
    }
  };

  public static Constraint nilConstraint() {
    return NIL;
  }


  public abstract static class Constraint implements Visitor<Constraint>, Visitable {

    @Override
    public Constraint disjunction() {
      return new DisjunctionConstraint(this);
    }

  }

  private abstract static class ConsConstraint extends Constraint  {
    final Constraint tail;
    final String name;
    final Operation<?> operation;

    ConsConstraint(
            Constraint tail,
            String name,
            Operation<?> operation) {
      this.tail = checkNotNull(tail, "tail");
      this.name = checkNotNull(name, "name");
      this.operation = checkNotNull(operation, "operation");
    }

  }

}
