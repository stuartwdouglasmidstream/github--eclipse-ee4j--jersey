/*
 * Copyright (c) 2017, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.jsr166.Flow;
import org.glassfish.jersey.internal.jsr166.SubmissionPublisher;
import org.glassfish.jersey.internal.jsr166.SubmissionPublisherFactory;
import org.glassfish.jersey.internal.jsr166.SubmittableFlowPublisher;


/**
 * Implementation of {@link Flow.Publisher} corresponding to reactive streams specification.
 * <p>
 * Delegates to {@link SubmissionPublisher} repackaged from jsr166 on JDK 8 or to JDK {@code SubmissionPublisher}.
 *
 * @author Adam Lindenthal
 */
public class JerseyPublisher<T> implements Flow.Publisher<T> {

    private static final int DEFAULT_BUFFER_CAPACITY = 256;
    private SubmittableFlowPublisher<T> submissionPublisher = SubmissionPublisherFactory.createSubmissionPublisher();

    private final PublisherStrategy strategy;

    private boolean cascadingClose;

    /**
     * Creates a new JerseyPublisher using the {@link ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run
     * each task), with maximum buffer capacity of {@value DEFAULT_BUFFER_CAPACITY} and default {@link PublisherStrategy},
     * which is {@link PublisherStrategy#BEST_EFFORT}.
     */
    public JerseyPublisher() {
        this(ForkJoinPool.commonPool(), DEFAULT_BUFFER_CAPACITY, PublisherStrategy.BEST_EFFORT);
    }

    /**
     * Creates a new JerseyPublisher using the {@link ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run
     * each task), with maximum buffer capacity of {@value DEFAULT_BUFFER_CAPACITY} and given {@link PublisherStrategy}.
     *
     * @param strategy publisher delivering strategy
     */
    public JerseyPublisher(final PublisherStrategy strategy) {
        this(ForkJoinPool.commonPool(), DEFAULT_BUFFER_CAPACITY, strategy);
    }

    /**
     * Creates a new JerseyPublisher using the given {@link Executor} for async delivery to subscribers, with the default
     * maximum buffer size of {@value DEFAULT_BUFFER_CAPACITY} and default {@link PublisherStrategy}, which is
     * {@link PublisherStrategy#BEST_EFFORT}.
     *
     * @param executor {@code Executor} the executor to use for async delivery,
     *                 supporting creation of at least one independent thread
     * @throws NullPointerException     if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not positive
     */
    public JerseyPublisher(final Executor executor) {
        this(executor, PublisherStrategy.BEST_EFFORT);
    }

    /**
     * Creates a new JerseyPublisher using the given {@link Executor} for async delivery to subscribers, with the default
     * maximum buffer size of {@value DEFAULT_BUFFER_CAPACITY} and given {@link PublisherStrategy}.
     *
     * @param executor {@code Executor} the executor to use for async delivery,
     *                 supporting creation of at least one independent thread
     * @param strategy publisher delivering strategy
     * @throws NullPointerException     if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not positive
     */
    public JerseyPublisher(final Executor executor, final PublisherStrategy strategy) {
        this.strategy = strategy;
        submissionPublisher = SubmissionPublisherFactory.createSubmissionPublisher(executor, DEFAULT_BUFFER_CAPACITY);
    }



    /**
     * Creates a new JerseyPublisher using the {@link ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two, in which case, a new Thread is created to run
     * each task), with specified maximum buffer capacity and default {@link PublisherStrategy}, which is
     * {@link PublisherStrategy#BEST_EFFORT}.
     *
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     */
    public JerseyPublisher(final int maxBufferCapacity) {
        this(ForkJoinPool.commonPool(), maxBufferCapacity, PublisherStrategy.BEST_EFFORT);
    }

    /**
     * Creates a new JerseyPublisher using the given {@link Executor} for async delivery to subscribers, with the given
     * maximum buffer size for each subscriber and given {@link PublisherStrategy}.
     *
     * @param executor          {@code Executor} the executor to use for async delivery,
     *                          supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     *                          subscriber's buffer (the enforced capacity may be rounded up to
     *                          the nearest power of two and/or bounded by the largest value
     *                          supported by this implementation; method {@link #getMaxBufferCapacity}
     *                          returns the actual value)
     * @param strategy          publisher delivering strategy
     * @throws NullPointerException     if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not positive
     */
    public JerseyPublisher(final Executor executor, final int maxBufferCapacity, PublisherStrategy strategy) {
        this.strategy = strategy;
        submissionPublisher = SubmissionPublisherFactory.createSubmissionPublisher(executor, maxBufferCapacity);
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super T> subscriber) {
        submissionPublisher.subscribe(new SubscriberWrapper<T>(subscriber));
    }

    /**
     * Publishes the given item to each current subscriber by asynchronously invoking its onNext method.
     * <p>
     * Blocks uninterruptibly while resources for any subscriber are unavailable.
     *
     * @param data published data
     * @return the estimated maximum lag among subscribers
     * @throws IllegalStateException if closed
     * @throws NullPointerException if data is null
     * @throws java.util.concurrent.RejectedExecutionException if thrown by Executor
     */
    private int submit(final T data) {
        return submissionPublisher.submit(data);
    }

    /**
     * Processes all published items using the given Consumer function. Returns a CompletableFuture that is completed
     * normally when this publisher signals {@code onComplete()}, or completed exceptionally upon any error, or an
     * exception is thrown by the Consumer, or the returned CompletableFuture is cancelled, in which case no further
     * items are processed.
     *
     * @param consumer function to process all published data
     * @return a {@link CompletableFuture} that is completed normally when the publisher signals onComplete,
     * and exceptionally upon any error or cancellation
     * @throws NullPointerException if consumer is null
     */
    public CompletableFuture<Void> consume(final Consumer<? super T> consumer) {
        return submissionPublisher.consume(consumer);
    }

    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its
     * {@link Flow.Subscriber#onNext(Object) onNext} method.
     * The item may be dropped by one or more subscribers if resource
     * limits are exceeded, in which case the given handler (if non-null)
     * is invoked, and if it returns true, retried once.  Other calls to
     * methods in this class by other threads are blocked while the
     * handler is invoked.  Unless recovery is assured, options are
     * usually limited to logging the error and/or issuing an {@link
     * Flow.Subscriber#onError(Throwable) onError}
     * signal to the subscriber.
     * <p>
     * This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     * <p>
     * If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item   the (non-null) item to publish
     * @param onDrop if non-null, the handler invoked upon a drop to a
     *               subscriber, with arguments of the subscriber and item; if it
     *               returns true, an offer is re-attempted (once)
     * @return if negative, the (negative) number of drops; otherwise
     * an estimate of maximum lag
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    private int offer(T item, BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {
        return offer(item, 0, TimeUnit.MILLISECONDS, onDrop);
    }

    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its {@link
     * Flow.Subscriber#onNext(Object) onNext} method,
     * blocking while resources for any subscription are unavailable,
     * up to the specified timeout or until the caller thread is
     * interrupted, at which point the given handler (if non-null) is
     * invoked, and if it returns true, retried once. (The drop handler
     * may distinguish timeouts from interrupts by checking whether
     * the current thread is interrupted.)
     * Other calls to methods in this class by other
     * threads are blocked while the handler is invoked.  Unless
     * recovery is assured, options are usually limited to logging the
     * error and/or issuing an
     * {@link Flow.Subscriber#onError(Throwable) onError}
     * signal to the subscriber.
     * <p>
     * This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     * <p>
     * If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item    the (non-null) item to publish
     * @param timeout how long to wait for resources for any subscriber
     *                before giving up, in units of {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     * @param onDrop  if non-null, the handler invoked upon a drop to a
     *                subscriber, with arguments of the subscriber and item; if it
     *                returns true, an offer is re-attempted (once)
     * @return if negative, the (negative) number of drops; otherwise
     * an estimate of maximum lag
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    private int offer(T item,
                      long timeout,
                      TimeUnit unit,
                      BiPredicate<Flow.Subscriber<? super T>, ? super T> onDrop) {


        BiPredicate<Flow.Subscriber<? super T>, ? super T> callback;

        callback = onDrop == null
                ? this::onDrop
                : (BiPredicate<Flow.Subscriber<? super T>, T>)
                (subscriber, data) -> {
                    onDrop.test(getSubscriberWrapper(subscriber).getWrappedSubscriber(), data);
                    return false;
                };

        return submissionPublisher.offer(item, timeout, unit, callback);
    }

    private boolean onDrop(Flow.Subscriber<? super T> subscriber, T t) {
        subscriber.onError(new IllegalStateException(LocalizationMessages.SLOW_SUBSCRIBER(t)));
        getSubscriberWrapper(subscriber).getSubscription().cancel();
        return false;
    }

    private SubscriberWrapper getSubscriberWrapper(Flow.Subscriber subscriber) {
        if (subscriber instanceof SubscriberWrapper) {
            return ((SubscriberWrapper) subscriber);
        } else {
            throw new IllegalArgumentException(LocalizationMessages.UNKNOWN_SUBSCRIBER());
        }

    }

    /**
     * Publishes the given item to all current subscribers by invoking its {@code onNext() method} using {@code Executor}
     * provided as constructor parameter (or the default {@code Executor} if not provided).
     * <p>
     * Concrete behaviour is specified by {@link PublisherStrategy} selected upon {@code JerseyPublisher} creation.
     *
     * @param item the (non-null) item to publish.
     * @return if negative, the (negative) number of drops; otherwise an estimate of maximum lag.
     * @throws IllegalStateException      if closed
     * @throws NullPointerException       if item is null
     * @throws RejectedExecutionException if thrown by {@code Executor}
     */
    public int publish(T item) {
        if (PublisherStrategy.BLOCKING == strategy) {
            return submit(item);
        } else {
            // PublisherStrategy.BEST_EFFORT
            return submissionPublisher.offer(item, this::onDrop);
        }
    }

    /**
     * Unless already closed, issues {@code onComplete()} signals to current subscribers, and disallows subsequent
     * attempts to publish. Upon return, this method does <em>NOT</em> guarantee that all subscribers have yet
     * completed.
     */
    public void close() {
        close(true);
    }

    /**
     * Same as {@link #close()} but with control as to whether registered subscribers should be
     * closed or not.
     *
     * @param cascading Boolean controlling whether to close subscribers or not.
     */
    public void close(boolean cascading) {
        cascadingClose = cascading;
        submissionPublisher.close();
    }

    /**
     * Issues onError signals to current subscribers with the given error, and disallows subsequent attempts to publish.
     *
     * @param error the {@code onError} argument sent to subscribers
     * @throws NullPointerException if error is null
     */
    public void closeExceptionally(final Throwable error) {
        submissionPublisher.closeExceptionally(error);
    }

    /**
     * Returns an estimate of the maximum number of items produced but not yet consumed among all current subscribers.
     *
     * @return estimated maximum lag
     */
    public int estimateMaximumLag() {
        return submissionPublisher.estimateMaximumLag();
    }

    /**
     * Returns an estimate of the minimum number of items requested but not yet produced, among all current subscribers.
     *
     * @return estimated minimum demand
     */
    public long estimateMinimumDemand() {
        return submissionPublisher.estimateMinimumDemand();
    }

    /**
     * Returns the exception associated with {@link #closeExceptionally}, or null if not closed or if closed normally.
     *
     * @return exception thrown on closing or {@code null}
     */
    public Throwable getClosedException() {
        return submissionPublisher.getClosedException();
    }

    /**
     * Returns the maximum per-subscriber buffer capacity.
     *
     * @return the maximum per-subscriber buffer capacity
     */
    public int getMaxBufferCapacity() {
        return submissionPublisher.getMaxBufferCapacity();
    }

    public class SubscriberWrapper<T> implements Flow.Subscriber<T> {
        private Flow.Subscriber<? super T> subscriber;
        private Flow.Subscription subscription = null;

        public SubscriberWrapper(Flow.Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(final Flow.Subscription subscription) {
            this.subscription = subscription;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(final long n) {
                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                }
            });
        }

        @Override
        public void onNext(final T item) {
            subscriber.onNext(item);
        }

        @Override
        public void onError(final Throwable throwable) {
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            // Propagate only when cascadingClose is true
            if (cascadingClose) {
                subscriber.onComplete();
            }
        }

        public Flow.Subscriber<? super T> getWrappedSubscriber() {
            return subscriber;
        }

        /**
         * Get reference to subscriber's {@link Flow.Subscription}.
         *
         * @return subscriber's {@code subscription}
         */
        public Flow.Subscription getSubscription() {
            return this.subscription;
        }
    }

    public enum PublisherStrategy {
        /**
         * Blocking publisher strategy - tries to deliver to all subscribers regardless the cost.
         *
         * The thread is blocked uninterruptibly while resources for any subscriber are unavailable.
         * This strategy comes with a risk of thread exhaustion, that will lead to publisher being completely blocked by slow
         * or incorrectly implemented subscribers.
         */
        BLOCKING,

        /**
         * Best effort publisher strategy - tries to deliver to all subscribers if possible without blocking the processing.
         *
         * If the buffer is full, publisher invokes {@code onError()} and cancels subscription on a subscriber, that is not
         * capable of read the messages at a speed sufficient to unblock the processing.
         */
        BEST_EFFORT,
    }
}
