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
package io.microsphere.spring.beans.factory.config;

import io.microsphere.util.BaseUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

import static io.microsphere.invoke.MethodHandleUtils.findVirtual;
import static io.microsphere.lang.function.ThrowableSupplier.execute;
import static io.microsphere.util.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static io.microsphere.util.ArrayUtils.length;
import static io.microsphere.util.ClassLoaderUtils.getDefaultClassLoader;
import static io.microsphere.util.ClassLoaderUtils.resolveClass;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forMethodReturnType;

/**
 * {@link BeanDefinition} Utilities class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see AbstractBeanDefinition
 * @see BeanDefinition#ROLE_APPLICATION
 * @see BeanDefinition#ROLE_INFRASTRUCTURE
 * @since 1.0.0
 */
public abstract class BeanDefinitionUtils extends BaseUtils {

    /**
     * The name of getResolvableType() method.
     *
     * <ul>
     *     <li>{@link RootBeanDefinition#getResolvableType()} since Spring Framework 5.1</li>
     *     <li>{@link AbstractBeanDefinition#getResolvableType()} Spring Framework 5.2</li>
     * </ul>
     */
    private static final String GET_RESOLVABLE_TYPE_METHOD_NAME = "getResolvableType";

    private static final MethodHandle GET_RESOLVABLE_TYPE_METHOD_HANDLE = findVirtual(RootBeanDefinition.class, GET_RESOLVABLE_TYPE_METHOD_NAME);

    /**
     * Build a generic instance of {@link AbstractBeanDefinition}
     *
     * @param beanType the type of bean
     * @return an instance of {@link AbstractBeanDefinition}
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType) {
        return genericBeanDefinition(beanType, EMPTY_OBJECT_ARRAY);
    }

    /**
     * Build a generic instance of {@link AbstractBeanDefinition}
     *
     * @param beanType             the type of bean
     * @param constructorArguments the arguments of Bean Classes' constructor
     * @return an instance of {@link AbstractBeanDefinition}
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType, Object... constructorArguments) {
        return genericBeanDefinition(beanType, ROLE_APPLICATION, constructorArguments);
    }

    /**
     * Build a generic instance of {@link AbstractBeanDefinition}
     *
     * @param beanType the type of bean
     * @param role     the role of {@link BeanDefinition}
     * @return an instance of {@link AbstractBeanDefinition}
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType, int role) {
        return genericBeanDefinition(beanType, role, EMPTY_OBJECT_ARRAY);
    }

    /**
     * Build a generic instance of {@link AbstractBeanDefinition}
     *
     * @param beanType             the type of bean
     * @param role                 the role of {@link BeanDefinition}
     * @param constructorArguments the arguments of Bean Classes' constructor
     * @return an instance of {@link AbstractBeanDefinition}
     */
    public static AbstractBeanDefinition genericBeanDefinition(Class<?> beanType, int role, Object[] constructorArguments) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanType)
                .setRole(role);
        // Add the arguments of constructor if present
        int length = length(constructorArguments);
        for (int i = 0; i < length; i++) {
            Object constructorArgument = constructorArguments[i];
            beanDefinitionBuilder.addConstructorArgValue(constructorArgument);
        }
        AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        return beanDefinition;
    }

    public static Class<?> resolveBeanType(RootBeanDefinition beanDefinition) {
        return resolveBeanType(beanDefinition, getDefaultClassLoader());
    }

    public static Class<?> resolveBeanType(RootBeanDefinition beanDefinition, @Nullable ClassLoader classLoader) {
        Class<?> beanClass = null;

        Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
        if (factoryMethod == null) {
            if (beanDefinition.hasBeanClass()) {
                beanClass = beanDefinition.getBeanClass();
            } else {
                String beanClassName = beanDefinition.getBeanClassName();
                if (StringUtils.hasText(beanClassName)) {
                    ClassLoader targetClassLoader = classLoader == null ? getDefaultClassLoader() : classLoader;
                    beanClass = resolveClass(beanClassName, targetClassLoader, true);
                }
            }
        } else {
            beanClass = factoryMethod.getReturnType();
        }
        return beanClass;
    }

    public static Set<String> findInfrastructureBeanNames(ConfigurableListableBeanFactory beanFactory) {
        return findBeanNames(beanFactory, BeanDefinitionUtils::isInfrastructureBean);
    }

    public static Set<String> findBeanNames(ConfigurableListableBeanFactory beanFactory, Predicate<BeanDefinition> predicate) {
        if (predicate == null) {
            return emptySet();
        }
        Set<String> matchedBeanNames = new LinkedHashSet<>();
        String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);
            if (predicate.test(beanDefinition)) {
                matchedBeanNames.add(beanDefinitionName);
            }
        }
        return unmodifiableSet(matchedBeanNames);
    }

    public static boolean isInfrastructureBean(BeanDefinition beanDefinition) {
        return beanDefinition != null && ROLE_INFRASTRUCTURE == beanDefinition.getRole();
    }

    /**
     * Get {@link ResolvableType} from {@link RootBeanDefinition}
     *
     * @param rootBeanDefinition {@link RootBeanDefinition}
     * @return non-null
     * @see RootBeanDefinition#getResolvableType()
     */
    public static ResolvableType getResolvableType(RootBeanDefinition rootBeanDefinition) {
        if (GET_RESOLVABLE_TYPE_METHOD_HANDLE == null) {
            return doGetResolvableType(rootBeanDefinition);
        }
        return execute(() -> (ResolvableType) GET_RESOLVABLE_TYPE_METHOD_HANDLE.invokeExact(rootBeanDefinition));
    }

    /**
     * Get {@link ResolvableType} from {@link AbstractBeanDefinition}
     *
     * @param beanDefinition {@link AbstractBeanDefinition}
     * @return {@link ResolvableType#NONE} if can't be resolved
     * @see AbstractBeanDefinition#getResolvableType()
     */
    public static ResolvableType getResolvableType(AbstractBeanDefinition beanDefinition) {
        if (beanDefinition instanceof RootBeanDefinition) {
            return getResolvableType((RootBeanDefinition) beanDefinition);
        }
        return doGetResolvableType(beanDefinition);
    }

    /**
     * Compatible with {@link RootBeanDefinition#getResolvableType()) since Spring Framework 5.1
     *
     * @param rootBeanDefinition {@link RootBeanDefinition}
     * @return
     */
    protected static ResolvableType doGetResolvableType(RootBeanDefinition rootBeanDefinition) {
        Method factoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
        if (factoryMethod != null) {
            return forMethodReturnType(factoryMethod);
        }
        return doGetResolvableType((AbstractBeanDefinition) rootBeanDefinition);
    }

    /**
     * Compatible with {@link AbstractBeanDefinition#getResolvableType()) since Spring Framework 5.2
     *
     * @param beanDefinition {@link AbstractBeanDefinition}
     * @return {@link ResolvableType#NONE} if can't be resolved
     */
    protected static ResolvableType doGetResolvableType(AbstractBeanDefinition beanDefinition) {
        return beanDefinition.hasBeanClass() ? forClass(beanDefinition.getBeanClass()) : ResolvableType.NONE;
    }
}