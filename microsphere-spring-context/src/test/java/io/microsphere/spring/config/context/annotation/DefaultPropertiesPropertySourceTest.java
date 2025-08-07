/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.spring.config.context.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.microsphere.spring.core.env.PropertySourcesUtils.DEFAULT_PROPERTIES_PROPERTY_SOURCE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DefaultPropertiesPropertySource} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see DefaultPropertiesPropertySource
 * @since 1.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        DefaultPropertiesPropertySourceTest.class
})
@DefaultPropertiesPropertySource(value = {
        "classpath*:/META-INF/test/*.properties"
})
class DefaultPropertiesPropertySourceTest {

    @Autowired
    private ConfigurableEnvironment environment;

    private MutablePropertySources propertySources;

    @BeforeEach
    void setUp() {
        this.propertySources = environment.getPropertySources();
    }

    @Test
    void test() {
        assertTrue(propertySources.contains(DEFAULT_PROPERTIES_PROPERTY_SOURCE_NAME));
        assertEquals("1", environment.getProperty("a"));
        assertEquals("3", environment.getProperty("b"));
    }
}
