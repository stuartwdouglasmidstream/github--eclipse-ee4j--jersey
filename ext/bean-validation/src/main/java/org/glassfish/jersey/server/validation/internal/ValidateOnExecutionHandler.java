/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.validation.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import jakarta.validation.Configuration;
import jakarta.validation.ValidationException;
import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;

import org.glassfish.jersey.internal.guava.Multimap;
import org.glassfish.jersey.internal.guava.Multimaps;
import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Handler providing methods to determine whether an executable should be validated during the validation process based on the
 * presence of {@link ValidateOnExecution} annotation.
 *
 * @author Michal Gajdos
 */
class ValidateOnExecutionHandler {

    private final ConcurrentMap<Method, Boolean> validateMethodCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Method, Boolean> validateGetterCache = new ConcurrentHashMap<>();

    private final Configuration config;
    private final boolean checkOverrides;

    /**
     * Create {@link ValidateOnExecutionHandler}.
     *
     * @param config validation configuration to obtain bootstrap config.
     * @param checkOverrides flag whether overriding/implementing methods should be checked if the {@link ValidateOnExecution}
     * annotation is present.
     */
    ValidateOnExecutionHandler(final Configuration config, final boolean checkOverrides) {
        this.config = config;
        this.checkOverrides = checkOverrides;
    }

    /**
     * Determine whether the given {@link Method getter} on the given {@link Class clazz} should be validated during the
     * resource class validation. See {@code #validateMethod} to understand the difference between this and {@code
     * #validateMethod}.
     *
     * @param clazz class on which the getter will be invoked.
     * @param method method to be examined.
     * @return {@code true} if the getter should be validated, {@code false} otherwise.
     */
    boolean validateGetter(final Class<?> clazz, final Method method) {
        if (!validateGetterCache.containsKey(method)) {
            processMethod(clazz, method, method, true);
        }
        return validateGetterCache.get(method);
    }

    /**
     * Determine whether the given resource {@link Method method} to-be-executed on the given {@link Class clazz} should be
     * validated. The difference between this and {@code #validateGetter} method is that this method returns {@code true} if the
     * {@code method} is getter and validating getter method is not explicitly disabled by {@link ValidateOnExecution} annotation
     * in the class hierarchy.
     *
     * @param clazz class on which the method will be invoked.
     * @param method method to be examined.
     * @param validationMethod method used for cache.
     * @return {@code true} if the method should be validated, {@code false} otherwise.
     */
    boolean validateMethod(final Class<?> clazz, final Method method, final Method validationMethod) {
        if (!validateMethodCache.containsKey(validationMethod)) {
            processMethod(clazz, method, validationMethod, false);
        }
        return validateMethodCache.get(validationMethod);
    }

    /**
     * Process the given {@code method} and {@code validationMethod} on given {@code clazz} and determine whether this method
     * should be validated or not.
     *
     * @param clazz class on which the method will be invoked.
     * @param method method to be examined.
     * @param validationMethod method used for cache.
     * @param forceValidation forces validation of a getter if no {@link ValidateOnExecution} annotation is present.
     */
    private void processMethod(final Class<?> clazz, final Method method, final Method validationMethod,
                               final boolean forceValidation) {
        final Deque<Class<?>> hierarchy = getValidationClassHierarchy(clazz);
        Boolean validateMethod = processAnnotation(method, hierarchy, checkOverrides);

        if (validateMethod != null) {
            validateMethodCache.putIfAbsent(validationMethod, validateMethod);
            validateGetterCache.putIfAbsent(validationMethod, validateMethod);
        }

        // Return value from validation.xml.
        if (!validateMethodCache.containsKey(validationMethod)) {
            final Set<ExecutableType> defaultValidatedExecutableTypes = config.getBootstrapConfiguration()
                    .getDefaultValidatedExecutableTypes();
            validateMethod = validateMethod(method, false, defaultValidatedExecutableTypes);

            validateGetterCache.putIfAbsent(validationMethod, validateMethod || forceValidation);

            // When validateMethod is called and no ValidateOnExecution annotation is present we want to validate getter resource
            // methods by default (see SPEC).
            validateMethodCache.putIfAbsent(validationMethod, ReflectionHelper.isGetter(validationMethod) || validateMethod);
        }
    }

    /**
     * Process {@link ValidateOnExecution} annotation for given method on a class hierarchy.
     *
     * @param method method to be examined.
     * @param hierarchy class hierarchy to be examined.
     * @param checkOverrides flag whether a overriding/implementing methods should also be checked.
     * @return {@code true} if the method should be validated, {@code false} if not, {@code null} if the flag cannot be
     * determined (no annotation present).
     */
    private Boolean processAnnotation(final Method method, final Deque<Class<?>> hierarchy, final boolean checkOverrides) {
        // Overridden methods.
        while (!hierarchy.isEmpty()) {
            final Class<?> overriddenClass = hierarchy.removeFirst();
            final Method overriddenMethod =
                    AccessController.doPrivileged(ReflectionHelper.findMethodOnClassPA(overriddenClass, method));

            if (overriddenMethod != null) {
                // Method.
                Set<ExecutableType> executableTypes = getExecutableTypes(overriddenMethod);
                if (!executableTypes.isEmpty()) {

                    // If an overriding/implementing method is annotated with @ValidateOnExecution, throw an exception.
                    if (checkOverrides
                            && processAnnotation(method, hierarchy, false) != null) {
                        final String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
                        throw new ValidationException(LocalizationMessages.OVERRIDE_CHECK_ERROR(methodName));
                    }

                    return validateMethod(overriddenMethod, true, executableTypes);
                }

                // Class.
                executableTypes = getExecutableTypes(overriddenClass);
                if (!executableTypes.isEmpty()
                        // It should contain not only ExecutableType#IMPLICIT but something else as well.
                        // ExecutableType#IMPLICIT on class itself does nothing.
                        && !(executableTypes.size() == 1 && executableTypes.contains(ExecutableType.IMPLICIT))) {

                    return validateMethod(overriddenMethod, false, executableTypes);
                }
            }
        }
        return null;
    }

    /**
     * Determine whether the given {@link Method method} should be validated depending on the given {@code executableTypes}.
     *
     * @param method method to be examined.
     * @param allowImplicit allows check for {@link ExecutableType#IMPLICIT} type.
     * @param executableTypes executable types assigned to the method.
     * @return {@code true} if the method should be validated, {@code false otherwise}.
     */
    private boolean validateMethod(final Method method, final boolean allowImplicit, final Set<ExecutableType> executableTypes) {
        if (executableTypes.contains(ExecutableType.ALL)
                || (allowImplicit && executableTypes.contains(ExecutableType.IMPLICIT))) {
            return true;
        }
        return ReflectionHelper.isGetter(method) ? executableTypes.contains(ExecutableType.GETTER_METHODS)
                : executableTypes.contains(ExecutableType.NON_GETTER_METHODS);
    }

    /**
     * Return a set of executable types contained in {@link ValidateOnExecution} annotation belonging to the {@code element}.
     *
     * @param element element to be examined for {@link ValidateOnExecution}.
     * @return set of executable types or an empty set if the element is not annotated with {@link ValidateOnExecution}.
     */
    private Set<ExecutableType> getExecutableTypes(final AnnotatedElement element) {
        final ValidateOnExecution validateExecutable = element.getAnnotation(ValidateOnExecution.class);
        return validateExecutable != null
                ? Arrays.stream(validateExecutable.type()).collect(Collectors.toSet())
                : Collections.emptySet();
    }

    /**
     * Get a class hierarchy for the given {@code clazz} suitable to be looked for {@link ValidateOnExecution} annotation
     * in order according to the priority defined by Bean Validation spec (superclasses, interfaces).
     *
     * @param clazz class to obtain hierarchy for.
     * @return class hierarchy.
     */
    private Deque<Class<?>> getValidationClassHierarchy(final Class<?> clazz) {
        final List<Class<?>> hierarchy = new ArrayList<>();

        // Get all superclasses.
        for (Class<?> currentClass = clazz; currentClass != Object.class; currentClass = currentClass.getSuperclass()) {
            hierarchy.add(clazz);
        }

        hierarchy.addAll(getAllValidationInterfaces(clazz));
        Collections.reverse(hierarchy);

        return new ArrayDeque<>(hierarchy);
    }

    private List<Class<?>> getAllValidationInterfaces(final Class<?> clazz) {
        final Multimap<Integer, Class<?>> map = Multimaps.newListMultimap(
                new TreeMap<Integer, Collection<Class<?>>>(), ArrayList::new);

        retrieveAllValidationInterfaces(clazz, map);

        final List<Class<?>> interfaces = new ArrayList<>(map.values());
        Collections.reverse(interfaces);

        return interfaces;
    }

    private int retrieveAllValidationInterfaces(Class<?> clazz, final Multimap<Integer, Class<?>> map) {
        if (clazz == null) {
            return 0;
        }

        int minDepth = 0;

        while (clazz != null) {
            for (final Class<?> iface : clazz.getInterfaces()) {
                int depth = retrieveAllValidationInterfaces(iface, map);

                if (!map.containsValue(iface)) {
                    map.put(depth, iface);
                }
                minDepth = minDepth > depth ? depth : minDepth;
            }

            clazz = clazz.getSuperclass();
        }

        return minDepth + 1;
    }
}
