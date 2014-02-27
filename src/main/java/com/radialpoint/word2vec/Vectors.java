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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class stores the mapping of String->array of float that constitutes each vector.
 * 
 * The class can serialize to/from a stream.
 * 
 * The ConvertVectors allows to transform the C binary vectors into instances of this class.
 */
public class Vectors {

    /**
     * The vectors themselves.
     */
    protected float[][] vectors;

    /**
     * The words associated with the vectors
     */
    protected String[] vocabVects;

    /**
     * Size of each vector
     */
    protected int size;

    /**
     * Inverse map, word-> index
     */
    protected Map<String, Integer> vocab;

    /**
     * Package-level constructor, used by the ConvertVectors program.
     * 
     * @param vectors
     *            , it cannot be empty
     * @param vocabVects
     *            , the length should match vectors
     */
    Vectors(float[][] vectors, String[] vocabVects) throws VectorsException {
        this.vectors = vectors;
        this.size = vectors[0].length;
        if (vectors.length != vocabVects.length)
            throw new VectorsException("Vectors and vocabulary size mismatch");
        this.vocabVects = vocabVects;
        this.vocab = new HashMap<String, Integer>();
        for (int i = 0; i < vocabVects.length; i++)
            vocab.put(vocabVects[i], i);
    }

    /**
     * Initialize a Vectors instance from an open input stream. This method closes the stream.
     * 
     * @param is
     *            the open stream
     * @throws IOException
     *             if there are problems reading from the stream
     */
    public Vectors(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);

        int words = dis.readInt();
        int size = dis.readInt();
        this.size = size;

        this.vectors = new float[words][];
        this.vocabVects = new String[words];

        for (int i = 0; i < words; i++) {
            this.vocabVects[i] = dis.readUTF();
            float[] vector = new float[size];
            for (int j = 0; j < size; j++)
                vector[j] = dis.readFloat();
            this.vectors[i] = vector;
        }
        this.vocab = new HashMap<String, Integer>();
        for (int i = 0; i < vocabVects.length; i++)
            vocab.put(vocabVects[i], i);
        dis.close();
    }

    /**
     * Writes this vector to an open output stream. This method closes the stream.
     * 
     * @param os
     *            the stream to write to
     * @throws IOException
     *             if there are problems writing to the stream
     */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(this.vectors.length);
        dos.writeInt(this.size);

        for (int i = 0; i < vectors.length; i++) {
            dos.writeUTF(this.vocabVects[i]);
            for (int j = 0; j < size; j++)
                dos.writeFloat(this.vectors[i][j]);
        }
        dos.close();
    }

    public float[][] getVectors() {
        return vectors;
    }

    public float[] getVector(int i) {
        return vectors[i];
    }

    public float[] getVector(String term) throws OutOfVocabularyException {
        Integer idx = vocab.get(term);
        if (idx == null)
            throw new OutOfVocabularyException("Unknown term '" + term + "'");
        return vectors[idx];
    }

    public int getIndex(String term) throws OutOfVocabularyException {
        Integer idx = vocab.get(term);
        if (idx == null)
            throw new OutOfVocabularyException("Unknown term '" + term + "'");
        return idx;
    }

    public Integer getIndexOrNull(String term) {
        return vocab.get(term);
    }

    public String getTerm(int index) {
        return vocabVects[index];
    }

    public Map<String, Integer> getVocabulary() {
        return vocab;
    }

    public boolean hasTerm(String term) {
        return vocab.containsKey(term);
    }

    public int vectorSize() {
        return size;
    }

    public int wordCount() {
        return vectors.length;
    }
}
