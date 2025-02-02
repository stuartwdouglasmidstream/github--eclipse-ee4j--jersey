/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.inject.weld.internal.injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Application;

import jakarta.enterprise.context.RequestScoped;

import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;

/**
 * Class working with JAX-RS/Jersey types injected using {@link jakarta.ws.rs.core.Context} annotation and all other types which
 * can be injected using using other {@code *Param} annotations.
 * <p>
 * Processed JAX-RS interfaces:
 *
 * @author Petr Bouda
 * @see jakarta.ws.rs.core.UriInfo
 * @see jakarta.ws.rs.core.Request
 * @see jakarta.ws.rs.core.HttpHeaders
 * @see jakarta.ws.rs.core.SecurityContext
 * @see jakarta.ws.rs.core.Configuration
 * @see jakarta.ws.rs.core.Application not proxiable because is registered as a singleton.
 * @see jakarta.ws.rs.ext.Providers
 */
class JerseyProxyResolver {

    /**
     * Contains already created proxies for the given type.
     * e.g. if the proxy has been already created for {@code UriInfo} don't create a new one and reuse the existing one.
     */
    private final ConcurrentHashMap<AnnotatedElement, Object> cachedProxies = new ConcurrentHashMap<>();

    /**
     * Classes in which is not needed to use a proxy because they are singletons.
     */
    private static final List<Class<?>> IGNORED_CLASSES = Collections.singletonList(Application.class);

    /**
     * Returns {@code true} if one of the proxiable annotations is present on the clazz into which values are injected.
     * <p>
     * In these cases the value is not proxiable:
     * <ul>
     * <li>Class without the annotation</li>
     * <li>Class annotated by {@link jakarta.enterprise.context.RequestScoped}</li>
     * <li>Class annotated by {@link org.glassfish.jersey.process.internal.RequestScoped}</li>
     * <ul/>
     *
     * @param injectee information about the injection point.
     * @return {@code true} if contains one proxiable annotation at least.
     */
    public boolean isProxiable(Injectee injectee) {
        return !ignoredClass(injectee.getRequiredType()) && isProxiable(injectee.getParentClassScope());
    }

    /**
     * Returns {@code true} if one of the proxiable annotations is present on the clazz.
     * <p>
     * In these cases the value is not proxiable:
     * <ul>
     * <li>Class without the annotation</li>
     * <li>Class annotated by {@link jakarta.enterprise.context.RequestScoped}</li>
     * <li>Class annotated by {@link org.glassfish.jersey.process.internal.RequestScoped}</li>
     * <ul/>
     *
     * @param scopeAnnotation annotation belonging to the scope of the class.
     * @return {@code true} if contains one proxiable annotation at least.
     */
    public boolean isProxiable(Class<? extends Annotation> scopeAnnotation) {
        return ignoreProxy().stream().noneMatch(ignoredAnnotation -> ignoredAnnotation == scopeAnnotation);
    }

    /**
     * Returns a proxy (newly created or cached) which is able to call {@link InjectionResolver} with the given {@link Injectee}
     * to get the value in proper scope.
     *
     * @param injectee information about the injection point.
     * @param resolver dedicated resolver which find the value.
     * @return created proxy which resolve the value in the proper scope.
     */
    public Object proxy(Injectee injectee, InjectionResolver resolver) {
        return cachedProxies.computeIfAbsent(injectee.getParent(), type -> createProxy(injectee, resolver));
    }

    /**
     * Returns a proxy (newly created or cached) which is able to call the given {@link Supplier}. This method does not cache
     * a result.
     *
     * @param injectee information about the injection point.
     * @param supplier supplier called using the proxy.
     * @return created proxy which resolve the value in the proper scope.
     */
    public Object noCachedProxy(Injectee injectee, Supplier<Object> supplier) {
        return createProxy(getClass(injectee.getRequiredType()), supplier);
    }

    private Object createProxy(Injectee injectee, InjectionResolver resolver) {
        return createProxy(getClass(injectee.getRequiredType()), () -> resolver.resolve(injectee));
    }

    private Object createProxy(Class<?> requiredClass, Supplier<Object> supplier) {
        return Proxy.newProxyInstance(
                requiredClass.getClassLoader(),
                new Class<?>[] {requiredClass},
                new JerseyInvocationHandler(supplier));
    }

    private Class<?> getClass(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return (Class<?>) paramType.getRawType();
        }

        return (Class<?>) type;
    }

    /**
     * Returns all annotations for which proxy will be created.
     *
     * @return all proxyable annotations.
     */
    private Collection<Class<? extends Annotation>> ignoreProxy() {
        return Arrays.asList(RequestScoped.class, org.glassfish.jersey.process.internal.RequestScoped.class);
    }

    /**
     * Classes in which is not needed to use a proxy because they are singletons.
     *
     * @return classes omitted during proxying.
     */
    private boolean ignoredClass(Type type) {
        Class<?> clazz;
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            clazz = (Class<?>) paramType.getRawType();
        } else {
            clazz = (Class<?>) type;
        }

        return IGNORED_CLASSES.contains(clazz);
    }

    /**
     * {@link InvocationHandler} to intercept a calling using a proxy by providing a value of the given injected type in a proper
     * scope.
     */
    private static class JerseyInvocationHandler implements InvocationHandler {

        private final Supplier<Object> supplier;

        /**
         * Creates a new invocation handler with supplier which provides a current injected value in proper scope.
         *
         * @param supplier provider of the value.
         */
        private JerseyInvocationHandler(Supplier<Object> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object target = supplier.get();
            return method.invoke(target, args);
        }
    }
}
