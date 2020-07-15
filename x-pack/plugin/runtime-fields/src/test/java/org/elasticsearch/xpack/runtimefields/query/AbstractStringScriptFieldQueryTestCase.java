/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.runtimefields.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.util.automaton.ByteRunAutomaton;
import org.elasticsearch.script.Script;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.EqualsHashCodeTestUtils;
import org.elasticsearch.xpack.runtimefields.StringScriptFieldScript;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public abstract class AbstractStringScriptFieldQueryTestCase<T extends AbstractStringScriptFieldQuery> extends ESTestCase {
    protected final StringScriptFieldScript.LeafFactory leafFactory = mock(StringScriptFieldScript.LeafFactory.class);

    protected abstract T createTestInstance();

    protected abstract T copy(T orig);

    protected abstract T mutate(T orig);

    protected final Script randomScript() {
        return new Script(randomAlphaOfLength(10));
    }

    public final void testEqualsAndHashCode() {
        EqualsHashCodeTestUtils.checkEqualsAndHashCode(createTestInstance(), this::copy, this::mutate);
    }

    public abstract void testMatches();

    public final void testToString() {
        T query = createTestInstance();
        assertThat(query.toString(), equalTo(query.fieldName() + ":" + query.toString(query.fieldName())));
    }

    protected abstract void assertToString(T query);

    public abstract void testVisit();

    /**
     * {@link Query#visit Visit} a query, collecting {@link ByteRunAutomaton automata},
     * failing if there are any terms or if there are more than one automaton.
     */
    protected final ByteRunAutomaton visitForSingleAutomata(T testQuery) {
        List<ByteRunAutomaton> automata = new ArrayList<>();
        testQuery.visit(new QueryVisitor() {
            @Override
            public void consumeTerms(Query query, Term... terms) {
                fail();
            }

            @Override
            public void consumeTermsMatching(Query query, String field, Supplier<ByteRunAutomaton> automaton) {
                assertThat(query, sameInstance(testQuery));
                assertThat(field, equalTo(testQuery.fieldName()));
                automata.add(automaton.get());
            }
        });
        assertThat(automata, hasSize(1));
        return automata.get(0);
    }
}