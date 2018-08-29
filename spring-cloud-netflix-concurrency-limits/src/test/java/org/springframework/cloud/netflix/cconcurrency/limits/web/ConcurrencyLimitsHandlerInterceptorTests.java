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

package org.springframework.cloud.netflix.cconcurrency.limits.web;

import com.netflix.concurrency.limits.limit.SettableLimit;
import com.netflix.concurrency.limits.servlet.ServletLimiterBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.netflix.cconcurrency.limits.support.LimiterBuilderConfigurer;
import org.springframework.cloud.netflix.cconcurrency.limits.test.AbstractConcurrencyLimitsTests;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "logging.level.reactor.netty=DEBUG", webEnvironment = RANDOM_PORT)
public class ConcurrencyLimitsHandlerInterceptorTests extends AbstractConcurrencyLimitsTests {

	@LocalServerPort
	public int port;

	private WebClient client;

	@Before
	public void init() {
		client = WebClient.create("http://localhost:"+port);
	}

	@Test
	public void handlerInterceptorWorks() {
		assertLimiter(client);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	protected static class TestConfig {

		@GetMapping
		public String get() throws Exception {
			return "Hello";
		}

		@Bean
		public LimiterBuilderConfigurer<ServletLimiterBuilder> limiterBuilderConfigurer() {
			return servletLimiterBuilder -> servletLimiterBuilder
					.limiter(limiterBuilder -> limiterBuilder.limit(SettableLimit.startingAt(1)));
		}
	}

}
