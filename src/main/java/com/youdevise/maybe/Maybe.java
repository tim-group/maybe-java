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


import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Collections;
import java.util.Iterator;

public abstract class Maybe<T> implements Iterable<T> {
    public abstract boolean isKnown();
    public abstract T otherwise(T defaultValue);
    public abstract T otherwiseThrow(RuntimeException exception);
    public abstract Maybe<T> otherwise(Maybe<T> maybeDefaultValue);
    public abstract <U> Maybe<U> transform(Function<? super T, ? extends U> mapping);
    public abstract <U> Maybe<U> bind(Function<? super T, Maybe<? extends U>> mapping);
    public abstract Maybe<Boolean> query(Predicate<? super T> mapping);
    public abstract Maybe<T> filter(Predicate<? super T> mapping);
    
    public final boolean isEmpty() {
        return !isKnown();
    }

    public static <T> Maybe<T> nothing() {
        return new AbsentValue<T>();
    }

    public static <T> Maybe<T> theAbsenceOfA(@SuppressWarnings("unused") Class<T> type) {
        return new AbsentValue<T>();
    }

    public static <T> Maybe<T> definitely(final T theValue) {
        return new DefiniteValue<T>(theValue);
    }

    public static <T> Maybe<T> maybe(final T theValue) {
        return (theValue == null) ? Maybe.<T>nothing() : definitely(theValue);
    }

    private static final class AbsentValue<T> extends Maybe<T> {
        @Override
        public boolean isKnown() {
            return false;
        }

        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public T otherwise(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T otherwiseThrow(RuntimeException exception) {
            throw exception;
        }

        @Override
        public Maybe<T> otherwise(Maybe<T> maybeDefaultValue) {
            return maybeDefaultValue;
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
        public T otherwise(T defaultValue) {
            return theValue;
        }

        @Override
        public T otherwiseThrow(RuntimeException exception) {
            return theValue;
        }

        @Override
        public Maybe<T> otherwise(Maybe<T> maybeDefaultValue) {
            return this;
        }

        @Override
        public <U> Maybe<U> transform(Function<? super T, ? extends U> mapping) {
            return definitely((U)mapping.apply(theValue));
        }

        @SuppressWarnings("unchecked")
        @Override
        public <U> Maybe<U> bind(Function<? super T, Maybe<? extends U>> mapping) {
            return (Maybe<U>)mapping.apply(theValue);
        }

        @Override
        public Maybe<Boolean> query(Predicate<? super T> mapping) {
            return definitely(mapping.apply(theValue));
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
}