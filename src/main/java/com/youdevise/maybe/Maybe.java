package com.youdevise.maybe;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Collections;
import java.util.Iterator;

public abstract class Maybe<T> implements Iterable<T> {
    public abstract boolean isKnown();
    public abstract T otherwise(T defaultValue);
    public abstract Maybe<T> otherwise(Maybe<T> maybeDefaultValue);
    public abstract <U> Maybe<U> to(Function<? super T, ? extends U> mapping);
    public abstract Maybe<Boolean> query(Predicate<? super T> mapping);
    
    public static <T> Maybe<T> unknown() {
        return new UnknownValue<T>();
    }
    
    public static <T> Maybe<T> unknown(Class<T> type) {
        return new UnknownValue<T>();
    }
    
    public static <T> Maybe<T> definitely(final T theValue) {
        return new DefiniteValue<T>(theValue);
    }
    
    public static <T> Maybe<T> possibly(final T theValue) {
        return (theValue == null) ? Maybe.<T>unknown() : definitely(theValue);
    }
    
    private static final class UnknownValue<T> extends Maybe<T> {
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
        public Maybe<T> otherwise(Maybe<T> maybeDefaultValue) {
            return maybeDefaultValue;
        }

        @Override
        public <U> Maybe<U> to(Function<? super T, ? extends U> mapping) {
            return unknown();
        }

        @Override
        public Maybe<Boolean> query(Predicate<? super T> mapping) {
            return unknown();
        }

        @Override
        public String toString() {
            return "unknown";
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static final class DefiniteValue<T> extends Maybe<T> {
        private final T theValue;

        public DefiniteValue(T theValue) {
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
        public Maybe<T> otherwise(Maybe<T> maybeDefaultValue) {
            return this;
        }

        @Override
        public <U> Maybe<U> to(Function<? super T, ? extends U> mapping) {
            return definitely((U)mapping.apply(theValue));
        }

        @Override
        public Maybe<Boolean> query(Predicate<? super T> mapping) {
            return definitely(mapping.apply(theValue));
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