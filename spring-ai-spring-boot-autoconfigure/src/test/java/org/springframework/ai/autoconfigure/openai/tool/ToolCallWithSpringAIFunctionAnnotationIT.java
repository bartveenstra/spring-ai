/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.autoconfigure.openai.tool;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.autoconfigure.common.function.SpringAiFunction;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".*")
class ToolCallWithSpringAIFunctionAnnotationIT {

	private final Logger logger = LoggerFactory.getLogger(ToolCallWithBeanFunctionRegistrationIT.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.openai.apiKey=" + System.getenv("OPENAI_API_KEY"))
		.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, OpenAiAutoConfiguration.class))
		.withUserConfiguration(Config.class);

	@Test
	void functionCallTest() {
		contextRunner.withPropertyValues("spring.ai.openai.chat.options.model=gpt-4-1106-preview").run(context -> {

			OpenAiChatClient chatClient = context.getBean(OpenAiChatClient.class);

			UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");

			ChatResponse response = chatClient.call(new Prompt(List.of(userMessage),
					OpenAiChatOptions.builder().withEnabledFunction("WeatherInfo").build()));

			logger.info("Response: {}", response);

			assertThat(response.getResult().getOutput().getContent()).contains("30", "10", "15");

		});
	}

	@Configuration
	static class Config {

		@SpringAiFunction(name = "WeatherInfo", description = "Get the weather in location",
				classType = MockWeatherService.Request.class)
		public Function<MockWeatherService.Request, MockWeatherService.Response> weatherFunction() {
			MockWeatherService weatherService = new MockWeatherService();
			return (weatherService::apply);
		}

	}

}