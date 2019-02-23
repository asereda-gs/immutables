package org.immutables.criteria.constraints;

public enum Operators implements Operator {

  EQ,
  NE,

  IS_PRESENT,
  IS_ABSENT,

  BETWEEN,

  GREATER_THAN,
  LESS_THAN;

}
