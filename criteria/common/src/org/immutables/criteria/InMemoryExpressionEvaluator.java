package org.immutables.criteria;

import com.google.common.base.Preconditions;
import org.immutables.criteria.constraints.Call;
import org.immutables.criteria.constraints.Expression;
import org.immutables.criteria.constraints.ExpressionVisitor;
import org.immutables.criteria.constraints.Expressions;
import org.immutables.criteria.constraints.Literal;
import org.immutables.criteria.constraints.Operator;
import org.immutables.criteria.constraints.Operators;
import org.immutables.criteria.constraints.Path;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Evaluator (predicate) based on reflection. Uses expression visitor API to construct the predicate.
 *
 * <p>Probably most useful in testing scenarios</p>
 */
public class InMemoryExpressionEvaluator<T> implements Predicate<T> {

  private final Expression<T> expression;

  private InMemoryExpressionEvaluator(Expression<T> expression) {
    this.expression = Objects.requireNonNull(expression, "expression");
  }

  /**
   * Factory method to create evaluator instance
   */
  public static <T> Predicate<T> of(Expression<T> expression) {
    if (Expressions.isNil(expression)) {
      // always true
      return instance -> true;
    }

    return new InMemoryExpressionEvaluator<>(expression);
  }

  @Override
  public boolean test(T instance) {
    Objects.requireNonNull(instance, "instance");
    final LocalVisitor visitor = new LocalVisitor(instance);
    return Boolean.TRUE.equals(expression.accept(visitor));
  }

  private static class LocalVisitor implements ExpressionVisitor<Object> {

    private final ValueExtractor<Object> extractor;

    private LocalVisitor(Object instance) {
      this.extractor = new ReflectionFieldExtractor<>(instance);
    }

    @Override
    public Object visit(Call<?> call) {
      final Operator op = call.getOperator();
      final List<Expression<?>> args = call.getArguments();

      if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
        Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
        final Object left = args.get(0).accept(this);
        final Object right = args.get(1).accept(this);

        final boolean equals = Objects.equals(left, right);
        return (op == Operators.EQUAL) == equals;
      }

      if (op == Operators.IN || op == Operators.NOT_IN) {
        Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
        final Object left = args.get(0).accept(this);
        @SuppressWarnings("unchecked")
        final Iterable<Object> right = (Iterable<Object>) args.get(1).accept(this);
        Preconditions.checkNotNull(right, "not expected to be null %s", args.get(1));
        final Stream<Object> stream = StreamSupport.stream(right.spliterator(), false);

        return op == Operators.IN ? stream.anyMatch(r -> Objects.equals(left, r)) : stream.noneMatch(r -> Objects.equals(left, r));
      }

      if (op == Operators.IS_ABSENT || op == Operators.IS_PRESENT) {
        Preconditions.checkArgument(args.size() == 1, "Size should be 1 for %s but was %s", op, args.size());
        final Object left = args.get(0).accept(this);

        if (left instanceof java.util.Optional) {
          Optional<?> opt = (java.util.Optional<?>) left;
          return (op == Operators.IS_ABSENT) != opt.isPresent();
        }

        if (left instanceof com.google.common.base.Optional) {
          // guava Optional
          com.google.common.base.Optional<?> opt = (com.google.common.base.Optional<?>) left;
          return (op == Operators.IS_ABSENT) != opt.isPresent();
        }


        return (op == Operators.IS_ABSENT) ? Objects.isNull(left) : Objects.nonNull(left);
      }

      if (op == Operators.AND || op == Operators.OR) {
        final Stream<Object> stream = args.stream().map(a -> a.accept(this));
        return op == Operators.AND ? stream.noneMatch(Boolean.FALSE::equals) : stream.anyMatch(Boolean.TRUE::equals);
      }

      // comparables
      if (Arrays.asList(Operators.GREATER_THAN, Operators.GREATER_THAN_OR_EQUAL,
              Operators.LESS_THAN, Operators.LESS_THAN_OR_EQUAL).contains(op)) {

        Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());

        @SuppressWarnings("unchecked")
        Comparable<Object> left = (Comparable<Object>) args.get(0).accept(this);
        Preconditions.checkNotNull(left, "left");
        @SuppressWarnings("unchecked")
        Comparable<Object> right = (Comparable<Object>) args.get(1).accept(this);
        Preconditions.checkNotNull(right, "right");

        final int compare = left.compareTo(right);

        if (op == Operators.GREATER_THAN) {
          return compare > 0;
        } else if (op == Operators.GREATER_THAN_OR_EQUAL) {
          return compare >= 0;
        } else if (op == Operators.LESS_THAN) {
          return compare < 0;
        } else if (op == Operators.LESS_THAN_OR_EQUAL) {
          return compare <= 0;
        }
      }

      throw new UnsupportedOperationException("Don't know how to handle " + op);
    }

    @Override
    public Object visit(Literal<?> literal) {
      return literal.value();
    }

    @Override
    public Object visit(Path<?> path) {
      return extractor.extract(path.path());
    }
  }

  private interface ValueExtractor<T> {
    @Nullable
    Object extract(String name);
  }

  private static class ReflectionFieldExtractor<T> implements ValueExtractor<T> {
    private final T object;
    private final Class<T> klass;

    private ReflectionFieldExtractor(T object) {
      this.object = object;
      this.klass = (Class<T>) object.getClass();
    }

    @Nullable
    @Override
    public Object extract(String name) {
      try {
        // TODO caching
        final Field field = klass.getDeclaredField(name);
        if (!field.isAccessible()) {
          field.setAccessible(true);
        }
        return field.get(object);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
