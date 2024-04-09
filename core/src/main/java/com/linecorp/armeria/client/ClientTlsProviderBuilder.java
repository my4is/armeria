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

package com.linecorp.armeria.client;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import com.linecorp.armeria.common.AbstractTlsProviderBuilder;
import com.linecorp.armeria.common.TlsKeyPair;
import com.linecorp.armeria.common.TlsProvider;
import com.linecorp.armeria.common.metric.MeterIdPrefix;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class ClientTlsProviderBuilder extends AbstractTlsProviderBuilder {

    private boolean tlsNoVerifySet;
    private final Set<String> insecureHosts = new HashSet<>();

    public ClientTlsProviderBuilder tlsNoVerify() {
        tlsNoVerifySet = true;
        checkState(insecureHosts.isEmpty(), "tlsNoVerify() and tlsNoVerifyHosts() are mutually exclusive.");
        return this;
    }

    public ClientTlsProviderBuilder tlsNoVerifyHosts(String... insecureHosts) {
        requireNonNull(insecureHosts, "insecureHosts");
        return tlsNoVerifyHosts(ImmutableList.copyOf(insecureHosts));
    }

    public ClientTlsProviderBuilder tlsNoVerifyHosts(Iterable<String> insecureHosts) {
        requireNonNull(insecureHosts, "insecureHosts");
        checkState(!tlsNoVerifySet, "tlsNoVerify() and tlsNoVerifyHosts() are mutually exclusive.");
        insecureHosts.forEach(this.insecureHosts::add);
        return this;
    }

    @Override
    public TlsProvider build() {
        if (tlsNoVerifySet) {
            tlsCustomizer(b -> b.trustManager(InsecureTrustManagerFactory.INSTANCE));
        } else if (!insecureHosts.isEmpty()) {
            tlsCustomizer(b -> b.trustManager(IgnoreHostsTrustManager.of(insecureHosts)));
        }
        return super.build();
    }

    // Override the return type of the chaining methods in the superclass.

    @Override
    public ClientTlsProviderBuilder set(String hostname, TlsKeyPair tlsKeyPair) {
        return (ClientTlsProviderBuilder) super.set(hostname, tlsKeyPair);
    }

    @Override
    public ClientTlsProviderBuilder setDefault(TlsKeyPair tlsKeyPair) {
        return (ClientTlsProviderBuilder) super.setDefault(tlsKeyPair);
    }

    @Deprecated
    @Override
    public ClientTlsProviderBuilder allowsUnsafeCiphers(boolean allowsUnsafeCiphers) {
        return (ClientTlsProviderBuilder) super.allowsUnsafeCiphers(allowsUnsafeCiphers);
    }

    @Override
    public ClientTlsProviderBuilder tlsCustomizer(Consumer<? super SslContextBuilder> tlsCustomizer) {
        return (ClientTlsProviderBuilder) super.tlsCustomizer(tlsCustomizer);
    }

    @Override
    public ClientTlsProviderBuilder meterIdPrefix(MeterIdPrefix meterIdPrefix) {
        return (ClientTlsProviderBuilder) super.meterIdPrefix(meterIdPrefix);
    }
}
