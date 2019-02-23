package org.immutables.criteria.constraints;

import javax.annotation.Nullable;

public interface Expression<T> {

  @Nullable
  <R> R accept(Constraints.Visitor<R> visitor);

}