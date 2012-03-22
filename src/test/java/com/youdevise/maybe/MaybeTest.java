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

import java.util.List;
import java.util.Set;

import org.junit.Test;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.youdevise.maybe.Maybe.definitely;
import static com.youdevise.maybe.Maybe.nothing;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class MaybeTest {
    public static class Customer {
        private Maybe<String> emailAddress;

        public Customer(String emailAddress) {
            this.emailAddress = Maybe.definitely(emailAddress);
        }

        public Customer() {
            this.emailAddress = Maybe.nothing();
        }

        public Maybe<String> emailAddress() {
            return emailAddress;
        }
    }

    @Test(expected=IllegalArgumentException.class) public void
    cannotHaveADefiniteNothing() {
        Maybe.definitely(null);
    }
    
    @Test
    public void equalsOfKnownValues() throws Exception {
        assertThat(definitely(1), equalTo(definitely(1)));
        assertThat(definitely(1), not(equalTo(definitely(2))));
    }

    @Test
    public void unknownValuesAreEqual() throws Exception {
        assertThat(nothing(), equalTo(nothing()));

        Maybe<Object> u = nothing();
        assertThat(u, equalTo(u));
    }

    @Test
    public void anUnknownThingIsNeverEqualToAKnownThing() throws Exception {
        assertThat(Maybe.<Integer>nothing(), not(equalTo(definitely(1))));
        assertThat(Maybe.<String>nothing(), not(equalTo(definitely("rumsfeld"))));

        assertThat(definitely(1), not(equalTo(Maybe.<Integer>nothing())));
        assertThat(definitely("rumsfeld"), not(equalTo(Maybe.<String>nothing())));
    }

    @Test
    public void otherwiseADefaultValue() throws Exception {
        assertThat(noString().otherwise(""), equalTo(""));
        assertThat(definitely("foo").otherwise(""), equalTo("foo"));
    }

    @Test
    public void chainingOtherwise() throws Exception {
        assertThat(noString().otherwise(noString()).otherwise(""), equalTo(""));
        assertThat(noString().otherwise(definitely("X")).otherwise(""), equalTo("X"));
        assertThat(definitely("X").otherwise(definitely("Y")).otherwise(""), equalTo("X"));
    }

    @Test
    public void transforming() throws Exception {
        assertThat(new Customer("alice@example.com").emailAddress().transform(toUpperCase).otherwise("nobody@example.com"),
                equalTo("ALICE@EXAMPLE.COM"));
        assertThat(new Customer().emailAddress().transform(toUpperCase).otherwise("UNKNOWN"),
                equalTo("UNKNOWN"));
    }

    @Test
    public void querying() throws Exception {
        assertThat(definitely("example@example.com").query(isValidEmailAddress), equalTo(definitely(true)));
        assertThat(definitely("invalid-email-address").query(isValidEmailAddress), equalTo(definitely(false)));

        assertThat(Maybe.<String>nothing().query(isValidEmailAddress).isKnown(), equalTo(false));
    }

    @Test
    public void ifThen() throws Exception {
        Maybe<String> foo = definitely("foo");

        if (foo.isKnown()) for (String s : foo) {
            assertThat(s, equalTo("foo"));
        }
        else {
            fail("should not have been called");
        }
    }

    @Test
    public void ifElse() throws Exception {
        Maybe<String> foo = nothing();

        if (foo.isKnown()) for (@SuppressWarnings("unused") String s : foo) {
            fail("should not have been called");
        }
        else {
            // ok!
        }
    }

    @Test
    public void exampleCollectingValidEmailAddresses() {
        List<Customer> customers = newArrayList(
                new Customer(),
                new Customer("alice@example.com"),
                new Customer("bob@example.com"),
                new Customer(),
                new Customer("alice@example.com")
        );

        Set<String> emailAddresses = newHashSet(
                concat(transform(customers, toEmailAddress)));

        assertThat(emailAddresses, equalTo((Set<String>) newHashSet(
                "alice@example.com",
                "bob@example.com",
                "alice@example.com")));
    }

    @Test(expected=IllegalStateException.class)
    public void otherwiseThrow() {
        Maybe.nothing().otherwiseThrow(IllegalStateException.class);
    }

    @Test
    public void otherwiseThrowWithMessage() {
        try {
            Maybe.nothing().otherwiseThrow(IllegalStateException.class, "myMessage");
            fail("expected IllegalStateException");
        }
        catch (IllegalStateException e) {
            assertThat(e.getMessage(), equalTo("myMessage"));
        }
    }

    private static final Function<Customer, Maybe<String>> toEmailAddress = new Function<Customer, Maybe<String>>() {
        public Maybe<String> apply(Customer c) {
            return c.emailAddress();
        }
    };

    private static final Predicate<String> isValidEmailAddress = new Predicate<String>() {
        public boolean apply(String input) {
            return input.contains("@");
        }
    };

    private static final Function<String, String> toUpperCase = new Function<String, String>() {
        public String apply(String from) {
            return from.toUpperCase();
        }
    };

    private Maybe<String> noString() {
        return nothing();
    }
}