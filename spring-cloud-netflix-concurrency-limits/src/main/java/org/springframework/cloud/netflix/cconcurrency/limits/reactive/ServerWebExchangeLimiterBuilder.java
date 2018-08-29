/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.netflix.cconcurrency.limits.reactive;

import java.security.Principal;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limiter.AbstractLimiterBuilder;
import com.netflix.concurrency.limits.strategy.LookupPartitionStrategy;

import org.springframework.web.server.ServerWebExchange;

public class ServerWebExchangeLimiterBuilder extends AbstractLimiterBuilder<ServerWebExchangeLimiterBuilder, ServerWebExchange> {
	/**
	 * Partition the limit by header
	 * @param configurer Configuration function though which header percentages may be specified
	 *                   Unspecified header values may only use excess capacity.
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByHeader(String name, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionByLookup(
				exchange -> exchange.getRequest().getHeaders().getFirst(name),
				configurer);
	}

	/**
	 * Partition the limit by {@link Principal}. Percentages of the limit are partitioned to named
	 * groups.  Group membership is derived from the provided mapping function.
	 * @param principalToGroup Mapping function from {@link Principal} to a named group.
	 * @param configurer Configuration function though which group percentages may be specified
	 *                   Unspecified group values may only use excess capacity.
	 * @return Chainable builder
	 */
	/*public ServerWebExchangeLimiterBuilder partitionByUserPrincipal(Function<Principal, String> principalToGroup, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionByLookup(
				exchange -> Optional.ofNullable(request.getUserPrincipal()).map(principalToGroup).orElse(null),
				configurer);
	}*/

	/**
	 * Partition the limit by request attribute
	 * @param configurer Configuration function though which attribute percentages may be specified
	 *                   Unspecified attribute values may only use excess capacity.
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByAttribute(String name, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionByLookup(
				exchange -> exchange.getAttribute(name),
				configurer);
	}

	/**
	 * Partition the limit by request parameter
	 * @param configurer Configuration function though which parameter value percentages may be specified
	 *                   Unspecified parameter values may only use excess capacity.
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByParameter(String name, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionByLookup(
				exchange -> exchange.getRequest().getQueryParams().getFirst(name),
				configurer);
	}

	/**
	 * Partition the limit by the full path. Percentages of the limit are partitioned to named
	 * groups.  Group membership is derived from the provided mapping function.
	 * @param pathToGroup Mapping function from full path to a named group.
	 * @param configurer Configuration function though which group percentages may be specified
	 *                   Unspecified group values may only use excess capacity.
	 * @return Chainable builder
	 */
	public ServerWebExchangeLimiterBuilder partitionByPathInfo(Function<String, String> pathToGroup, Consumer<LookupPartitionStrategy.Builder<ServerWebExchange>> configurer) {
		return partitionByLookup(
				exchange -> {
					//TODO: pathWithinApplication?
					String path = exchange.getRequest().getPath().contextPath().value();
					return Optional.ofNullable(path).map(pathToGroup).orElse(null);
				},
				configurer);
	}

	@Override
	protected ServerWebExchangeLimiterBuilder self() {
		return this;
	}

	public Limiter<ServerWebExchange> build() {
		return buildLimiter();
	}
}
