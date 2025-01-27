/*
 * Copyright 2024 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.common.reactor3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.linecorp.armeria.common.CommonPools;

import io.netty.util.concurrent.Future;
import reactor.core.scheduler.Schedulers;

final class EventLoopNonBlockingTest {

    /**
     * Verifies that the current thread is registered a non-blocking thread via {@code ReactorNonBlockingUtil}.
     */
    @Test
    void checkEventLoopNonBlocking() throws Exception {
        final Future<Boolean> submit = CommonPools.workerGroup().submit(Schedulers::isInNonBlockingThread);
        assertThat(submit.get()).isTrue();
    }
}
