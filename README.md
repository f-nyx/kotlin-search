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
* Sentiment analysis

## Dependency

This is available on Maven Central Repository. It can be added using the following dependency:

```xml
<dependency>
    <groupId>be.rlab</groupId>
    <artifactId>kotlin-search</artifactId>
    <version>1.1.0</version>
</dependency>
```

This version supports Kotlin 1.6 and Lucene 9.

The only two hard dependencies are SLF4j and commons-codec. We will not add more dependencies unless it's strictly
necessary to avoid classpath errors.

## Multi-language search index

The IndexManager is the component that provides access to the search index. It allows to index, retrieve
and search for documents. Documents are dictionaries (a set of keys/values) that are scoped to a namespace.

The IndexManager is a file-system based index. In order to support multiple languages, it creates an
index per-language. It means that documents in different languages will be physically stored in different
indexes. It allows very efficient operations for inserting and searching for documents since the index does
not need to perform a range query to retrieve all documents for a language. This model penalizes searching
for documents in different languages simultaneously.

Before continue reading the following sections, we strongly recommend reading
[Search and Scoring](https://lucene.apache.org/core/7_1_0/core/org/apache/lucene/search/package-summary.html#package.description)
and [Classic Scoring Formula](https://lucene.apache.org/core/7_1_0/core/org/apache/lucene/search/similarities/TFIDFSimilarity.html)
in Lucene documentation. This library uses the default scoring algorithm to match documents.

### Documents

The [Document](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/model/Document.kt)
is the root entity that represents an entry in the index.

Documents have a 160-bits unique identifier composed by the following fields:

```
<32-bits hash of the Language><96-bits timestamp><32-bits unique id>
```

The language is included in the identifier in order to resolve which index should be queried to retrieve
a document. It includes a timestamp, which means that sorting documents by id will produce a collection sorted
by creation date. If you need to retrieve the document's language, you can use the
[be.rlab.search.Hashes.getLanguage(id)](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/Hashes.kt)
utility method to retrieve the language from the document's identifier.

The document namespace emulates _domain collections_. All queries will be scoped to a namespace, which means
that querying the index is analog to query a collection in a no-sql database.

### Indexing documents

_kotlin-search_ provides a simple DSL to index documents. Let's start with the following example:

```kotlin
import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager

val indexManager = IndexManager("/tmp/lucene-index")

indexManager.index("players", Language.SPANISH) {
    text("firstName", "Juan")
    text("lastName", "Pérez")
    int("age", 27) {
        store()
    }
    float("score", 10.0F)
}
```

It creates a new document within the _players_ namespace in the _spanish_ index. Lucene stores and indexes
fields depending on the field type. The following table describes the supported field types and how fields
are indexed and stored by default.

| Field type | Stored | Indexed | Description
|------------|--------|---------|-------------
| string     |   yes  |   yes   | A String value stored exactly as it is provided.
| text       |   yes  |   yes   | A String value that is tokenized and pre-processed by language analyzers.
| int        |   no   |   no    | A multi-dimensional Int value for fast range filters.
| long       |   no   |   no    | A multi-dimensional Long value for fast range filters.
| float      |   no   |   no    | A multi-dimensional Float value for fast range filters.
| double     |   no   |   no    | A multi-dimensional Double value for fast range filters.

By default, numeric fields are not stored. In order to force the storing or indexing of a field you can apply
the ```store()``` and ```index()``` modifiers as shown above. If a field is not stored, it will not be available
in the _Document_ when it is retrieved from the index.

Lucene does not support to store multi-dimensional fields, since they're packed as a
[BytesRef](https://lucene.apache.org/core/7_2_1/core/org/apache/lucene/util/BytesRef.html) value. This library
does not support _ByteRef_ field types.

Column-wise fields for sorting/faceting are not supported yet.

### Searching

_kotlin-search_ provides a DSL to build Lucene queries.

```
val indexManager = IndexManager("/tmp/lucene-index")

indexManager.search("players", Language.SPANISH) {
    term("firstName", "Juan")
    range("age", 20, 30)
}
```

The DSL supports the following type of queries:

| Query type |   Field types    |  Default boolean clause
|------------|------------------|-------------------------
|   term     |    all           |  MUST
|   range    |    numeric       |  SHOULD
|  wildcard  |   string, text   |  MUST
|   regex    |   string, text   |  MUST
|   fuzzy    |   string, text   |  MUST
|   phrase   |   string, text   |  MUST

If you need to build a custom query, the ```QueryBuilder``` providers the ```custom()``` method that receives
the current ```BooleanQuery``` in construction.

All queries have additional parameters that are initialized to the default Lucene values. If you need to
boost a query you can apply the boost as a modifier:

```
indexManager.search("players", Language.SPANISH) {
    term("firstName", "Juan") {
        boost(0.5F)
    }
}
```

Faceted search is not supported yet.

### Query parsing

The ```QueryBuilder``` also supports parsing Lucene queries using the
[QueryParser](https://lucene.apache.org/core/8_0_0/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html)
syntax.

```
val indexManager = IndexManager("/tmp/lucene-index")

indexManager.search("players", Language.SPANISH) {
    parse("firstName", "age:[22 TO 35] AND Juan")
}
```
The first parameter of the ```parse()``` method is the default field if no field is specified in the query. For
instance, in the previous query, it will search for all persons with first name ```Juan``` with ages between
22 and 35 years. For full syntax documentation take a look at the [Lucene documentation](https://lucene.apache.org/core/8_0_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description).

### Pagination

```indexManager.search``` search for documents in the index. By default it limits results up to
```IndexManager.DEFAULT_LIMIT``` documents. The operation returns a
[SearchResult](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/model/SearchResult.kt)
that contains the documents in the first page and a cursor to the next page in the recordset. You must call
```indexManager.search``` providing the cursor to search for the next page. This pagination strategy is useful when
you have to defer the search in order to continue later.

If you don't need a deferred pagination, you can use ```indexManager.find``` to get the full list of results as
a Sequence. It will query the index as many times as required until the recordset has no more documents.
