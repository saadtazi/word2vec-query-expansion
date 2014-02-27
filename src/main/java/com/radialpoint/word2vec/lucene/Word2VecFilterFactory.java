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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import com.radialpoint.word2vec.Vectors;
import com.radialpoint.word2vec.query_expansion.QueryExpander;
import com.radialpoint.word2vec.query_expansion.QueryExpander.TermSelection;

/**
 * Factory class for word2vec expansion TokenStream filters.
 */
final public class Word2VecFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

    private QueryExpander expander = null;
    private final String vectorsFile;
    private final int size;
    private final TermSelection termSelectionStrategy;
    private final boolean multiword;

    protected Word2VecFilterFactory(Map<String, String> args) {
        super(args);

        this.vectorsFile = require(args, "vectors");
        this.size = getInt(args, "size", 2);
        this.termSelectionStrategy = TermSelection.valueOf(get(args, "selection", "CUT_75_ABS"));
        this.multiword = getBoolean(args, "multiword", true);

        if (!args.isEmpty())
            throw new IllegalArgumentException("Unknown parameters: " + args);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        File vectorsFile = new File(this.vectorsFile);
        Vectors vectors = new Vectors(vectorsFile.exists() ? new FileInputStream(vectorsFile)
                : loader.openResource(this.vectorsFile));
        this.expander = new QueryExpander(vectors, true, termSelectionStrategy);
    }

    @SuppressWarnings("resource")
    @Override
    public TokenStream create(TokenStream input) {
        // if there are no vectors, just return the original token stream.
        return this.expander == null ? input : new Word2VecFilter(input, expander, size, multiword);
    }

}
