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

import static com.linecorp.armeria.internal.common.TlsProviderUtil.normalizeHostname;
import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.collect.ImmutableMap;

import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.metric.MeterIdPrefix;
import com.linecorp.armeria.server.ServerBuilder;

import io.netty.handler.ssl.SslContextBuilder;

/**
 * Builds a new {@link TlsProvider}.
 */
public final class TlsProviderBuilder {

    private static final Consumer<SslContextBuilder> NOOP = b -> {};

    private final ImmutableMap.Builder<String, TlsKeyPair> tlsKeyPairMappings = ImmutableMap.builder();
    private boolean allowsUnsafeCiphers;
    private Consumer<SslContextBuilder> tlsCustomizer = NOOP;
    @Nullable
    private MeterIdPrefix meterIdPrefix;

    TlsProviderBuilder() {}

    /**
     * Set the {@link TlsKeyPair} for the specified (optionally wildcard) {@code hostname}.
     *
     * <p><a href="https://en.wikipedia.org/wiki/Wildcard_DNS_record">DNS wildcard</a> is supported as hostname.
     * The wildcard will only match one sub-domain deep and only when wildcard is used as the most-left label.
     * For example, *.armeria.dev will match foo.armeria.dev but NOT bar.foo.armeria.dev
     *
     * <p>Note that {@code "*"} is a special hostname which matches any hostname which may be used to find the
     * {@link TlsKeyPair} for the {@linkplain ServerBuilder#defaultVirtualHost() default virtual host}.
     */
    public TlsProviderBuilder set(String hostname, TlsKeyPair tlsKeyPair) {
        requireNonNull(hostname, "hostname");
        requireNonNull(tlsKeyPair, "tlsKeyPair");
        tlsKeyPairMappings.put(normalizeHostname(hostname), tlsKeyPair);
        return this;
    }

    /**
     * Set the default {@link TlsKeyPair} which is used when no {@link TlsKeyPair} is specified for a hostname.
     */
    public TlsProviderBuilder setDefault(TlsKeyPair tlsKeyPair) {
        return set("*", tlsKeyPair);
    }

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
     * @param allowsUnsafeCiphers Whether to allow the unsafe ciphers
     *
     * @deprecated It's not recommended to enable this option. Use it only when you have no other way to
     *             communicate with an insecure peer than this.
     */
    @Deprecated
    public TlsProviderBuilder allowsUnsafeCiphers(boolean allowsUnsafeCiphers) {
        this.allowsUnsafeCiphers = allowsUnsafeCiphers;
        return this;
    }

    /**
     * Adds the {@link Consumer} which can arbitrarily configure the {@link SslContextBuilder} that will be
     * applied to the SSL session. For example, use {@link SslContextBuilder#trustManager(TrustManagerFactory)}
     * to configure a custom server CA or {@link SslContextBuilder#keyManager(KeyManagerFactory)} to configure
     * a client certificate for SSL authorization.
     */
    public TlsProviderBuilder tlsCustomizer(Consumer<? super SslContextBuilder> tlsCustomizer) {
        requireNonNull(tlsCustomizer, "tlsCustomizer");
        if (this.tlsCustomizer == NOOP) {
            //noinspection unchecked
            this.tlsCustomizer = (Consumer<SslContextBuilder>) tlsCustomizer;
        } else {
            this.tlsCustomizer = this.tlsCustomizer.andThen(tlsCustomizer);
        }
        return this;
    }

    /**
     * Sets the {@link MeterIdPrefix} for the {@link TlsProvider}.
     */
    public TlsProviderBuilder meterIdPrefix(MeterIdPrefix meterIdPrefix) {
        this.meterIdPrefix = requireNonNull(meterIdPrefix, "meterIdPrefix");
        return this;
    }

    /**
     * Returns a newly-created {@link TlsProvider} instance.
     */
    public TlsProvider build() {
        return new DefaultTlsProvider(tlsKeyPairMappings.build(), tlsCustomizer, allowsUnsafeCiphers,
                                      meterIdPrefix);
    }
}
