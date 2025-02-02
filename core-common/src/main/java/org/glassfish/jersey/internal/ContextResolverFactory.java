/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;

import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.internal.util.collection.KeyComparatorHashMap;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.spi.ContextResolvers;

/**
 * A factory implementation for managing {@link ContextResolver} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar
 */
public class ContextResolverFactory implements ContextResolvers {

    /**
     * Configurator which initializes and register {@link ContextResolvers} instance into {@link InjectionManager} and
     * {@link BootstrapBag}.
     *
     * @author Petr Bouda
     */
    public static class ContextResolversConfigurator implements BootstrapConfigurator {

        private ContextResolverFactory contextResolverFactory;

        @Override
        public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            contextResolverFactory = new ContextResolverFactory();
            InstanceBinding<ContextResolverFactory> binding =
                    Bindings.service(contextResolverFactory)
                            .to(ContextResolvers.class);
            injectionManager.register(binding);
        }

        @Override
        public void postInit(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            contextResolverFactory.initialize(injectionManager.getAllInstances(ContextResolver.class));
            bootstrapBag.setContextResolvers(contextResolverFactory);
        }
    }

    private final Map<Type, Map<MediaType, ContextResolver>> resolver = new HashMap<>(3);
    private final Map<Type, ConcurrentHashMap<MediaType, ContextResolver>> cache = new HashMap<>(3);

    /**
     * Private constructor to allow to create {@link ContextResolverFactory} only in {@link ContextResolversConfigurator}.
     */
    private ContextResolverFactory(){
    }

    private void initialize(List<ContextResolver> contextResolvers) {
        Map<Type, Map<MediaType, List<ContextResolver>>> rs = new HashMap<>();

        for (ContextResolver provider : contextResolvers) {
            List<MediaType> ms = MediaTypes.createFrom(provider.getClass().getAnnotation(Produces.class));
            Type type = getParameterizedType(provider.getClass());

            Map<MediaType, List<ContextResolver>> mr = rs.get(type);
            if (mr == null) {
                mr = new HashMap<>();
                rs.put(type, mr);
            }
            for (MediaType m : ms) {
                List<ContextResolver> crl = mr.get(m);
                if (crl == null) {
                    crl = new ArrayList<>();
                    mr.put(m, crl);
                }
                crl.add(provider);
            }
        }

        // Reduce set of two or more context resolvers for same type and
        // media type

        for (Map.Entry<Type, Map<MediaType, List<ContextResolver>>> e : rs.entrySet()) {
            Map<MediaType, ContextResolver> mr = new KeyComparatorHashMap<>(4, MessageBodyFactory.MEDIA_TYPE_KEY_COMPARATOR);
            resolver.put(e.getKey(), mr);
            cache.put(e.getKey(), new ConcurrentHashMap<>(4));

            for (Map.Entry<MediaType, List<ContextResolver>> f : e.getValue().entrySet()) {
                mr.put(f.getKey(), reduce(f.getValue()));
            }
        }
    }

    private Type getParameterizedType(final Class<?> c) {
        final DeclaringClassInterfacePair p = ReflectionHelper.getClass(
                c, ContextResolver.class);

        final Type[] as = ReflectionHelper.getParameterizedTypeArguments(p);

        return (as != null) ? as[0] : Object.class;
    }

    private static final NullContextResolverAdapter NULL_CONTEXT_RESOLVER =
            new NullContextResolverAdapter();

    private static final class NullContextResolverAdapter implements ContextResolver {

        @Override
        public Object getContext(final Class type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final class ContextResolverAdapter implements ContextResolver {

        private final ContextResolver[] cra;

        ContextResolverAdapter(final ContextResolver... cra) {
            this(removeNull(cra));
        }

        ContextResolverAdapter(final List<ContextResolver> crl) {
            this.cra = crl.toArray(new ContextResolver[crl.size()]);
        }

        @Override
        public Object getContext(final Class objectType) {
            for (final ContextResolver cr : cra) {
                @SuppressWarnings("unchecked") final Object c = cr.getContext(objectType);
                if (c != null) {
                    return c;
                }
            }
            return null;
        }

        ContextResolver reduce() {
            if (cra.length == 0) {
                return NULL_CONTEXT_RESOLVER;
            }
            if (cra.length == 1) {
                return cra[0];
            } else {
                return this;
            }
        }

        private static List<ContextResolver> removeNull(final ContextResolver... cra) {
            final List<ContextResolver> crl = new ArrayList<>(cra.length);
            for (final ContextResolver cr : cra) {
                if (cr != null) {
                    crl.add(cr);
                }
            }
            return crl;
        }
    }

    private ContextResolver reduce(final List<ContextResolver> r) {
        if (r.size() == 1) {
            return r.iterator().next();
        } else {
            return new ContextResolverAdapter(r);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ContextResolver<T> resolve(final Type t, MediaType m) {
        final ConcurrentHashMap<MediaType, ContextResolver> crMapCache = cache.get(t);
        if (crMapCache == null) {
            return null;
        }

        if (m == null) {
            m = MediaType.WILDCARD_TYPE;
        }

        ContextResolver<T> cr = crMapCache.get(m);
        if (cr == null) {
            final Map<MediaType, ContextResolver> crMap = resolver.get(t);

            if (m.isWildcardType()) {
                cr = crMap.get(MediaType.WILDCARD_TYPE);
                if (cr == null) {
                    cr = NULL_CONTEXT_RESOLVER;
                }
            } else if (m.isWildcardSubtype()) {
                // Include x, x/* and */*
                final ContextResolver<T> subTypeWildCard = crMap.get(m);
                final ContextResolver<T> wildCard = crMap.get(MediaType.WILDCARD_TYPE);

                cr = new ContextResolverAdapter(subTypeWildCard, wildCard).reduce();
            } else {
                // Include x, x/* and */*
                final ContextResolver<T> type = crMap.get(m);
                final ContextResolver<T> subTypeWildCard = crMap.get(new MediaType(m.getType(), "*"));
                final ContextResolver<T> wildCard = crMap.get(MediaType.WILDCARD_TYPE);

                cr = new ContextResolverAdapter(type, subTypeWildCard, wildCard).reduce();
            }

            final ContextResolver<T> _cr = crMapCache.putIfAbsent(m, cr);
            // If there is already a value in the cache use that
            // instance, and discard the new and redundant instance, to
            // ensure the same instance is always returned.
            // The cached instance and the new instance will have the same
            // functionality.
            if (_cr != null) {
                cr = _cr;
            }
        }

        return (cr != NULL_CONTEXT_RESOLVER) ? cr : null;
    }
}
