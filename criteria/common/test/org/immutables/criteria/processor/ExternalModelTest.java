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

package org.immutables.criteria.processor;

import org.immutables.criteria.Criteria;
import org.immutables.value.processor.meta.ProcessorRule;
import org.immutables.value.processor.meta.ValueType;
import org.junit.Rule;
import org.junit.Test;

import static org.immutables.check.Checkers.check;

public class ExternalModelTest {

  @Rule // TODO migrate to JUnit5 Extension
  public final ProcessorRule rule = new ProcessorRule();

  @Test
  public void name() {
    ValueType valueType = rule.value(Model1.class);
    check(valueType.attributes).notEmpty();
    check(valueType.allMarshalingAttributes()).notEmpty();

    Model1Criteria model1 = Model1Criteria.model1;
    model1.base.is("aaa");

  }

  @ProcessorRule.TestImmutable
  @Criteria
  @Criteria.Repository
  static class Model1  extends BaseModel {

    private int foo;

    public int getFoo() {
      return foo;
    }

    public void setFoo(int foo) {
      this.foo = foo;
    }
  }

  static abstract class BaseModel {
    private String base;

    public String getBase() {
      return base;
    }

    public void setBase(String base) {
      this.base = base;
    }
  }
}
