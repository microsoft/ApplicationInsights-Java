/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.instrumentation.opentelemetryapi.v0_2_4.trace;

import static io.opentelemetry.trace.TracingContextUtils.getSpan;
import static io.opentelemetry.trace.TracingContextUtils.withSpan;

import io.grpc.Context;
import io.opentelemetry.trace.DefaultSpan;
import java.util.List;
import unshaded.io.opentelemetry.context.propagation.HttpTextFormat;
import unshaded.io.opentelemetry.trace.SpanContext;

class UnshadedHttpTextFormat implements HttpTextFormat<SpanContext> {

  private final io.opentelemetry.context.propagation.HttpTextFormat shadedHttpTextFormat;

  UnshadedHttpTextFormat(
      final io.opentelemetry.context.propagation.HttpTextFormat shadedHttpTextFormat) {
    this.shadedHttpTextFormat = shadedHttpTextFormat;
  }

  @Override
  public List<String> fields() {
    return shadedHttpTextFormat.fields();
  }

  @Override
  public <C> SpanContext extract(final C carrier, final HttpTextFormat.Getter<C> getter) {
    final Context context =
        shadedHttpTextFormat.extract(Context.ROOT, carrier, new ShadedGetter<>(getter));
    return Bridging.toUnshaded(getSpan(context).getContext());
  }

  @Override
  public <C> void inject(
      final SpanContext value, final C carrier, final HttpTextFormat.Setter<C> setter) {
    final Context context = withSpan(DefaultSpan.create(Bridging.toShaded(value)), Context.ROOT);
    shadedHttpTextFormat.inject(context, carrier, new ShadedSetter<>(setter));
  }

  private static class ShadedGetter<C>
      implements io.opentelemetry.context.propagation.HttpTextFormat.Getter<C> {

    private final HttpTextFormat.Getter<C> unshadedGetter;

    ShadedGetter(final HttpTextFormat.Getter<C> unshadedGetter) {
      this.unshadedGetter = unshadedGetter;
    }

    @Override
    public String get(final C carrier, final String key) {
      return unshadedGetter.get(carrier, key);
    }
  }

  private static class ShadedSetter<C>
      implements io.opentelemetry.context.propagation.HttpTextFormat.Setter<C> {

    private final HttpTextFormat.Setter<C> unshadedSetter;

    ShadedSetter(final Setter<C> unshadedSetter) {
      this.unshadedSetter = unshadedSetter;
    }

    @Override
    public void set(final C carrier, final String key, final String value) {
      unshadedSetter.set(carrier, key, value);
    }
  }
}
