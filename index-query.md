## Index and Query

Cloudant Sync provides simple way to index and query your documents 
in datastore.

### Index

Index maps each document to a one of multiple index values. All the index 
values must be the same type. The index value type is specified when an 
index is created. 

The simplest index would be index document by one of its first level field. 
For example: 

```json
{
    "firstname": "John",
    "lastname": "Doe", 
    "age": "29"
}

```

we can create a index on field named "firstname", and then we can query 
to get the document whose has "firstname" field as "John".

```java
// IndexManager manages all the indexes for give Datastore
IndexManager indexManager = new IndexManager(datastore);
Index default = indexManager.ensureIndexed("default", "firstname")
```

#### Delete Index

```java
Index default = indexManager.deleteIndex("default")
```

#### Update Index

Let's say we want to change "default" index to use field "lastname". 
There is no direct way to update an existing Index. But you can
delete the old index, and create new one with the old name: 

```java
Index default = indexManager.deleteIndex("default");
Index defaultNew = indexManager.ensureIndexed("default", "lastname");
```

### Query

Index can be used to query documents. Here is an example:

```java
IndexManager indexManager = new IndexManager(datastore);
indexManager.updateAllIndexes();
Map query = new QueryBuilder().index("firstname").equalTo("John").build();
QueryResult result = indexManager.query(query);
```

Query is described using a map, where the map key is the name of the index to 
use: "firstname", and map value is the index value: "John".

Query can use more than one indexes. 

### Index Function

Index uses `IndexFunction` to mape document to index value. Here is the
`IndexFunction` used by the index we used in the example above. This 
function is provided out of box.

```java
public class FieldIndexFunction implements IndexFunction<Object> {

    private String fieldName;

    public FieldIndexFunction(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public List<Object> indexedValues(String indexName, Map map) {
        if (map.containsKey(this.fieldName)) {
            Object value = map.get(this.fieldName);
            return Arrays.asList(value);
        }
        return null;
    }
}
```

You can provide a custom `IndexFunction` when you create an index. Keep in 
mind that index definition is not persistent, you have to add the indexes 
every time the datastore is opened. But document index are persistent 
and built incrementally. 

### One more example

Here is another example, using `Index` to build collections, and 
this will be a pretty common use case for Index. 

Let's say you are building an app managing music, you will need a way to 
get music for each album, or for particular artist etc. Without index, 
you have to traverse all the songs to build these collections you self. 
But with index, it is simple query. 

For example, assume all the songs are in format like this:

```json
{
    "name": "Life in Technicolor",
    "album": "Viva la Vida",
    "artist": "coldplay"
    ....
}

{
    "name": "Viva la Vida",
    "album": "Viva la Vida",
    "artist": "coldplay"
    ....
}


{
    "name": "Square One",
    "album": "X&Y",
    "artist": "coldplay"
    ....
}

{
    "name": "What If",
    "album": "X&Y",
    "artist": "coldplay"
    ....
}

```

Index can be built for "album" and "artist", and you can easliy get all the 
songs in album "Viva la Vida" like this:

```java
IndexManager indexManager = new IndexManager(datastore);
Index albumIdx = indexManager.ensureIndexed("album", "album")
Index artistIdx = indexManager.ensureIndexed("artist", "artist")

Map query = new QueryBuilder()
    .index("artist").equalTo("coldplay")
    .index("album").equaltTo("Viva la Vida")
    .build();
QueryResult result = indexManager.query(query);

```
