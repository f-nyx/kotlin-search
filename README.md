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
    <version>1.4.0</version>
</dependency>
```

This version supports Kotlin 1.7 and Lucene 9.

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
is the root entity that represents an entry in the index. Each document in the Lucene index is a plain list of
`key -> value` fields. Lucene supports multi-value fields, which means the same key might be stored multiple times with
different values in a document. In order to support both single-value and multi-value fields, we use a
`List<Any>` type to store the values in a [Field](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/model/Field.kt).

Documents have a 160-bits unique identifier composed by the following fields:

```
<32-bits hash of the Language><96-bits timestamp><32-bits unique id>
```

The language is included in the identifier in order to resolve which index should be queried to retrieve
a document. It includes a timestamp, which means that sorting documents by id will produce a collection sorted
by creation date. If you need to retrieve the document's language, you can use the
[be.rlab.search.Hashes.getLanguage(id)](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/Hashes.kt)
utility method to retrieve the language from the document's identifier.

The document `namespace` emulates _domain collections_. All queries will be scoped to a `namespace`, which means
that querying the index is analog to query a collection in a no-sql database.

### Documents schemas

Lucene fields have some attributes that are used in index-time to determine how the field is processed by the index.
The `stored` attribute tells Lucene to store the field value in the index. The `indexed` attribute indicates that a
field will be used for search, so it needs to be processed for that purpose. The `docValues` attribute marks a field
to be saved in a dedicated document-level space, which makes sorting and faceting much faster.

Each Lucene data type provides a default value for all these attributes (look at the data types section below). If you
want to change the behavior of a field, you should change it for each field and each document. In order to make it
easier, _kotlin-search_ introduces the concept of `document schemas`. A document schema allows to pre-define a set of
fields and its preferred attributes for a document. It provides data type validation out of the box, and it can be
added to the `IndexManager`.

The following example adds a document schema for the namespace `players` using the Functional DSL. For the Object
Mapper, the document schema is automatically created based on the annotations (look at the
[Indexing Documents](#indexing-documents) section below).

```kotlin
import be.rlab.search.query.*

val indexManager = IndexManager("/tmp/lucene-index").apply {
    addSchema("players") {
        string("id")
        text("firstName")
        text("lasName")
        int("age") {
            store()
            index()
            docValues()
        }
        float("score") {
            index()
            docValues()
        }
    }
}
```

### Data types

Lucene supports only a few native data types. The following table shows the default attributes for each data type.

| Field type | Stored | Indexed | Description
|------------|--------|---------|-------------
| string     |   yes  | no      | A String value stored exactly as it is provided.
| text       |   yes  | yes     | A String value that is tokenized and pre-processed by language analyzers.
| int        |   no   | no      | A multi-dimensional Int value for fast range filters.
| long       |   no   | no      | A multi-dimensional Long value for fast range filters.
| float      |   no   | no      | A multi-dimensional Float value for fast range filters.
| double     |   no   | no      | A multi-dimensional Double value for fast range filters.

In order to support additional Kotlin types, _kotlin-search_ provides a flexible `FieldTypeMapper` interface with a
default implementation for the native Lucene types. Custom implementations can be registered only through the
`IndexMapper`. So far the standard `IndexManager` does not support custom mappers, but we might consider adding
support if there are valid use cases.

Lucene does not support to store multi-dimensional fields, since they're packed as a
[BytesRef](https://lucene.apache.org/core/7_2_1/core/org/apache/lucene/util/BytesRef.html) value. This library
does not support _ByteRef_ field types yet.

The following table shows the default mapping from Kotlin to Lucene types.

| Lucene Type | Kotlin Type(s)       | nullable |
|-------------|----------------------|----------|
| string      | String, List<String> | yes      |
| text        | String, List<String> | yes      |
| int         | Int, List<Int>       | yes      |
| long        | Long, List<Long>     | yes      |
| float       | Float, List<Float>   | yes      |
| double      | Double, List<Double> | yes      |

You can take a look at the
[SimpleTypeMapper](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/mapper/SimpleTypeMapper.kt)
and [ListTypeMapper](https://github.com/f-nyx/kotlin-search/blob/master/src/main/kotlin/be/rlab/search/mapper/ListTypeMapper.kt)
components for further information.

### Indexing documents

_kotlin-search_ provides two strategies to index documents, a functional DSL and an object mapper. In order to keep
backward-compatibility with older versions, the functional DSL and object mapper strategies cannot be mixed. If
you indexed documents using the functional DSL, you cannot use the object mapper for searching.

There is a plan to support older indexes in the future, but it will require some extra configuration.

The following examples create a new document within the _players_ namespace in the _spanish_ index.

**Functional DSL**

```kotlin
import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager

val indexManager = IndexManager("/tmp/lucene-index")

indexManager.index("players", Language.SPANISH) {
    string("id", "player-id-1234")
    text("firstName", "Juan")
    text("lastName", "Pérez")
    int("age", 27) {
        store()
    }
    float("score", 10.0F)
}
```

**Object Mapper**

The object mapper strategy allows to use a data class to define a Lucene document structure. It uses a set of
`FieldTypeMapper`s to transform from Kotlin objects to Lucene documents and viceversa.

By default, fields are stored and indexed according to the Lucene default behavior for the data type, but you can
override this behavior setting the `store` and `index` attributes in the `@IndexField` annotation. If marked as not
stored, the field must be nullable.

You can override the default Lucene type using the `@IndexFieldType` annotation.

Note that the object mapper is strictly designed to map Lucene documents. You should not try to annotate your
domain entities since it probably won't work as expected. The Kotlin field types are restricted to the supported
Lucene field types, using other types will cause an error. If you want to map your custom types, you need to implement
a `FieldTypeMapper`.

```kotlin
import be.rlab.nlp.model.Language
import be.rlab.nlp.model.BoolValue
import be.rlab.search.*
import be.rlab.search.annotation.*

@IndexDocument(namespace = "players")
data class Player(
    @IndexField @IndexFieldType(FieldType.STRING) val id: String,
    @IndexField val firstName: String,
    @IndexField val lastName: String,
    @IndexField(index = BoolValue.YES) val age: Int,
    @IndexField(store = BoolValue.YES) val score: Float?
)

val indexManager = IndexManager("/tmp/lucene-index")
val mapper = IndexMapper(indexManager)

mapper.index(Player(
    firstName = "Juan",
    lastName = "Pérez",
    age = 27,
    score = 10.0F
), Language.SPANISH)
```

### Searching

_kotlin-search_ provides a functional DSL and an object mapper to build Lucene queries. All queries (look at the table
below) provide the following types of search:

* By a single field using the field name
* By a single field using a class annotated property
* By all fields in the document

To search by all fields in the document, all queries support a signature without the field name. You can
search by all fields or use the `by` modifier to restrict the search to a list of fields (look at the example below).

**Functional DSL**

In order to search by all fields using the functional DSL, you need to define a schema for the index namespace. The
schema must be defined only once. Defining the schema is optional, if you don't plan to search by multiple fields, you
can ignore the schema definition.

```kotlin
import be.rlab.search.query.*

val indexManager = IndexManager("/tmp/lucene-index").apply {
    addSchema("players") {
        string("id")
        text("firstName")
        text("lasName")
        int("age")
        float("score")
    }
}

indexManager.search("players", Language.SPANISH) {
    // Search in all fields defined in the schema.
    term("Juan") {
        // Optionally you can specify the list of fields, 
        // if not specified it will search in all fields.
        by("firstName")
    }
    range("age", 20, 30)
}
```

**Object Mapper**

```kotlin
import be.rlab.search.query.*

val indexManager = IndexManager("/tmp/lucene-index")
val mapper = IndexMapper(indexManager)

mapper.search<Player>(Language.SPANISH) {
    term(Player::firstName, "Juan")
    range(Player::age, 20, 30)
}
```

Both the functional DSL and the object mapper supports the following type of queries:

| Query type | Field types  |  Default boolean clause
|------------|--------------|-------------------------
|   term     | all          |  MUST
|   range    | numeric      |  SHOULD
|  wildcard  | string, text |  MUST
|   regex    | string, text |  MUST
|   fuzzy    | string, text |  MUST
|   phrase   | string, text |  MUST

If you need to build a custom query, the ```QueryBuilder``` providers the ```custom()``` method that receives
the current ```BooleanQuery``` in construction.

All queries have additional parameters that are initialized to the default Lucene values. If you need to
boost a query you can apply the boost as a modifier:

```kotlin
indexManager.search("players", Language.SPANISH) {
    term("firstName", "Juan") {
        boost(0.5F)
    }
}
```

Faceted search is not supported yet.

### Sorting

Sorting search results requires to mark fields as `DocValues` before indexing. `DocValues` is a fast document-level
index used for sorting and faceting. You can mark a field using both the Functional DSL and the Object Mapper.

The query builder provides a `sortBy()` clause that allows to specify a list of fields to sort results by. Note that
using the `sortBy()` clause requires a valid document schema. Sorting documents without an explicit document schema
is not supported, since the sorting criteria depends on the data type (look at the Document Schemas section above).

**Functional DSL**

```kotlin
import be.rlab.nlp.model.Language
import be.rlab.search.IndexManager

val indexManager = IndexManager("/tmp/lucene-index")

indexManager.index("players", Language.SPANISH) {
    string("id", "player-id-1234")
    text("firstName", "Juan")
    text("lastName", "Pérez")
    int("age", 27) {
        store()
    }
    float("score", 10.0F) {
        docValues()
    }
}

indexManager.search("players", Language.SPANISH) {
    range("age", 20, 30)
    sortBy("score")
}
```

**Object Mapper**

```kotlin
import be.rlab.nlp.model.Language
import be.rlab.nlp.model.BoolValue
import be.rlab.search.*
import be.rlab.search.annotation.*

@IndexDocument(namespace = "players")
data class Player(
    @IndexField @IndexFieldType(FieldType.STRING) val id: String,
    @IndexField val firstName: String,
    @IndexField val lastName: String,
    @IndexField(index = BoolValue.YES) val age: Int,
    @IndexField(store = BoolValue.YES, docValues = true) val score: Float?
)

val indexManager = IndexManager("/tmp/lucene-index")
val mapper = IndexMapper(indexManager)

mapper.index(Player(
    firstName = "Juan",
    lastName = "Pérez",
    age = 27,
    score = 10.0F
), Language.SPANISH)

mapper.search<Player>(Language.SPANISH) {
    range(Player::age, 20, 30)
    sortBy(Player::score)
}
```

### Query parsing

The ```QueryBuilder``` also supports parsing Lucene queries using the
[QueryParser](https://lucene.apache.org/core/8_0_0/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html)
syntax.

```kotlin
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
