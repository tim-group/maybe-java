/*
 * Copyright 2010 Nat Pryce
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.youdevise.maybe;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public abstract class Maybe<T> implements Iterable<T> {
    public abstract boolean isKnown();
    public abstract T otherwise(Supplier<T> defaultValueSupplier);
    public abstract <E extends Throwable> T otherwiseThrow(Supplier<? extends E> exceptionSupplier) throws E;
    public abstract Maybe<T> otherwise(Maybe<? extends T> maybeDefaultValue);
    public abstract <U> Maybe<U> transform(Function<? super T, ? extends U> mapping);
    public abstract <U> Maybe<U> bind(Function<? super T, Maybe<? extends U>> mapping);
    public abstract Maybe<Boolean> query(Predicate<? super T> mapping);
    public abstract Maybe<T> filter(Predicate<? super T> mapping);

    public final boolean isEmpty() {
        return !isKnown();
    }

    public final T otherwise(T defaultValue) {
        return otherwise(Suppliers.ofInstance(defaultValue));
    }

    public final <E extends Throwable> T otherwiseThrow(final Class<E> exceptionClass) throws E {
        return otherwiseThrow(ExceptionSuppliers.of(exceptionClass));
    }

    public final <E extends Throwable> T otherwiseThrow(final Class<E> exceptionClass, final String message) throws E {
        return otherwiseThrow(ExceptionSuppliers.of(exceptionClass, message));
    }

    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> nothing() {
        return (Maybe<T>) AbsentValue.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> theAbsenceOfA(Class<T> type) {
        return (Maybe<T>) AbsentValue.INSTANCE;
    }

    public static <T> Maybe<T> definitely(final T theValue) {
        return new DefiniteValue<T>(theValue);
    }

    public static <T> Maybe<T> maybe(final T theValue) {
        return (theValue == null) ? Maybe.<T>nothing() : definitely(theValue);
    }

    public static <T> Predicate<Maybe<T>> known() {
        return new Predicate<Maybe<T>>() {
            public boolean apply(Maybe<T> input) {
                return input.isKnown();
            }
        };
    }
    
    public static <T> Predicate<Maybe<T>> knownToBeA(Class<T> type) {
        return known();
    }

    private static final class AbsentValue<T> extends Maybe<T> {
        private static final Maybe<?> INSTANCE = new AbsentValue<Object>();

        private AbsentValue() {
        }

        @Override
        public boolean isKnown() {
            return false;
        }

        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public T otherwise(Supplier<T> defaultValueSupplier) {
            return defaultValueSupplier.get();
        }

        @Override
        public <E extends Throwable> T otherwiseThrow(Supplier<? extends E> exceptionSupplier) throws E {
            throw exceptionSupplier.get();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Maybe<T> otherwise(Maybe<? extends T> maybeDefaultValue) {
            return (Maybe<T>) maybeDefaultValue;
        }

        @Override
        public <U> Maybe<U> transform(Function<? super T, ? extends U> mapping) {
            return nothing();
        }

        @Override
        public <U> Maybe<U> bind(Function<? super T, Maybe<? extends U>> mapping) {
            return nothing();
        }

        @Override
        public Maybe<Boolean> query(Predicate<? super T> mapping) {
            return nothing();
        }

        @Override
        public Maybe<T> filter(Predicate<? super T> mapping) {
            return this;
        }

        @Override
        public String toString() {
            return "unknown";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AbsentValue;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static final class DefiniteValue<T> extends Maybe<T> {
        private final T theValue;

        public DefiniteValue(T theValue) {
            if (null == theValue) {
                throw new IllegalArgumentException("Cannot have a definite value that is null");
            }
            this.theValue = theValue;
        }

        @Override
        public boolean isKnown() {
            return true;
        }

        public Iterator<T> iterator() {
            return Collections.singleton(theValue).iterator();
        }

        @Override
        public T otherwise(Supplier<T> defaultValueSupplier) {
            return theValue;
        }

        @Override
        public <E extends Throwable> T otherwiseThrow(Supplier<? extends E> exception) throws E {
            return theValue;
        }

        @Override
        public Maybe<T> otherwise(Maybe<? extends T> maybeDefaultValue) {
            return this;
        }

        @Override
        public <U> Maybe<U> transform(Function<? super T, ? extends U> mapping) {
            return maybe((U)mapping.apply(theValue));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Maybe<U> bind(Function<? super T, Maybe<? extends U>> mapping) {
            return (Maybe<U>)mapping.apply(theValue);
        }

        @Override
        public Maybe<Boolean> query(Predicate<? super T> mapping) {
            return maybe(mapping.apply(theValue));
        }

        @Override
        public Maybe<T> filter(Predicate<? super T> mapping) {
            return mapping.apply(theValue) ? this : Maybe.<T>nothing();
        }

        @Override
        public String toString() {
            return "definitely " + theValue.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            
            return theValue.equals(((DefiniteValue<?>)o).theValue);
        }

        @Override
        public int hashCode() {
            return theValue.hashCode();
        }
    }

    private static final class ExceptionSuppliers {

        private static <E extends Throwable> Supplier<E> of(final Class<E> exceptionClass) {
            return of(exceptionClass, null);
        }

        private static <E extends Throwable> Supplier<E> of(final Class<E> exceptionClass, final String message) {
            try {
                return (message == null) ? of(exceptionClass.getConstructor())
                                         : of(exceptionClass.getConstructor(String.class), message);
            } catch (SecurityException e) {
                throw new IllegalStateException("Unable to instantiate exception of class " + exceptionClass, e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unable to instantiate exception of class " + exceptionClass, e);
            }
        }

        private static <E extends Throwable> Supplier<E> of(final Constructor<E> constructor, final Object... args) {
            return new Supplier<E>() {
                public E get() {
                    try {
                        return constructor.newInstance(args);
                    } catch (InstantiationException e) {
                        throw new IllegalStateException("Unable to instantiate exception class ", e);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Unable to instantiate exception class ", e);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unable to instantiate exception class ", e);
                    } catch (SecurityException e) {
                        throw new IllegalStateException("Unable to instantiate exception class ", e);
                    } catch (InvocationTargetException e) {
                        throw new IllegalStateException("Unable to instantiate exception class ", e);
                    }
                }
            };
        }
    }
}