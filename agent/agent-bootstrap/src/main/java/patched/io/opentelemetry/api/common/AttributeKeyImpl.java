/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package patched.io.opentelemetry.api.common;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("rawtypes")
// TEMPORARY until optimization lands upstream
final class AttributeKeyImpl<T> implements AttributeKey<T> {

  private final AttributeType type;
  private final String key;
  private final int hashCode;

  private AttributeKeyImpl(AttributeType type, String key) {
    if (type == null) {
      throw new NullPointerException("Null type");
    }
    this.type = type;
    if (key == null) {
      throw new NullPointerException("Null key");
    }
    this.key = key;
    this.hashCode = buildHashCode();
  }

  // Used by auto-instrumentation agent. Check with auto-instrumentation before making changes to
  // this method.
  //
  // In particular, do not change this return type to AttributeKeyImpl because auto-instrumentation
  // hijacks this method and returns a bridged implementation of Context.
  //
  // Ideally auto-instrumentation would hijack the public AttributeKey.*Key() instead of this
  // method, but auto-instrumentation also needs to inject its own implementation of AttributeKey
  // into the class loader at the same time, which causes a problem because injecting a class into
  // the class loader automatically resolves its super classes (interfaces), which in this case is
  // Context, which would be the same class (interface) being instrumented at that time,
  // which would lead to the JVM throwing a LinkageError "attempted duplicate interface definition"
  static <T> AttributeKey<T> create(@Nullable String key, AttributeType type) {
    return new AttributeKeyImpl<>(type, key != null ? key : "");
  }

  @Override
  public AttributeType getType() {
    return type;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AttributeKeyImpl) {
      AttributeKeyImpl<?> that = (AttributeKeyImpl<?>) o;
      return this.type.equals(that.getType()) && this.key.equals(that.getKey());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return key;
  }

  private int buildHashCode() {
    int result = 1;
    result *= 1000003;
    result ^= type.hashCode();
    result *= 1000003;
    result ^= key.hashCode();
    return result;
  }
}
