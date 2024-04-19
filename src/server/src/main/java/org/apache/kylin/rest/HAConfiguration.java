/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kylin.rest;

import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextListener;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@Profile("!dev")
public class HAConfiguration extends AbstractHttpSessionApplicationInitializer {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        val restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(
                HttpClientBuilder.create().disableCookieManagement().useSystemProperties().build()));
        restTemplate.setUriTemplateHandler(new ProxyUriTemplateHandler());
        return restTemplate;
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}
