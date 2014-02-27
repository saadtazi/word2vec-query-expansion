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
package com.radialpoint.word2vec;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Distance {

    public static class ScoredTerm {
        private String term;
        private float score;

        public ScoredTerm(String term, float score) {
            super();
            this.term = term;
            this.score = score;
        }

        public String getTerm() {
            return term;
        }

        public float getScore() {
            return score;
        }

    }

    public static List<ScoredTerm> measure(Vectors vectors, int wordsToReturn, String[] tokens)
            throws OutOfVocabularyException {
        double distance, length;
        float[] bestDistance = new float[wordsToReturn];
        String[] bestWords = new String[wordsToReturn];
        int d;
        int size = vectors.vectorSize();
        float[] vec = new float[size];
        float[][]allVec = vectors.getVectors();

        Set<Integer> wordIdx = new TreeSet<Integer>();

        int tokenCount = tokens.length;
        boolean outOfDict = false;
        String outOfDictWord = null;
        Arrays.fill(vec, 0.0f);
        wordIdx.clear();
        for (int i = 0; i < tokenCount; i++) {
            Integer idx = vectors.getIndexOrNull(tokens[i]);
            if (idx == null) {
                outOfDictWord = tokens[i];
                outOfDict = true;
                break;
            }
            wordIdx.add(idx);
            float[] vect1 = allVec[idx];
            for (int j = 0; j < size; j++)
                vec[j] += vect1[j];
        }
        if (outOfDict)
            throw new OutOfVocabularyException(outOfDictWord);

        length = 0;
        for (int i = 0; i < size; i++)
            length += vec[i] * vec[i];
        length = (float) Math.sqrt(length);
        for (int i = 0; i < size; i++)
            vec[i] /= length;

        for (int i = 0; i < wordsToReturn; i++) {
            bestDistance[i] = Float.MIN_VALUE;
            bestWords[i] = "";
        }

        for (int c = 0; c < vectors.wordCount(); c++) {
            if (wordIdx.contains(c))
                continue;
            distance = 0;
            for (int i = 0; i < size; i++)
                distance += vec[i] * allVec[c][i];
            for (int i = 0; i < wordsToReturn; i++) {
                if (distance > bestDistance[i]) {
                    for (d = wordsToReturn - 1; d > i; d--) {
                        bestDistance[d] = bestDistance[d - 1];
                        bestWords[d] = bestWords[d - 1];
                    }
                    bestDistance[i] = (float) distance;
                    bestWords[i] = vectors.getTerm(c);
                    break;
                }
            }
        }
        List<ScoredTerm> result = new ArrayList<ScoredTerm>(wordsToReturn);
        for (int i = 0; i < wordsToReturn; i++)
            result.add(new ScoredTerm(bestWords[i], bestDistance[i]));
        return result;
    }

}
