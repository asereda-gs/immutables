package org.immutables.criteria.constraints;

import java.util.List;

public interface Operation<T> extends Expression<T> {

  /**
   * Get the argument with the given index
   *
   * @param index zero based index of expression
   * @return expression at index
   */
  Expression<?> getArg(int index);

  /**
   * Get the arguments of this operation
   *
   * @return arguments
   */
  List<Expression<?>> getArgs();

  /**
   * Get the operator symbol for this operation
   *
   * @return operator
   */
  Operator getOperator();

}