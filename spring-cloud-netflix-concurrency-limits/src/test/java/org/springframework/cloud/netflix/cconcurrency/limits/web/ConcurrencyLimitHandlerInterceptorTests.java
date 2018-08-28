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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import com.netflix.concurrency.limits.Limiter;
import com.netflix.concurrency.limits.limit.SettableLimit;
import com.netflix.concurrency.limits.servlet.ServletLimiterBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(/*properties = "logging.level.reactor=DEBUG",*/ webEnvironment = RANDOM_PORT)
public class ConcurrencyLimitHandlerInterceptorTests {

	@LocalServerPort
	public int port;

	private WebClient client;

	@Before
	public void init() {
		client = WebClient.create("http://localhost:"+port);
	}

	@Test
	public void handlerInterceptorWorks() {

		// TODO: assert the body
		/*ParallelFlux<Tuple2<String, HttpStatus>> flux = Flux.range(1, 10)
				.flatMap(integer -> client.get().uri("/").exchange())
				.parallel(2)
				.runOn(Schedulers.parallel())
				.flatMap(response -> response.bodyToMono(String.class).zipWith(Mono.just(response.statusCode())))
				.log("reqs", Level.INFO);

		Responses responses = new Responses();
		StepVerifier.create(flux)
				.thenConsumeWhile(response -> true, response -> {
					HttpStatus status = response.getT2();
					if (status.equals(HttpStatus.OK)) {
						responses.success.incrementAndGet();
					} else if (status.equals(HttpStatus.TOO_MANY_REQUESTS)) {
						responses.tooManyReqs.incrementAndGet();
						String body = response.getT1();
						System.out.println(body);
					} else {
						responses.other.incrementAndGet();
					}
				}).verifyComplete();*/

		ParallelFlux<ClientResponse> flux = Flux.range(1, 10)
				.flatMap(integer -> client.get().uri("/").exchange())
				.parallel(2)
				.runOn(Schedulers.parallel());

		Responses responses = new Responses();
		StepVerifier.create(flux)
				.thenConsumeWhile(response -> true, response -> {
					if (response.statusCode().equals(HttpStatus.OK)) {
						responses.success.incrementAndGet();
					} else if (response.statusCode().equals(HttpStatus.TOO_MANY_REQUESTS)) {
						responses.tooManyReqs.incrementAndGet();
						// String body = response.bodyToMono(String.class).block();
						// System.out.println(body);
					} else {
						responses.other.incrementAndGet();
					}
				}).verifyComplete();

		System.out.println("Responses: " + responses);

		assertThat(responses.other).hasValue(0);
		assertThat(responses.tooManyReqs).hasValueGreaterThanOrEqualTo(1);
	}

	protected static class Responses {
		AtomicInteger success = new AtomicInteger(0);
		AtomicInteger tooManyReqs = new AtomicInteger(0);
		AtomicInteger other = new AtomicInteger(0);

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("success", success)
					.append("tooManyReqs", tooManyReqs)
					.append("other", other)
					.toString();
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@RestController
	protected static class TestConfig implements WebMvcConfigurer {

		@GetMapping
		public String get() throws Exception {
			Thread.sleep(300);
			return "Hello";
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			Limiter<HttpServletRequest> limiter = new ServletLimiterBuilder()
					.limiter(builder -> builder.limit(SettableLimit.startingAt(1)))
					.build();
			ConcurrencyLimitHandlerInterceptor interceptor = new ConcurrencyLimitHandlerInterceptor(limiter);
			registry.addInterceptor(interceptor);
		}
	}

}
