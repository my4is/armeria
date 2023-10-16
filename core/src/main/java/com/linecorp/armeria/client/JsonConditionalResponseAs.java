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

import static com.linecorp.armeria.client.ResponseAsUtil.OBJECT_MAPPER;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.ResponseEntity;
import com.linecorp.armeria.common.annotation.UnstableApi;

/**
 * Provides a way for users to add {@link ResponseAs} mappings to transform an aggregated response
 * given that the corresponding {@link Predicate} is satisfied. Note that the conditionals are
 * invoked in the order in which they are added.
 *
 * <pre>{@code
 * RestClient.of(...)
 *   .get("/")
 *   .execute(
 *     ResponseAs.<MyResponse>json(MyMessage.class, res -> res.status().isError())
 *       .orElseJson(EmptyMessage.class, res -> res.status().isInformational())
 *       .orElseJson(MyError.class)).join();
 * }</pre>
 */
@UnstableApi
public final class JsonConditionalResponseAs<T> {

    private final List<Entry<Predicate<AggregatedHttpResponse>,
            ResponseAs<AggregatedHttpResponse, ResponseEntity<T>>>> responseConverters = new ArrayList<>();

    JsonConditionalResponseAs(Predicate<AggregatedHttpResponse> predicate,
                              ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> responseAs) {
        responseConverters.add(Maps.immutableEntry(
                requireNonNull(predicate, "predicate"), requireNonNull(responseAs, "responseAs")));
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * to the specified {@link Class}.
     */
    public JsonConditionalResponseAs<T> orElseJson(
            Class<? extends T> clazz, Predicate<AggregatedHttpResponse> predicate) {
        return orElseJson(clazz, OBJECT_MAPPER, predicate);
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * to the specified {@link Class} using the {@link ObjectMapper}.
     */
    public JsonConditionalResponseAs<T> orElseJson(
            Class<? extends T> clazz, ObjectMapper objectMapper, Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(clazz, "clazz");
        requireNonNull(objectMapper, "objectMapper");
        requireNonNull(predicate, "predicate");
        responseConverters.add(Maps.immutableEntry(predicate, AggregatedResponseAs.json(clazz, objectMapper)));
        return this;
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * using the specified {@link TypeReference}.
     */
    public JsonConditionalResponseAs<T> orElseJson(
            TypeReference<? extends T> typeRef, Predicate<AggregatedHttpResponse> predicate) {
        return orElseJson(typeRef, OBJECT_MAPPER, predicate);
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * using the specified {@link TypeReference} and {@link ObjectMapper}.
     */
    public JsonConditionalResponseAs<T> orElseJson(
            TypeReference<? extends T> typeRef, ObjectMapper objectMapper,
            Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(typeRef, "typeRef");
        requireNonNull(objectMapper, "objectMapper");
        requireNonNull(predicate, "predicate");
        responseConverters.add(Maps.immutableEntry(predicate,
                                                   AggregatedResponseAs.json(typeRef, objectMapper)));
        return this;
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * to the specified {@link Class}.
     */
    public FutureResponseAs<ResponseEntity<T>> orElseJson(Class<? extends T> clazz) {
        return orElse(AggregatedResponseAs.json(clazz));
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * to the specified {@link Class} using the {@link ObjectMapper}.
     */
    public FutureResponseAs<ResponseEntity<T>> orElseJson(
            Class<? extends T> clazz, ObjectMapper objectMapper) {
        return orElse(AggregatedResponseAs.json(clazz, objectMapper));
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * using the specified {@link TypeReference}.
     */
    public FutureResponseAs<ResponseEntity<T>> orElseJson(TypeReference<? extends T> typeRef) {
        return orElse(AggregatedResponseAs.json(typeRef));
    }

    /**
     * Returns {@link ResponseAs} based on the configured {@link ResponseAs} to {@link Predicate}
     * mappings. If none of the {@link Predicate}s are satisfied, the content will be deserialized
     * using the specified {@link TypeReference} and {@link ObjectMapper}.
     */
    public FutureResponseAs<ResponseEntity<T>> orElseJson(
            TypeReference<? extends T> typeRef, ObjectMapper objectMapper) {
        return orElse(AggregatedResponseAs.json(typeRef, objectMapper));
    }

    private FutureResponseAs<ResponseEntity<T>> orElse(
            ResponseAs<AggregatedHttpResponse, ResponseEntity<T>> lastConverter) {
        final List<Entry<Predicate<AggregatedHttpResponse>,
                ResponseAs<AggregatedHttpResponse, ResponseEntity<T>>>> converters =
                ImmutableList.copyOf(responseConverters);
        return new FutureResponseAs<ResponseEntity<T>>() {
            @Override
            public CompletableFuture<ResponseEntity<T>> as(HttpResponse response) {
                requireNonNull(response, "response");
                return response.aggregate().thenApply(aggregated -> {
                    for (Entry<Predicate<AggregatedHttpResponse>,
                            ResponseAs<AggregatedHttpResponse, ResponseEntity<T>>>
                            converter : converters) {
                        if (converter.getKey().test(aggregated)) {
                            return converter.getValue().as(aggregated);
                        }
                    }
                    return lastConverter.as(aggregated);
                });
            }

            @Override
            public boolean requiresAggregation() {
                return true;
            }
        };
    }
}
