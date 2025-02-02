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
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.util.Pretty;
import org.glassfish.jersey.internal.util.collection.ImmutableCollectors;

/**
 * Utility class for processing of an injection.
 *
 * @author Petr Bouda
 */
public final class InjectionUtils {

    /**
     * Forbids the creation of {@link InjectionUtils} instance.
     */
    private InjectionUtils() {
    }

    /**
     * Just injects the thing, doesn't try to do anything else
     *
     * @param injectMe      the object to inject into.
     * @param bean          information about the injected instance.
     * @param resolvers     all injection resolvers registered in the application.
     * @param proxyResolver object which is able to create a proxy.
     * @param <T>           type of the injected instance.
     */
    static <T> void justInject(T injectMe, Bean<T> bean, Map<Field, InjectionResolver> resolvers,
            JerseyProxyResolver proxyResolver) {
        if (injectMe == null) {
            throw new IllegalArgumentException();
        }

        for (Map.Entry<Field, InjectionResolver> entry : resolvers.entrySet()) {
            Field field = entry.getKey();
            InjectionResolver resolver = entry.getValue();
            Injectee injectee = InjectionUtils.getFieldInjectee(bean, field);

            Object resolvedValue;
            if (injectee.isProvider()) {
                resolvedValue = (Provider<Object>) () -> resolver.resolve(injectee);
            } else if (proxyResolver.isProxiable(injectee)) {
                resolvedValue = proxyResolver.proxy(injectee, resolver);
            } else {
                resolvedValue = resolver.resolve(injectee);
            }

            try {
                ReflectionUtils.setField(field, injectMe, resolvedValue);
            } catch (MultiException me) {
                throw me;
            } catch (Throwable th) {
                throw new MultiException(th);
            }
        }
    }

    /**
     * Returns the injectee for a field.
     *
     * @param bean bean in which the field is placed.
     * @param field      the field to analyze.
     * @return the list (in order) of parameters to the constructor.
     */
    private static Injectee getFieldInjectee(Bean<?> bean, Field field) {
        Set<Annotation> annotations = Arrays.stream(field.getAnnotations())
                .collect(ImmutableCollectors.toImmutableSet());

        Type adjustedType = ReflectionUtils.resolveField(bean.getBeanClass(), field);

        InjecteeImpl injectee = new InjecteeImpl();
        injectee.setParentClassScope(bean.getScope());

        if (isProvider(adjustedType)) {
            ParameterizedType paramType = (ParameterizedType) adjustedType;
            injectee.setRequiredType(paramType.getActualTypeArguments()[0]);
            injectee.setProvider(true);
        } else {
            injectee.setRequiredType(adjustedType);
        }

        injectee.setParent(field);
        injectee.setRequiredQualifiers(getFieldAdjustedQualifierAnnotations(field, annotations));
        return injectee;
    }

    public static boolean isProvider(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return Provider.class.isAssignableFrom((Class<?>) paramType.getRawType());
        }

        return false;
    }

    private static Set<Annotation> getFieldAdjustedQualifierAnnotations(Field field, Set<Annotation> qualifiers) {
        Named n = field.getAnnotation(Named.class);
        if (n == null || !"".equals(n.value())) {
            return qualifiers;
        }

        HashSet<Annotation> retVal = new HashSet<>();
        for (Annotation qualifier : qualifiers) {
            if (qualifier.annotationType().equals(Named.class)) {
                retVal.add(new NamedImpl(field.getName()));
            } else {
                retVal.add(qualifier);
            }
        }

        return retVal;
    }

    /**
     * Gets the fields from the given class and analyzer. Checks service output.
     *
     * @param clazz             the non-null impl class.
     * @param injectAnnotations all annotations which can be used to inject a value.
     * @param errors            for gathering errors.  @return a non-null set (even in error cases, check the collector).
     */
    static Set<Field> getFields(Class<?> clazz, Set<? extends Class<?>> injectAnnotations, Collector errors) {
        Set<Field> retVal;

        try {
            retVal = getFieldsInternal(clazz, injectAnnotations, errors);
        } catch (MultiException me) {
            errors.addMultiException(me);
            return Collections.emptySet();
        } catch (Throwable th) {
            errors.addThrowable(th);
            return Collections.emptySet();
        }

        return retVal;
    }

    /**
     * Will find all the initialize fields in the class.
     *
     * @param clazz             the class to search for fields
     * @param injectAnnotations all annotations which can be used to inject a value.
     * @param errors            the error collector
     * @return A non-null but possibly empty set of initializer fields
     */
    private static Set<Field> getFieldsInternal(Class<?> clazz, Set<? extends Class<?>> injectAnnotations, Collector errors) {
        Set<Field> retVal = new LinkedHashSet<>();

        for (Field field : ReflectionUtils.getAllFields(clazz)) {
            if (!hasInjectAnnotation(field, injectAnnotations)) {
                // Not an initializer field
                continue;
            }

            if (!isProperField(field)) {
                errors.addThrowable(new IllegalArgumentException("The field " + Pretty.field(field)
                        + " may not be static, final or have an Annotation type"));
                continue;
            }

            retVal.add(field);
        }

        return retVal;
    }

    /**
     * Checks whether an annotated element has any annotation that was used for the injection.
     *
     * @param annotated         the annotated element.
     * @param injectAnnotations all annotations which can be used to inject a value.
     * @return True if element contains at least one inject annotation.
     */
    private static boolean hasInjectAnnotation(AnnotatedElement annotated, Set<? extends Class<?>> injectAnnotations) {
        for (Annotation annotation : annotated.getAnnotations()) {
            if (injectAnnotations.contains(annotation.annotationType())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isProperField(Field field) {
        if (isStatic(field) || isFinal(field)) {
            return false;
        }

        Class<?> type = field.getType();
        return !type.isAnnotation();
    }

    /**
     * Returns true if the underlying member is static.
     *
     * @param member The non-null member to test.
     * @return true if the member is static.
     */
    private static boolean isStatic(Member member) {
        int modifiers = member.getModifiers();
        return ((modifiers & Modifier.STATIC) != 0);
    }

    /**
     * Returns true if the underlying member is abstract.
     *
     * @param member The non-null member to test.
     * @return true if the member is abstract.
     */
    private static boolean isFinal(Member member) {
        int modifiers = member.getModifiers();
        return ((modifiers & Modifier.FINAL) != 0);
    }

    /**
     * Returns all annotations that can be managed using registered and provided {@link InjectionResolver injection resolvers}.
     *
     * @param resolvers all registered resolvers.
     * @return all possible injection annotations.
     */
    @SuppressWarnings("unchecked")
    public static Collection<Class<? extends Annotation>> getInjectAnnotations(Collection<InjectionResolver> resolvers) {
        List<Class<? extends Annotation>> annotations = new ArrayList<>();
        for (InjectionResolver resolver : resolvers) {
            annotations.add(resolver.getAnnotation());
        }
        return annotations;
    }

    /**
     * Assigns {@link InjectionResolver} to every {@link AnnotatedElement} provided as a method parameter. Injection resolver
     * will be used for fetching the proper value during the injection processing.
     *
     * @param annotatedElements all annotated elements from the class which this injector belongs to.
     * @param resolvers         all registered injection resolvers.
     * @param <A>               type of the annotated elements.
     * @return immutable map of all fields along with injection resolvers using that can be injected.
     */
    static <A extends AnnotatedElement> Map<A, InjectionResolver> mapElementToResolver(Set<A> annotatedElements,
            Map<? extends Class<?>, InjectionResolver> resolvers) {

        Map<A, InjectionResolver> mappedElements = new HashMap<>();
        for (A element : annotatedElements) {
            mappedElements.put(element, findResolver(resolvers, element));
        }
        return mappedElements;
    }

    static InjectionResolver findResolver(Map<? extends Class<?>, InjectionResolver> resolvers, AnnotatedElement element) {
        for (Annotation annotation : element.getAnnotations()) {
            InjectionResolver injectionResolver = resolvers.get(annotation.annotationType());
            if (injectionResolver != null) {
                return injectionResolver;
            }
        }

        return null;
    }

    /**
     * Creates a map from resolvers where the annotation that is handled by resolver is a key and resolver itself is value.
     *
     * @param resolvers collection of resolvers.
     * @return map resolver annotation to resolver.
     */
    static Map<? extends Class<?>, InjectionResolver> mapAnnotationToResolver(Collection<InjectionResolver> resolvers) {
        return resolvers.stream().collect(Collectors.toMap(InjectionResolver::getAnnotation, Function.identity()));
    }
}
