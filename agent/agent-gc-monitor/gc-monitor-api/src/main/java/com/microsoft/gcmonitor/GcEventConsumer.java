// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import java.util.function.Consumer;

public interface GcEventConsumer extends Consumer<GcCollectionEvent> {}
