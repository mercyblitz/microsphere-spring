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

import io.microsphere.annotation.Nonnull;
import io.microsphere.annotation.Nullable;
import io.microsphere.logging.Logger;
import io.microsphere.util.Utils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.microsphere.invoke.MethodHandleUtils.findVirtual;
import static io.microsphere.lang.function.Predicates.and;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.spring.util.MethodHandleUtils.handleInvokeExactFailure;
import static io.microsphere.util.ArrayUtils.EMPTY_OBJECT_ARRAY;
import static io.microsphere.util.ArrayUtils.length;
import static io.microsphere.util.ClassLoaderUtils.getDefaultClassLoader;
import static io.microsphere.util.ClassLoaderUtils.resolveClass;
import static java.util.Collections.unmodifiableSet;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;
import static org.springframework.core.ResolvableType.NONE;
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
public abstract class BeanDefinitionUtils implements Utils {

    private static final Logger logger = getLogger(BeanDefinitionUtils.class);

    /**
     * The name of getResolvableType() method.
     *
     * <ul>
     *     <li>{@link RootBeanDefinition#getResolvableType()} since Spring Framework 5.1</li>
     *     <li>{@link AbstractBeanDefinition#getResolvableType()} Spring Framework 5.2</li>
     * </ul>
     */
    private static final String GET_RESOLVABLE_TYPE_METHOD_NAME = "getResolvableType";

    /**
     * The method name of {@linkplain AbstractBeanDefinition#setInstanceSupplier(Supplier)}
     *
     * @since Spring Framework 5.0
     */
    private static final String SET_INSTANCE_SUPPLIER_METHOD_NAME = "setInstanceSupplier";

    /**
     * The method name of {@linkplain AbstractBeanDefinition#getInstanceSupplier()}
     *
     * @since Spring Framework 5.0
     */
    private static final String GET_INSTANCE_SUPPLIER_METHOD_NAME = "getInstanceSupplier";

    /**
     * The {@link MethodHandle} of {@linkplain RootBeanDefinition#getResolvableType()}
     *
     * @since Spring Framework 5.1
     */
    private static final MethodHandle GET_RESOLVABLE_TYPE_METHOD_HANDLE = findVirtual(RootBeanDefinition.class, GET_RESOLVABLE_TYPE_METHOD_NAME);

    /**
     * The {@link MethodHandle} of {@linkplain AbstractBeanDefinition#setInstanceSupplier(Supplier)}
     *
     * @since Spring Framework 5.0
     */
    private static final MethodHandle SET_INSTANCE_SUPPLIER_METHOD_HANDLE = findVirtual(AbstractBeanDefinition.class, SET_INSTANCE_SUPPLIER_METHOD_NAME, Supplier.class);

    /**
     * The {@link MethodHandle} of {@linkplain AbstractBeanDefinition#getInstanceSupplier()}
     *
     * @since Spring Framework 5.0
     */
    private static final MethodHandle GET_INSTANCE_SUPPLIER_METHOD_HANDLE = findVirtual(AbstractBeanDefinition.class, GET_INSTANCE_SUPPLIER_METHOD_NAME);

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
        ResolvableType resolvableType = getResolvableType(beanDefinition);
        Class<?> beanClass = resolvableType.resolve();
        if (beanClass == null) { // resolving the bean class as fallback
            String beanClassName = beanDefinition.getBeanClassName();
            beanClass = resolveClass(beanClassName, classLoader);
        }
        return beanClass;
    }

    public static Set<String> findInfrastructureBeanNames(ConfigurableListableBeanFactory beanFactory) {
        return findBeanNames(beanFactory, BeanDefinitionUtils::isInfrastructureBean);
    }

    public static Set<String> findBeanNames(ConfigurableListableBeanFactory beanFactory, Predicate<? super BeanDefinition>... predicates) {
        Predicate<? super BeanDefinition> predicate = and(predicates);
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
     * Determine whether the {@link AbstractBeanDefinition#setInstanceSupplier(Supplier)} method is present
     *
     * @return <code>true</code> if the {@link AbstractBeanDefinition#setInstanceSupplier(Supplier)} method is present,
     * <code>false</code> otherwise
     * @see #SET_INSTANCE_SUPPLIER_METHOD_HANDLE
     */
    public static boolean isSetInstanceSupplierMethodPresent() {
        return SET_INSTANCE_SUPPLIER_METHOD_HANDLE != null;
    }

    /**
     * Determine whether the {@link AbstractBeanDefinition#getInstanceSupplier()} method is present
     *
     * @return <code>true</code> if the {@link AbstractBeanDefinition#getInstanceSupplier()} method is present,
     * <code>false</code> otherwise
     * @see #GET_INSTANCE_SUPPLIER_METHOD_HANDLE
     */
    public static boolean isGetInstanceSupplierMethodPresent() {
        return GET_INSTANCE_SUPPLIER_METHOD_HANDLE != null;
    }

    /**
     * Determine whether the {@link AbstractBeanDefinition#getResolvableType()} method is present
     *
     * @return <code>true</code> if the {@link AbstractBeanDefinition#getResolvableType()} method is present,
     * <code>false</code> otherwise
     * @see #GET_RESOLVABLE_TYPE_METHOD_HANDLE
     */
    public static boolean isGetResolvableTypeMethodPresent() {
        return GET_RESOLVABLE_TYPE_METHOD_HANDLE != null;
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
     * Get {@link ResolvableType} from {@link RootBeanDefinition}
     *
     * @param rootBeanDefinition {@link RootBeanDefinition}
     * @return {@link ResolvableType#NONE} if the bean definition can't be resolved
     * @see RootBeanDefinition#getResolvableType()
     */
    @Nonnull
    public static ResolvableType getResolvableType(RootBeanDefinition rootBeanDefinition) {
        MethodHandle methodHandle = GET_RESOLVABLE_TYPE_METHOD_HANDLE;
        if (methodHandle == null) {
            return doGetResolvableType(rootBeanDefinition);
        }
        ResolvableType resolvableType = null;
        try {
            resolvableType = (ResolvableType) methodHandle.invokeExact(rootBeanDefinition);
        } catch (Throwable e) {
            handleInvokeExactFailure(e, methodHandle, rootBeanDefinition);
            resolvableType = doGetResolvableType(rootBeanDefinition);
        }
        return resolvableType;
    }

    /**
     * Set the {@link Supplier} reference of bean instance for {@link AbstractBeanDefinition}
     *
     * @param beanDefinition   {@link AbstractBeanDefinition}
     * @param instanceSupplier {@link Supplier} for bean instance
     * @return <code>true</code> if set successfully, <code>false</code> otherwise
     */
    public static boolean setInstanceSupplier(AbstractBeanDefinition beanDefinition, @Nullable Supplier<?> instanceSupplier) {
        MethodHandle methodHandle = SET_INSTANCE_SUPPLIER_METHOD_HANDLE;
        if (methodHandle == null || instanceSupplier == null) {
            return false;
        }
        try {
            methodHandle.invokeExact(beanDefinition, instanceSupplier);
        } catch (Throwable e) {
            handleInvokeExactFailure(e, methodHandle, beanDefinition, instanceSupplier);
        }
        return true;
    }

    /**
     * Get the {@link Supplier} reference of bean instance for {@link AbstractBeanDefinition}
     *
     * @param beanDefinition {@link AbstractBeanDefinition}
     * @return <code>null</code> if not found
     */
    @Nullable
    public static Supplier<?> getInstanceSupplier(AbstractBeanDefinition beanDefinition) {
        MethodHandle methodHandle = GET_INSTANCE_SUPPLIER_METHOD_HANDLE;
        if (methodHandle == null) {
            return null;
        }
        Supplier<?> supplier = null;
        try {
            supplier = (Supplier<?>) methodHandle.invokeExact(beanDefinition);
        } catch (Throwable e) {
            handleInvokeExactFailure(e, methodHandle, beanDefinition);
        }
        return supplier;
    }

    /**
     * Compatible with {@link RootBeanDefinition#getResolvableType()) since Spring Framework 5.1
     *
     * @param rootBeanDefinition {@link RootBeanDefinition}
     * @return
     */
    protected static ResolvableType doGetResolvableType(RootBeanDefinition rootBeanDefinition) {
        if (rootBeanDefinition == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("The argument of RootBeanDefinition is null");
            }
            return NONE;
        }
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
        return beanDefinition.hasBeanClass() ? forClass(beanDefinition.getBeanClass()) : NONE;
    }

    private BeanDefinitionUtils() {
    }
}
