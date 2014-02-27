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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import com.radialpoint.word2vec.Distance;
import com.radialpoint.word2vec.OutOfVocabularyException;
import com.radialpoint.word2vec.Vectors;
import com.radialpoint.word2vec.VectorsException;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

/**
 * This small program takes a file with comma-separated queries and query IDs and produce the expanded query terms in separate files.
 * Useful for running experiments.
 */

public class ExpandQuery {

    @Argument(alias = "v", description = "File containing word2vec vectors in binary format", required = true)
    private static String vectorsFileName;

    @Argument(alias = "c", description = "Whether to combine all the query terms or expand them independently", required = false)
    private static Boolean combineTerms = false;

    @Argument(alias = "s", description = "Term selection strategy", required = false)
    private static String termSelectionString = "ALL";

    @Argument(alias = "i", description = "File containing the queries, one query per line", required = true)
    private static String inputFileName;

    @Argument(alias = "o", description = "Output folder", required = true)
    private static String outputFolderName;

    /**
     * @param args
     *            , three arguments, whether all the words are combined or separated
     * @throws IOException
     * @throws OutOfVocabularyException
     */
    public static void main(String[] args) throws VectorsException, IOException {
        // arguments
        try {
            Args.parse(ExpandQuery.class, args);
        } catch (IllegalArgumentException e) {
            Args.usage(ExpandQuery.class);
            System.exit(1);
        }

        QueryExpander queryExpander = new QueryExpander(new Vectors(new FileInputStream(new File(vectorsFileName))),
                combineTerms, QueryExpander.TermSelection.valueOf(termSelectionString));

        // read queries, one per line
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName), "UTF-8"));
        String line = br.readLine();
        line = br.readLine();
        while (line != null) {
            // qid and query
            String[] parts = line.split(",");
            String qid = parts[0];
            String query = parts[1];

            List<Distance.ScoredTerm> expansion = queryExpander.expand(query);

            PrintWriter pw = new PrintWriter(new FileWriter(new File(outputFolderName, qid + ".terms")));
            for (Distance.ScoredTerm scoredTerm : expansion)
                pw.println(scoredTerm.getTerm() + "\t" + scoredTerm.getScore());
            pw.close();

            line = br.readLine();
        }
        br.close();

    }
}
