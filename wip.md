---
---

# Cloudant Sync - Android Datastore

Cloudant Sync Datastore is an embedded document datastore for Android. It implements the CouchDB replication protocol for replicating local data to and from a remote CouchDB or Cloudant database. The datastore implements a native-code friendly API.

Documents within the datastore are heterogeneous JSON documents.

The API is quite different from CouchDB's; we retain the [MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control) data model but not the HTTP-centric API.

## Using in your project

Using the library in your project should be as simple as adding it as
a dependency via maven or gradle. We've tested using gradle at this
point.

### Gradle

Add the EAP maven repo and a compile time dependency:

```groovy
repositories {
    mavenLocal()
    maven { url "http://cloudant.github.io/cloudant-sync-eap/repository/" }
    mavenCentral()
}

dependencies {
    // Other dependencies
    compile group: 'com.cloudant', name: 'cloudant-sync-datastore-android', version:'0.1-SNAPSHOT'
}
```

### Maven

Note: syntax untested at time of writing.

It's a similar story in maven, add the repo and the dependency:

```xml
<project>
  ...

  <repositories>
    ...
    <repository>
      <id>cloudant-sync-eap</id>
      <name>Cloudant Sync EAP</name>
      <url>http://cloudant.github.io/cloudant-sync-eap/repository/</url>
    </repository>
  </repositories>

  <dependencies>
    ...
    <dependency>
      <groupId>com.cloudant</groupId>
      <artifactId>cloudant-sync-datastore-android</artifactId>
      <version>0.1-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>group-a</groupId>
      <artifactId>artifact-b</artifactId>
      <version>1.0</version>
      <type>bar</type>
      <scope>runtime</scope>
    </dependency>
  </dependencies>
</project>
```

## Developer Guide

### DatastoreManager and Datastore

The `DatastoreManager` object manages a directory where `Datastore` objects store their data. It's a factory object for named `Datastore`s.

Therefore, a developer starts by creating a `DatastoreManager` object bound to a directory:

```java
File path = new File("~/data/");
DatastoreManager manager = new DatastoreManager(path.getAbsolutePath());

Datastore ds = manager.openDatastore("my_datastore");
Datastore ds2 = manager.openDatastore("other_datastore");
```

On Android, it's very similar:

```java
// Create a DatastoreManager using application internal storage path
File path = getApplicationContext().getFilesDir();
DatastoreManager helper = new DatastoreManager(path.getAbsolutePath());
```

You should avoid manipulating the content of the directory managed by the `DatastoreManager` directly. The `DatastoreManager` for a given directory should be a singleton within your application too. Hopefully we'll fix this at a later stage.

### Document CRUD APIs

Developer manipulates document through `Datastore` interface to create, update and delete `DBObject` instances:

```java
Datastore ds = manager.openDatastore("my_datastore");

// Create a document
DBBody body = new BasicDBBody(jsonData);
BasicDBObject rev = ds.createDocument(body);

// Read a document
DBObject rev3 = ds.getDocument(rev.getId());

// Update a document
DBBody body2 = new BasicDBBody(jsonData2);
BasicDBObject rev2 = ds.updateDocument(rev.getId(), rev.getRevision(), body2);

// Delete a document
ds.deleteDocument(rev2.getId(), rev2.getRevision());
```

The `getAllDocuments()` method allows iterating through all documents in the database:

```java
// read all documents in one go
int pageSize = ds.getDocumentCount();
List<DBObject> docs = ds.getAllDocuments(0, pageSize, true);
```

### Replication

Replication is straight forward with Cloudant Sync. You can replicate from a local datastore to a remote database, from a remote database to a local datastore, or both ways to implement synchronisation.

First we create a simple listener that just sets a CountDownLatch when the replication finishes so we can wait for a replication to finish without needing to poll:

```java
/**
 * A {@code ReplicationListener} that sets a latch when it's told the
 * replication has finished.
 */
private class Listener implements ReplicationListener {

    private final CountDownLatch latch;
    public ErrorInfo error = null;

    Listener(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void complete(Replicator replicator) {
        latch.countDown();
    }

    @Override
    public void error(Replicator replicator, ErrorInfo error) {
        this.error = error;
        latch.countDown();
    }
}
```

Next we replicate a local datastore to a remote database:

```java
URI uri = "https://username.cloudant.com/my_database";
Datastore ds = manager.openDatastore("my_datastore");

// Create a replicator that replicates changes from the local
// datastore to the remote database.
Replicator replicator = ReplicatorFactory.oneway(ds, uri);

latch = new CountDownLatch(1);
Listener listener = new Listener(latch);
replicator.setListener(listener);
replicator.start();
latch.await();
if (replicator.getState() != Replicator.State.COMPLETE) {
    System.out.println("Error replicating TO remote");
    System.out.println(listener.error);
}
```

And getting data from a remote database to a local one:

```java
// Create a replicator that replicates changes from the remote
// database to the local datastore.
replicator = ReplicatorFactory.oneway(url, ds);

latch = new CountDownLatch(1);
Listener listener = new Listener(latch);
replicator.setListener(listener);
replicator.start();
latch.await();
if (replicator.getState() != Replicator.State.COMPLETE) {
    System.out.println("Error replicating FROM remote");
    System.out.println(listener.error);
}
```

## Finding data

At the moment, you can only retrieve a document using its ID. However, more sophisticated indexing and querying is on the way.




