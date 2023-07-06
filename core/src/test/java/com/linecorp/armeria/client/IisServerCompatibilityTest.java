/*
 * Copyright 2023 LINE Corporation
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

package com.linecorp.armeria.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import java.nio.channels.ClosedChannelException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Windows may be a different order of closing events.")
class IisServerCompatibilityTest {

    @RegisterExtension
    static ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) {
            sb.https(0);
            sb.tlsSelfSigned();
            sb.childChannelPipelineCustomizer(pipeline -> {
                // Simulate the behavior of IIS.
                pipeline.channel().close();
            });
            sb.service("/", (ctx, req) -> HttpResponse.of("OK"));
        }
    };

    @Test
    void shouldRaiseSslHandleShakeExceptionWhenConnectionIsResetDuringTlsHandshake() {
        final BlockingWebClient client = WebClient.builder(server.httpsUri())
                                                  .factory(ClientFactory.insecure())
                                                  .build()
                                                  .blocking();
        final Throwable cause = catchThrowable(() -> client.get("/"));
        assertThat(cause).isInstanceOf(UnprocessedRequestException.class)
                         .getCause().isInstanceOf(IllegalStateException.class)
                         .hasMessageFindingMatch(
                                 "An unexpected exception during TLS handshake. " +
                                 "Possible reasons: no cipher suites in common, unsupported TLS version," +
                                 " etc. \\(TLS version: .*, cipher suites: .*\\)");
        final Throwable suppressed0 = cause.getCause().getSuppressed()[0];
        assertThat(suppressed0)
                .isInstanceOf(ClosedChannelException.class);
        assertThat(suppressed0.getSuppressed()[0])
                .isInstanceOf(SSLHandshakeException.class)
                .hasMessageContaining("Connection closed while SSL/TLS handshake was in progress");
    }
}
