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

package com.linecorp.armeria.common;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.annotation.UnstableApi;
import com.linecorp.armeria.common.metric.MeterIdPrefix;
import com.linecorp.armeria.common.metric.MoreMeterBinders;
import com.linecorp.armeria.server.ServerBuilder;

import io.netty.handler.ssl.SslContextBuilder;

/**
 * Provides {@link TlsKeyPair}s for TLS handshakes.
 */
@UnstableApi
@FunctionalInterface
public interface TlsProvider {

    /**
     * Returns a newly created {@link TlsProviderBuilder}.
     *
     * <pre>{@code
     * TlsProvider
     *   .builder()
     *   // Set the default key pair.
     *   .setDefault(TlsKeyPair.of(...))
     *   // Set the key pair for "example.com".
     *   .set("example.com", TlsKeyPair.of(...))
     *   .build();
     * }</pre>
     */
    static TlsProviderBuilder builder() {
        return new TlsProviderBuilder();
    }

    /**
     * Returns a {@link TlsProvider} which always returns the specified {@link TlsKeyPair}.
     */
    static TlsProvider of(TlsKeyPair tlsKeyPair) {
        requireNonNull(tlsKeyPair, "tlsKeyPair");
        return hostname -> tlsKeyPair;
    }

    /**
     * Finds a {@link TlsKeyPair} for the specified {@code hostname}.
     *
     * <p>Note that {@code "*"} is a special hostname which represents the
     * {@linkplain ServerBuilder#defaultVirtualHost() default virtual hostname}.
     * If no matching {@link TlsKeyPair} is found for a hostname, {@code "*"} can be specified to get the
     * {@link TlsKeyPair} of the default virtual host.
     */
    @Nullable
    TlsKeyPair find(String hostname);

    /**
     * Allows the bad cipher suites listed in
     * <a href="https://datatracker.ietf.org/doc/html/rfc7540#appendix-A">RFC7540</a> for TLS handshake.
     *
     * <p>Note that enabling this option increases the security risk of your connection.
     * Use it only when you must communicate with a legacy system that does not support
     * secure cipher suites.
     * See <a href="https://datatracker.ietf.org/doc/html/rfc7540#section-9.2.2">Section 9.2.2, RFC7540</a> for
     * more information. This option is disabled by default.
     *
     * @deprecated It's not recommended to enable this option. Use it only when you have no other way to
     *             communicate with an insecure peer than this.
     */
    @Deprecated
    default boolean allowsUnsafeCiphers() {
        return false;
    }

    /**
     * Returns the {@link Consumer} which can arbitrarily configure the {@link SslContextBuilder} that will be
     * applied to the SSL session.
     */
    default Consumer<SslContextBuilder> tlsCustomizer() {
        return builder -> {};
    }

    /**
     * Returns the {@link MeterIdPrefix} for TLS metrics.
     * If not specified, "armeria.server" or "armeria.client" will be used by default based on the usage.
     *
     * @see MoreMeterBinders#certificateMetrics(Iterable, MeterIdPrefix)
     */
    @Nullable
    default MeterIdPrefix meterIdPrefix() {
        return null;
    }
}
