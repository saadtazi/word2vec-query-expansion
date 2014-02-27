/*
 * Copyright 2014 Radialpoint SafeCare Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.radialpoint.word2vec.lucene;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import com.radialpoint.word2vec.Distance.ScoredTerm;
import com.radialpoint.word2vec.query_expansion.QueryExpander;

/**
 * This filter is not intended for indexing, just for query expansion. Trying to use this analyzer for indexing will
 * result in a very high indexing time and an unnecessarily large index.
 */

public class Word2VecFilter extends TokenFilter {

    public static final String TYPE_SYNONYM = "SYNONYM";

    private final QueryExpander expander;

    private final int size;

    private final boolean multiword;

    private final Set<String> output;

    private Iterator<String> outputIt;

    private final String[] terms;

    private final String[] termsTmp;

    /**
     * Expand the terms in a token stream using word2vec vectors.
     * 
     * @param input
     *            the original tokenstream
     * @param expander
     *            the query expander using word2vec vectors
     * @param size
     *            the number of terms to look-ahead to perform the word2vec queries
     * @param multiword
     *            whether to also combine the terms using underscores and to split returned terms on underscores.
     */
    public Word2VecFilter(TokenStream input, QueryExpander expander, int size, boolean multiword) {
        super(input);
        this.expander = expander;
        this.size = size;
        this.multiword = multiword;
        this.output = new HashSet<String>();
        this.outputIt = null;
        this.terms = new String[size];
        this.termsTmp = new String[size];
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

    @Override
    final public boolean incrementToken() throws java.io.IOException {
        if (outputIt == null || !outputIt.hasNext()) {
            if (!input.incrementToken())
                return false;

            State state = input.captureState();

            // if new word, advance up to size collecting the terms,
            boolean consumed = false;
            for (int i = 0; i < this.size; i++) {
                if (consumed)
                    this.terms[i] = null;
                else {
                    this.terms[i] = termAtt.toString();
                    if (!input.incrementToken())
                        consumed = true;
                }
            }

            // then expand and record the expansions
            this.output.clear();
            for (int i = 0; i < this.size; i++) {
                String term = this.terms[i];
                this.termsTmp[i] = expander.isTermKnown(term) ? term : null;
            }
            List<ScoredTerm> expansion = expander.expand(this.termsTmp);
            for (ScoredTerm scoredTerm : expansion) {
                String term = scoredTerm.getTerm();
                if (this.multiword && term.indexOf('_') >= 0) {
                    String[] parts = term.split("_");
                    for (String subTerm : parts)
                        this.output.add(subTerm);
                } else
                    this.output.add(term);
            }
            if (this.multiword) {
                // combine pairs of words

                int current = 0;
                while (current < this.size - 1) {
                    if (this.terms[current] != null && this.terms[current + 1] != null) {
                        String mwe = this.terms[current] + "_" + this.terms[current + 1];
                        if (expander.isTermKnown(mwe)) {
                            // expand
                            String currentTerm = this.termsTmp[current];
                            String currentTermNext = this.termsTmp[current + 1];
                            this.termsTmp[current + 1] = null;
                            this.termsTmp[current] = mwe;

                            expansion = expander.expand(this.termsTmp);
                            for (ScoredTerm scoredTerm : expansion) {
                                String term = scoredTerm.getTerm();
                                if (this.multiword && term.indexOf('_') >= 0) {
                                    String[] parts = term.split("_");
                                    for (String subTerm : parts)
                                        this.output.add(subTerm);
                                } else
                                    this.output.add(term);
                            }

                            this.termsTmp[current + 1] = currentTermNext;
                            this.termsTmp[current] = currentTerm;
                        }
                    }
                    current++;
                }
            }
            input.restoreState(state);
            this.outputIt = output.iterator();
            return true;
        } else {
            String next = outputIt.next();
            termAtt.copyBuffer(next.toCharArray(), 0, next.length());
            posIncrAtt.setPositionIncrement(0);
            typeAtt.setType(TYPE_SYNONYM);
            return true;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        this.output.clear();
        this.outputIt = null;
    }
}
