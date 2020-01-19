# kotlin-search

This is a basic multi-language index implementation that can be embedded into applications. It is useful for
applications that require full-text search and natural language processing support without adding a full-featured
search engine like Elastic Search.

This library is built on top of Lucene and it does not implement any search or NLP algorithm. It wraps Lucene
to provide the following features:

* Multi-language search index
* Text normalization
* Multi-language stemming and tokenization
* Text classification

## Dependency

This is available on Maven Central Repository. It can be added using the following dependency:

```xml
<dependency>
    <groupId>be.rlab</groupId>
    <artifactId>kotlin-search</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Multi-language search index

The IndexManager is the component that provides the search index capability. It allows to index, retrieve
and search for documents. Documents are dictionaries (a set of keys/values) that are scoped to a namespace
and a language.

The IndexManager is a file-system based index. In order to support multiple languages, it creates an
index per-language. It means that documents in different languages will be physically stored in different
indexes. It allows very efficient operations for inserting and searching for documents since the index does
not need to perform a range query to retrieve all documents for a language. This model penalizes searching
for documents in different languages simultaneously.
