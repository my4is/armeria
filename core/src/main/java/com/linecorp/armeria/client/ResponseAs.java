/*
 * Copyright 2022 LINE Corporation
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

import static com.linecorp.armeria.client.ResponseAsUtil.aggregateAndConvert;
import static com.spotify.futures.CompletableFutures.allAsList;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpHeaders;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.JacksonObjectMapperProvider;
import com.linecorp.armeria.common.ResponseEntity;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.SplitHttpResponse;
import com.linecorp.armeria.common.annotation.UnstableApi;
import com.linecorp.armeria.common.stream.StreamMessages;

/**
 * Transforms a response into another.
 */
@UnstableApi
@FunctionalInterface
public interface ResponseAs<T, R> {
    Predicate<AggregatedHttpResponse> SUCCESS_PREDICATE = res -> res.status().isSuccess();
    Predicate<AggregatedHttpResponse> TRUE_PREDICATE = unused -> true;

    /**
     * Aggregates an {@link HttpResponse} and waits the result of {@link HttpResponse#aggregate()}.
     */
    @UnstableApi
    static ResponseAs<HttpResponse, AggregatedHttpResponse> blocking() {
        return ResponseAsUtil.BLOCKING;
    }

    /**
     * Aggregates an {@link HttpResponse} and converts the {@link AggregatedHttpResponse#content()} into bytes.
     */
    @UnstableApi
    static FutureResponseAs<ResponseEntity<byte[]>> bytes() {
        return aggregateAndConvert(AggregatedResponseAs.bytes());
    }

    /**
     * Aggregates an {@link HttpResponse} and converts the {@link AggregatedHttpResponse#content()} into
     * {@link String}.
     */
    @UnstableApi
    static FutureResponseAs<ResponseEntity<String>> string() {
        return aggregateAndConvert(AggregatedResponseAs.string());
    }

    /**
     * Writes the content of an {@link HttpResponse} into the specified {@link Path}.
     */
    @UnstableApi
    static FutureResponseAs<ResponseEntity<Path>> path(Path path) {
        requireNonNull(path, "path");
        return response -> {
            final SplitHttpResponse splitResponse = response.split();
            final CompletableFuture<Void> future = StreamMessages.writeTo(splitResponse.body(), path);

            return allAsList(ImmutableList.of(splitResponse.headers(), future, splitResponse.trailers()))
                    .thenApply(objects -> {
                        final ResponseHeaders headers = (ResponseHeaders) objects.get(0);
                        final HttpHeaders trailers = (HttpHeaders) objects.get(2);
                        return ResponseEntity.of(headers, path, trailers);
                    });
        };
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified non-container type using the default {@link ObjectMapper}.
     *
     * <p>Note that this method should NOT be used if the result type is a container ({@link Collection} or
     * {@link Map}. Use {@link #json(TypeReference)} for the container type.
     *
     * @see JacksonObjectMapperProvider
     */
    @UnstableApi
    static <T> FutureResponseAs<ResponseEntity<T>> json(Class<? extends T> clazz) {
        requireNonNull(clazz, "clazz");
        return aggregateAndConvert(AggregatedResponseAs.json(clazz, SUCCESS_PREDICATE));
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified non-container type using the specified {@link ObjectMapper}.
     *
     * <p>Note that this method should NOT be used if the result type is a container ({@link Collection} or
     * {@link Map}. Use {@link #json(TypeReference, ObjectMapper)} for the container type.
     */
    @UnstableApi
    static <T> FutureResponseAs<ResponseEntity<T>> json(Class<? extends T> clazz, ObjectMapper mapper) {
        requireNonNull(clazz, "clazz");
        requireNonNull(mapper, "mapper");
        return aggregateAndConvert(AggregatedResponseAs.json(clazz, mapper, SUCCESS_PREDICATE));
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified Java type using the default {@link ObjectMapper}.
     *
     * @see JacksonObjectMapperProvider
     */
    @UnstableApi
    static <T> FutureResponseAs<ResponseEntity<T>> json(TypeReference<? extends T> typeRef) {
        requireNonNull(typeRef, "typeRef");
        return aggregateAndConvert(AggregatedResponseAs.json(typeRef, SUCCESS_PREDICATE));
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified Java type using the specified {@link ObjectMapper}.
     */
    @UnstableApi
    static <T> FutureResponseAs<ResponseEntity<T>> json(TypeReference<? extends T> typeRef,
                                                        ObjectMapper mapper) {
        requireNonNull(typeRef, "typeRef");
        requireNonNull(mapper, "mapper");
        return aggregateAndConvert(AggregatedResponseAs.json(typeRef, mapper, SUCCESS_PREDICATE));
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified non-container type using the default {@link ObjectMapper} if the
     * {@link Predicate} is satisifed.
     *
     * <p>Note that this method should NOT be used if the result type is a container ({@link Collection} or
     * {@link Map}. Use {@link #json(TypeReference, Predicate)} for the container type.
     *
     * @see JacksonObjectMapperProvider
     */
    @UnstableApi
    static <V> BlockingConditionalResponseAs<V> json(
            Class<? extends V> clazz, Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(clazz, "clazz");
        return new BlockingConditionalResponseAs<>(blocking(),
                                                   AggregatedResponseAs.json(clazz, TRUE_PREDICATE),
                                                   predicate);
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified non-container type using the specified {@link ObjectMapper} if the
     * {@link Predicate} is satisfied.
     *
     * <p>Note that this method should NOT be used if the result type is a container ({@link Collection} or
     * {@link Map}. Use {@link #json(TypeReference, ObjectMapper, Predicate)} for the container type.
     */
    @UnstableApi
    static <V> BlockingConditionalResponseAs<V> json(
            Class<? extends V> clazz, ObjectMapper mapper, Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(clazz, "clazz");
        requireNonNull(mapper, "mapper");
        return new BlockingConditionalResponseAs<>(
                blocking(), AggregatedResponseAs.json(clazz, mapper, TRUE_PREDICATE), predicate);
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified Java type using the default {@link ObjectMapper} if the {@link Predicate}
     * is satisfied.
     *
     * @see JacksonObjectMapperProvider
     */
    @UnstableApi
    static <V> BlockingConditionalResponseAs<V> json(
            TypeReference<? extends V> typeRef, Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(typeRef, "typeRef");
        return new BlockingConditionalResponseAs<>(blocking(),
                                                   AggregatedResponseAs.json(typeRef, TRUE_PREDICATE),
                                                   predicate);
    }

    /**
     * Aggregates an {@link HttpResponse} and deserializes the JSON {@link AggregatedHttpResponse#content()}
     * into the specified Java type using the specified {@link ObjectMapper} if the {@link Predicate}
     * is satisfied.
     */
    @UnstableApi
    static <V> BlockingConditionalResponseAs<V> json(
            TypeReference<? extends V> typeRef, ObjectMapper mapper,
            Predicate<AggregatedHttpResponse> predicate) {
        requireNonNull(typeRef, "typeRef");
        requireNonNull(mapper, "mapper");
        return new BlockingConditionalResponseAs<>(
                blocking(), AggregatedResponseAs.json(typeRef, mapper, TRUE_PREDICATE), predicate);
    }

    /**
     * Transforms the response into another.
     */
    @UnstableApi
    R as(T response);

    /**
     * Returns whether the response should be aggregated.
     */
    @UnstableApi
    default boolean requiresAggregation() {
        return false;
    }

    /**
     * Returns a composed {@link ResponseAs} that first applies this {@link ResponseAs} to
     * its input, and then applies the {@code after} {@link ResponseAs} to the result.
     */
    @UnstableApi
    default <V> ResponseAs<T, V> andThen(ResponseAs<R, V> after) {
        requireNonNull(after, "after");
        return new ResponseAs<T, V>() {

            @Override
            public V as(T response) {
                requireNonNull(response, "response");
                return after.as(ResponseAs.this.as(response));
            }

            @Override
            public boolean requiresAggregation() {
                if (ResponseAs.this.requiresAggregation()) {
                    // The response was aggregated already.
                    return true;
                }
                return after.requiresAggregation();
            }
        };
    }

    /**
     * Returns a {@link DefaultConditionalResponseAs} which is used to return {@link ResponseAs} whose
     * {@link Predicate} is evaluated as true.
     */
    default <V> DefaultConditionalResponseAs<T, R, V> andThen(
            ResponseAs<R, V> responseAs, Predicate<R> predicate) {
        return new DefaultConditionalResponseAs<>(this, responseAs, predicate);
    }
}
