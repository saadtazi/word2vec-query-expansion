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

package com.radialpoint.word2vec.query_expansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.radialpoint.word2vec.Distance;
import com.radialpoint.word2vec.Distance.ScoredTerm;
import com.radialpoint.word2vec.OutOfVocabularyException;
import com.radialpoint.word2vec.Vectors;

/**
 * Expand a query using a given word2vec vectors.
 */
public class QueryExpander {

    /**
     * word2vec Vectors used for expansion.
     */
    private Vectors vectors;

    /**
     * Whether to consider the query terms independently or jointly
     */
    private boolean combinedVector;

    /**
     * Term selection strategy
     */
    private TermSelection termSelectionStrategy;

    public static enum TermSelection {

        /**
         * Return all terms
         */
        ALL {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return terms;
            }
        },

        /**
         * Return the top term
         */
        TOP {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return selectTopN(terms, 1);
            }

        },

        /**
         * Return the top 2 term
         */
        TOP2 {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return selectTopN(terms, 2);
            }
        },

        /**
         * Return the top 5 terms
         */
        TOP5 {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return selectTopN(terms, 5);
            }
        },

        /**
         * Cut off at 75% cosine (absolute)
         */
        CUT_75_ABS {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return selectWithThreshold(terms, 0.75f);
            }
        },

        /**
         * Cut off at 66% cosine (absolute)
         */
        CUT_66_ABS {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                return selectWithThreshold(terms, 0.66f);
            }
        },

        /**
         * Cut off at 90% cosine relative to top term
         */
        CUT_90_REL {
            protected List<ScoredTerm> select(List<ScoredTerm> terms) {
                if (terms.isEmpty())
                    return terms;
                float thr = terms.get(0).getScore() * 0.9f;
                return selectWithThreshold(terms, thr);
            }
        };

        protected List<ScoredTerm> selectTopN(List<ScoredTerm> terms, int n) {
            return terms.size() < n ? terms : terms.subList(0, n);
        }

        protected List<ScoredTerm> selectWithThreshold(List<ScoredTerm> terms, float thr) {
            int idx = 0;
            while (idx < terms.size() && terms.get(idx).getScore() > thr)
                idx++;
            return terms.subList(0, idx);
        }

        protected abstract List<ScoredTerm> select(List<ScoredTerm> terms);

    }

    public QueryExpander(Vectors vectors) {
        this(vectors, true, TermSelection.ALL);
    }

    public QueryExpander(Vectors vectors, boolean combinedVector) {
        this(vectors, combinedVector, TermSelection.ALL);
    }

    public QueryExpander(Vectors vectors, boolean combinedVector, TermSelection termSelectionStrategy) {
        this.vectors = vectors;
        this.combinedVector = combinedVector;
        this.termSelectionStrategy = termSelectionStrategy;
    }

    /**
     * Expand a query by combining all terms into a vector.
     * 
     * TODO: this method and Distance.measure should be refactored to receive the lists and arrays as parameters and
     * re-use them, for memory efficiency.
     */
    public List<Distance.ScoredTerm> expand(String[] terms) {

        // check for missing terms
        List<String> goodTerms = new ArrayList<String>();
        for (String term : terms)
            if (vectors.hasTerm(term))
                goodTerms.add(term);
        if (goodTerms.size() != terms.length)
            terms = goodTerms.toArray(new String[0]);

        // expanded terms according to GENCO or GENW
        List<Distance.ScoredTerm> expansion = null;
        try {
            expansion = Distance.measure(vectors, 50, terms);
        } catch (OutOfVocabularyException e) {
            // can't happen
            throw new IllegalStateException(e);
        }
        return this.termSelectionStrategy.select(expansion);
    }

    /**
     * Expand a query, either by combining all terms into a vector or by merging expansion lists for different vectors.
     */
    public List<Distance.ScoredTerm> expand(String query) {
        // calculate the list of terms
        String[] terms = query.split("\\s+");

        // check for missing terms
        List<String> goodTerms = new ArrayList<String>();
        for (String term : terms)
            if (term != null && vectors.hasTerm(term))
                goodTerms.add(term);
        if (goodTerms.size() != terms.length)
            terms = goodTerms.toArray(new String[0]);

        // expanded terms according to GENCO or GENW
        List<Distance.ScoredTerm> expansion = null;
        try {
            if (combinedVector)
                expansion = Distance.measure(vectors, 50, terms);
            else {
                expansion = new ArrayList<Distance.ScoredTerm>();
                for (String term : terms)
                    merge(expansion, Distance.measure(vectors, 50, new String[] { term }));
            }
        } catch (OutOfVocabularyException e) {
            // can't happen
            throw new IllegalStateException(e);
        }
        return this.termSelectionStrategy.select(expansion);
    }

    /**
     * Whether the term is known
     * 
     * @param term
     *            to check
     * @return true if this query expander knows about that term (i.e., it is in vocabulary).
     */
    public boolean isTermKnown(String term) {
        return this.vectors.hasTerm(term);
    }

    private void merge(List<ScoredTerm> expansion, List<ScoredTerm> extra) {
        Map<String, Integer> termPos = new HashMap<String, Integer>();
        for (int i = 0; i < expansion.size(); i++)
            termPos.put(expansion.get(i).getTerm(), i);
        for (Distance.ScoredTerm scoredTerm : extra) {
            Integer pos = termPos.get(scoredTerm.getTerm());
            if (pos == null)
                expansion.add(scoredTerm);
            else
                expansion.set(pos,
                        new ScoredTerm(scoredTerm.getTerm(), expansion.get(pos).getScore() + scoredTerm.getScore()));
        }
        Collections.sort(expansion, new Comparator<ScoredTerm>() {
            public int compare(ScoredTerm o1, ScoredTerm o2) {
                return new Float(o2.getScore()).compareTo(o1.getScore());
            }
        });

    }
}
