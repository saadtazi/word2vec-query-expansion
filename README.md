# word2vec query expansion component for Apache Lucene

This is a working Apache Lucene TokenFilter that uses a word2vec model for term expansion.

To build the scripts, do:

```
git submodule update --init
mvn package appassembler:assemble
```

The current code can read word2vec vectors in its C binary representation and will transform 
them to a Java format. It will scan each token in the TokenStream and expand it using word2vec 
vectors. More interestingly, it can also consider a window of terms (two or three terms), and 
add them together to find a vector related to the words in question. In a manner similar to 
the default [Apache Lucene](http://lucene.apache.org/core/)’s
[SynonymFilter](http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters#solr.SynonymFilterFactory),
the expanded terms appear as extra tokens appearing 
at the exact position as the original terms.

Word2VecFilter finds expansions by exhaustively searching for word2vec vectors closer 
(in an Euclidean distance sense) to a target vector. The target vector can be the vector 
associated with an original term or the additive combination of the vectors associated 
with several continuous query terms.

The current code base works directly with the Google News and Freebase vectors distributed 
by Google Inc.

An example corpus of 19 documents (the Wikipedia pages for all cities in Quebec with more
than 25,000 inhabitants) is included in 

```
src/test/resources/com/radialpoint/word2vec/lucene/quebec
```

and its associated vectors are included in the file 

```
src/test/resources/com/radialpoint/word2vec/lucene/quebec.vectors.ser
```  

The provided test case will index the 19 files and create an index under target, then 
use the provided vectors to expand a few queries. The performance of the system in this
example is very poor, the corpus has only 19 documents! But it is an end-to-end example
that can help you write your own.



## Limitations


The current code has only been tested under GNU/Linux amd64 platform. 
Due to the C nature of word2vec serialization, it might need adjustment
to run under other platforms. As the provided convert-vectors itself 
transforms the C serialization to a Java serialization, as a work-around
you might want to that migration in a 64-bit Intel architecture. Alternatively
you can adapt to your architecture

```
src/main/java/com/radialpoint/word2vec/ConvertVectors.java
```

and open a pull request with your changes :)


The terms used to create the word2vec vectors and the index should 
be kept in sync. This is currently left to the user, but it is possible
to extract the terms from a suitable index and feed them to word2vec
directly. Improving the current code in that direction is part of the
ROADMAP but patches are always welcomed.



## Building your own vectors


For convenience, this repository has word2vec as supplied by Google Inc. as a 
submodule. After checking out this repository, please do

```
git submodule update --init
```

to populate the folder ./main/src/c

Then the goal:

```
mvn exec:exec
```

should compile that code in `./main/src/c`.

To obtain vectors, use the word2vec command (see the word2vec documentation for details):

```
./main/src/c/word2vec -train ./test/resources/com/radialpoint/word2vec/lucene/word2vec.corpus -output target/quebec.vectors.bin -cbow 0 -size 200 -window 5 -negative 0 -hs 1 -sample 1e-3 -threads 12 -binary 1
```

where word2vec.corpus mimics the downcasing done by the Lucene analyzer, it was obtain
by running the following shell commands:

```
cat ./test/resources/com/radialpoint/word2vec/lucene/quebec/* | perl -pe 's/[^A-Za-z]/ /g' > ./test/resources/com/radialpoint/word2vec/lucene/word2vec.corpus
```

with the vectors in C binary serialization, use the provided transform-vectors tool:

```
sh ./runner/target/appassembler/bin/convert-vectors target/quebec.vectors.bin ./test/resources/com/radialpoint/word2vec/lucene/quebec.vectors.ser
```
