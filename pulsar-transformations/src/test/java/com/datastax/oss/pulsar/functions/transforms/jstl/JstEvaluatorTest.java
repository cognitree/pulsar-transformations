/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.pulsar.functions.transforms.jstl;

import static org.testng.AssertJUnit.assertEquals;

import com.datastax.oss.pulsar.functions.transforms.TransformContext;
import com.datastax.oss.pulsar.functions.transforms.Utils;
import de.odysseus.el.tree.TreeBuilderException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.apache.pulsar.client.api.Schema;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JstEvaluatorTest {

  @Test(
    dataProvider = "methodInvocationExpressionProvider",
    expectedExceptions = TreeBuilderException.class
  )
  void testMethodInvocationsDisabled(String expression, TransformContext context) {
    new JstlEvaluator<>(String.format("${%s}", expression), String.class).evaluate(context);
  }

  @Test(dataProvider = "functionExpressionProvider")
  void testFunctions(String expression, TransformContext context, Object expectedValue) {
    assertEquals(
        expectedValue,
        new JstlEvaluator<>(String.format("${%s}", expression), Object.class).evaluate(context));
  }

  @Test
  void testPrimitiveValue() {
    TransformContext primitiveStringContext =
        Utils.createContextWithPrimitiveRecord(Schema.STRING, "test-message", "");

    String value = new JstlEvaluator<>("${value}", String.class).evaluate(primitiveStringContext);

    assertEquals("test-message", value);
  }

  @Test
  void testNowFunction() {
    TransformContext primitiveStringContext =
        Utils.createContextWithPrimitiveRecord(Schema.STRING, "test-message", "");

    long expectedMillis = 123L;
    Clock clock = Clock.fixed(Instant.ofEpochMilli(expectedMillis), ZoneOffset.UTC);
    JstlFunctions.setClock(clock);

    long actualMillis =
        new JstlEvaluator<>("${fn:now()}", long.class).evaluate(primitiveStringContext);

    assertEquals(expectedMillis, actualMillis);
  }

  @Test
  void testDateAddFunctionsNow() {
    TransformContext primitiveStringContext =
        Utils.createContextWithPrimitiveRecord(Schema.STRING, "test-message", "");

    long nowMillis = 5000L;
    long millisToAdd = -3333L * 1000L;
    Clock clock = Clock.fixed(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC);
    JstlFunctions.setClock(clock);
    long actualMillis =
        new JstlEvaluator<>("${fn:dateadd(fn:now(), -3333, 'seconds')}", long.class)
            .evaluate(primitiveStringContext);

    assertEquals(nowMillis + millisToAdd, actualMillis);
  }

  /** @return {"expression", "transform context"} */
  @DataProvider(name = "methodInvocationExpressionProvider")
  public static Object[][] methodInvocationExpressionProvider() {
    TransformContext primitiveStringContext =
        Utils.createContextWithPrimitiveRecord(Schema.STRING, "test-message", "header-key");

    return new Object[][] {
      {"value.contains('test')", primitiveStringContext},
      {"value.toUpperCase() == 'TEST-MESSAGE'", primitiveStringContext},
      {"value.toUpperCase().toLowerCase() == 'test-message'", primitiveStringContext},
      {"value.substring(0, 4) == 'test'", primitiveStringContext},
      {"value.contains('random')", primitiveStringContext},
    };
  }

  /** @return {"expression", "context", "expected value"} */
  @DataProvider(name = "functionExpressionProvider")
  public static Object[][] functionExpressionProvider() {
    TransformContext primitiveBytesContext =
        Utils.createContextWithPrimitiveRecord(
            Schema.BYTES, "Test-Message ".getBytes(StandardCharsets.UTF_8), "");
    TransformContext primitiveInstantContext =
        Utils.createContextWithPrimitiveRecord(
            Schema.INSTANT, Instant.parse("2017-01-02T00:01:02Z"), "");
    TransformContext chronoUnitBytesContext =
        Utils.createContextWithPrimitiveRecord(
            Schema.BYTES, "millis".getBytes(StandardCharsets.UTF_8), "");
    long millis = Instant.parse("2017-01-02T00:01:02Z").toEpochMilli();
    return new Object[][] {
      {"fn:uppercase('test')", primitiveBytesContext, "TEST"},
      {"fn:uppercase(value) == 'TEST-MESSAGE '", primitiveBytesContext, true},
      {"fn:uppercase(null)", primitiveBytesContext, null},
      {"fn:lowercase('TEST')", primitiveBytesContext, "test"},
      {"fn:lowercase(value) == 'test-message '", primitiveBytesContext, true},
      {"fn:lowercase(null)", primitiveBytesContext, null},
      {"fn:coalesce(null, 'another-value')", primitiveBytesContext, "another-value"},
      {"fn:coalesce('value', 'another-value')", primitiveBytesContext, "value"},
      {"fn:coalesce(fn:str(value), 'another-value')", primitiveBytesContext, "Test-Message "},
      {"fn:contains(value, 'Test')", primitiveBytesContext, true},
      {"fn:contains(value, 'random')", primitiveBytesContext, false},
      {"fn:contains(null, 'random')", primitiveBytesContext, false},
      {"fn:contains(value, null)", primitiveBytesContext, false},
      {"fn:contains(value, null)", primitiveBytesContext, false},
      {"fn:trim('    trimmed      ')", primitiveBytesContext, "trimmed"},
      {"fn:trim(value)", primitiveBytesContext, "Test-Message"},
      {"fn:trim(null)", primitiveBytesContext, null},
      {"fn:concat(value, 'suffix')", primitiveBytesContext, "Test-Message suffix"},
      {"fn:concat(value, null)", primitiveBytesContext, "Test-Message "},
      {"fn:concat(null, 'suffix')", primitiveBytesContext, "suffix"},
      {"fn:concat('prefix-', value)", primitiveBytesContext, "prefix-Test-Message "},
      {"fn:replace(value, '.*-', '')", primitiveBytesContext, "Message "},
      {"fn:replace('Test-Message test', value, '')", primitiveBytesContext, "test"},
      {"fn:replace('Something test', '.* ', value)", primitiveBytesContext, "Test-Message test"},
      {"fn:replace(null, '.* ', '')", primitiveBytesContext, null},
      {"fn:replace('test', null, '')", primitiveBytesContext, "test"},
      {"fn:replace('test', '.*', null)", primitiveBytesContext, "test"},
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1, 'years')",
        primitiveBytesContext,
        Instant.parse("2018-01-02T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', -1, 'months')",
        primitiveBytesContext,
        Instant.parse("2016-12-02T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1, 'days')",
        primitiveBytesContext,
        Instant.parse("2017-01-03T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', -1, 'hours')",
        primitiveBytesContext,
        Instant.parse("2017-01-01T23:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1, 'minutes')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:02:02Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', -1, 'seconds')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:01Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1, 'millis')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1000000, 'nanos')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", 1, 'years')",
        primitiveBytesContext,
        Instant.parse("2018-01-02T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", -1, 'months')",
        primitiveBytesContext,
        Instant.parse("2016-12-02T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", 1, 'days')",
        primitiveBytesContext,
        Instant.parse("2017-01-03T00:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", -1, 'hours')",
        primitiveBytesContext,
        Instant.parse("2017-01-01T23:01:02Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", 1, 'minutes')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:02:02Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", -1, 'seconds')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:01Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", 1, 'millis')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
      {
        "fn:dateadd(" + millis + ", 1000000, 'nanos')",
        primitiveBytesContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
      {
        "fn:dateadd(value, '1', 'millis')",
        primitiveInstantContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
      {
        "fn:dateadd('2017-01-02T00:01:02Z', 1, value)",
        chronoUnitBytesContext,
        Instant.parse("2017-01-02T00:01:02.001Z").toEpochMilli()
      },
    };
  }
}
