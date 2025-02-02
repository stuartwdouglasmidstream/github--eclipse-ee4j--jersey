/*
 * Copyright (c) 2015, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.bv;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Context;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.ValidationInterceptor;
import org.glassfish.jersey.server.spi.ValidationInterceptorContext;

/**
 * HK2 managed validation interceptor.
 */
@Vetoed
public class Hk2ValidationInterceptor implements ValidationInterceptor {


    private final Provider<Hk2ValidationResult> validationResult;

    public Hk2ValidationInterceptor(Provider<Hk2ValidationResult> validationResult) {
        this.validationResult = validationResult;
    }

    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindFactory(ValidationInterceptorFactory.class, Singleton.class)
                    .to(ValidationInterceptor.class);
        }

    }

    private static class ValidationInterceptorFactory implements Supplier<ValidationInterceptor> {

        @Inject
        Provider<Hk2ValidationResult> validationResultProvider;

        @Override
        public ValidationInterceptor get() {
            return new Hk2ValidationInterceptor(validationResultProvider);
        }
    }

    @Override
    public void onValidate(
            ValidationInterceptorContext ctx) throws ValidationException {
        try {
            ctx.proceed();
        } catch (ConstraintViolationException ex) {
            ensureValidationResultInjected(ctx, ex);
            validationResult.get().setViolations(ex.getConstraintViolations());
        }
    }

    private void ensureValidationResultInjected(
            final ValidationInterceptorContext ctx, final ConstraintViolationException ex) {

        if (!isValidationResultInArgs(ctx.getArgs())
                && !isValidationResultInResource(ctx)
                && !hasValidationResultProperty(ctx.getResource())) {

            throw ex;
        }
    }

    private boolean isValidationResultInResource(ValidationInterceptorContext ctx) {
        Class<?> clazz = ctx.getResource().getClass();
        do {
            for (Field f : clazz.getDeclaredFields()) {
                // Of ValidationResult and JAX-RS injectable
                if (ValidationResult.class.isAssignableFrom(f.getType())
                        && f.getAnnotation(Context.class) != null) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return false;
    }

    private boolean isValidationResultInArgs(Object[] args) {
        for (Object a : args) {
            if (a != null && ValidationResult.class.isAssignableFrom(a.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a resource has a property of type {@code javax.mvc.validation.ValidationResult}.
     *
     * @param resource resource instance.
     * @return outcome of test.
     */
    public static boolean hasValidationResultProperty(final Object resource) {
        return getValidationResultGetter(resource) != null && getValidationResultSetter(resource) != null;
    }

    /**
     * Returns a getter for {@code javax.mvc.validation.ValidationResult} or {@code null}
     * if one cannot be found.
     *
     * @param resource resource instance.
     * @return getter or {@code null} if not available.
     */
    public static Method getValidationResultGetter(final Object resource) {
        Class<?> clazz = resource.getClass();
        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (isValidationResultGetter(m)) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return null;
    }

    /**
     * Determines if a method is a getter for {@code javax.mvc.validation.ValidationResult}.
     *
     * @param m method to test.
     * @return outcome of test.
     */
    private static boolean isValidationResultGetter(Method m) {
        return m.getName().startsWith("get")
                && ValidationResult.class.isAssignableFrom(m.getReturnType())
                && Modifier.isPublic(m.getModifiers()) && m.getParameterTypes().length == 0;
    }

    /**
     * Returns a setter for {@code javax.mvc.validation.ValidationResult} or {@code null}
     * if one cannot be found.
     *
     * @param resource resource instance.
     * @return setter or {@code null} if not available.
     */
    public static Method getValidationResultSetter(final Object resource) {
        Class<?> clazz = resource.getClass();
        do {
            for (Method m : clazz.getDeclaredMethods()) {
                if (isValidationResultSetter(m)) {
                    return m;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return null;
    }

    /**
     * Determines if a method is a setter for {@code javax.mvc.validation.ValidationResult}.
     * As a CDI initializer method, it must be annotated with {@link jakarta.inject.Inject}.
     *
     * @param m method to test.
     * @return outcome of test.
     */
    private static boolean isValidationResultSetter(Method m) {
        return m.getName().startsWith("set") && m.getParameterTypes().length == 1
                && ValidationResult.class.isAssignableFrom(m.getParameterTypes()[0])
                && m.getReturnType() == Void.TYPE && Modifier.isPublic(m.getModifiers())
                && m.getAnnotation(Context.class) != null;
    }

}
