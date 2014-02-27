package com.radialpoint.word2vec.lucene;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class Word2VecFilterTest {

    @Before
    public void setUp() throws Exception {
        IndexFiles.main(new String[] { "-index", new File("./target/index").getAbsolutePath(), //
                "-docs", new File("src/test/resources/com/radialpoint/word2vec/lucene/quebec").getAbsolutePath() });
    }

    @After
    public void tearDown() throws Exception {
        File index = new File("./target/index");
        for (File f : index.listFiles())
            if (f.isFile())
                f.delete();
        index.delete();
    }

    @Test
    public void testIncrementToken() throws Exception {
        // no vector expansion, for documents
        SearchFiles.main(new String[] { "-index", //
                new File("./target/index").getAbsolutePath(), //
                "-query", "tourism" });

        assertEquals(4, SearchFiles.cachedHits.size());
        assertEquals("Rimouski.txt", SearchFiles.cachedHits.get(0).replaceAll("^.*/", ""));

        SearchFiles.main(new String[] {
                "-index",
                new File("./target/index").getAbsolutePath(), //
                "-vectors",
                new File("./src/test/resources/com/radialpoint/word2vec/lucene/quebec.vectors.ser").getAbsolutePath(),
                "-query", "tourism" });

        assertEquals(SearchFiles.cachedHits.size(), 9);
        assertEquals("Montreal.txt", SearchFiles.cachedHits.get(0).replaceAll("^.*/", ""));

    }
}
