---
layout: default
---

# Cloudant Sync - Early Access Program

[Cloudant Sync][eap] is an [Apache CouchDB&trade;][acdb]
replication-protocol-compatible datastore for
devices that don't want or need to run a full CouchDB instance. It's built
by [Cloudant](https://cloudant.com), building on the work of many others, and
is available under the [Apache 2.0 licence][ap2].

[ap2]: https://github.com/cloudant/cloudant-sync-eap/blob/master/LICENSE
[eap]: https://github.com/cloudant/cloudant-sync-eap
[acdb]: http://couchdb.apache.org/

## EAP - version 1.0

The first part of the EAP is a basic datastore and replication engine for
Android. The datastore implements a native-code friendly API.
This API is differs from CouchDB's: we retain the
[MVCC](http://en.wikipedia.org/wiki/Multiversion_concurrency_control)
data model but not the HTTP-centric API.

Documents within the datastore are heterogeneous JSON documents.

The library currently supports:

* Creating, updating and deleting documents (CRUD).
* Replication of data between a datastore managed by the library and remote
  Cloudant or CouchDB databases.

We currently do not support, but plan to add support for:

* Managing conflicted documents.
* Documents with attachments.
* Indexing and query support. For now only retrieving documents by ID is
  supported.

## Support

There is a [getting started][eap] guide. The javadocs for the Android library
are hosted [here][jd].

Get in contact with us via the [GitHub issue tracker][ghit] if you have any
problems and we'll try to get you up and running. We'll try to update both the
libraries and this page with updates as we tease out common problems and
questions.

[ghit]: https://github.com/cloudant/cloudant-sync-eap/issues

## Using in your project

Using the library in your project should be as simple as adding it as
a dependency via [maven][maven] or [gradle][gradle].

[maven]: http://maven.apache.org/
[gradle]: http://www.gradle.org/

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

_Note: syntax untested at time of writing._

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
  </dependencies>

</project>
```

## Developer Guide

Once you have the dependencies installed, the classes described below should
all be available to your project.

### Datastore and DatastoreManager objects

A `Datastore` object manages a set of JSON documents, keyed by ID.

A `DatastoreManager` object manages a directory where `Datastore` objects
store their data. It's a factory object for named `Datastore` instances. A
named datastore will persist its data between application runs. Names are
arbitrary strings, with the restriction that the name must match
`^[a-zA-Z]+[a-zA-Z0-9_]*`.

It's best to give a `DatastoreManager` a directory of its own, and to make the
manager a singleton within an application. The content of the directory is
simple folders and SQLite databases if you want to take a peek.

Therefore, start by creating a `DatastoreManager` to manage datastores for
a given directory:

```java
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.Datastore;

// Create a DatastoreManager using application internal storage path
File path = getApplicationContext().getDir("datastores");
DatastoreManager helper = new DatastoreManager(path.getAbsolutePath());
```

Once you've a manager set up, it's straightforward to create datastores:

```java
Datastore ds = manager.openDatastore("my_datastore");
Datastore ds2 = manager.openDatastore("other_datastore");
```

The `DatabaseManager` handles creating and initialising non-existent
datastores, so the object returned is ready for reading and writing.

To delete a datastore:

```java
manager.deleteDatastore("my_datastore");
```

It's important to note that this doesn't check there are any active
`Datastore` objects for this datastore. The behaviour of active `Datastore`
objects after their underlying files have been deleted is undefined.

### Document CRUD APIs

Once you have a `Datastore` instance, you can use it to create, update and
delete documents.

```java
Datastore ds = manager.openDatastore("my_datastore");

// Create a document
DBBody body = new BasicDBBody(jsonData);
DBObject revision = ds.createDocument(body);

// Read a document
DBObject aRevision = ds.getDocument(revision.getId());

// Update a document
DBBody updatedBody = new BasicDBBody(moreJsonData);
updatedRevision = ds.updateDocument(
    revision.getId(),
    revision.getRevision(),
    updatedBody
);

// Delete a document
ds.deleteDocument(
    updatedRevision.getId(),
    updatedRevision.getRevision()
);
```

The `getAllDocuments()` method allows iterating through all documents in the
database:

```java
// read all documents in one go
int pageSize = ds.getDocumentCount();
List<DBObject> docs = ds.getAllDocuments(0, pageSize, true);
```

### Replication

Replication is straightforward. You can replicate from a
local datastore to a remote database, from a remote database to a local
datastore, or both ways to implement synchronisation.

First we create a simple listener that just sets a CountDownLatch when the
replication finishes so we can wait for a replication to finish without
needing to poll:

```java
import com.cloudant.sync.replication.ReplicationListener;

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
import com.cloudant.sync.replication.ReplicationFactory;
import com.cloudant.sync.replication.Replicator;

// username/password can be Cloudant API keys
URI uri = new URI("https://username:password@username.cloudant.com/my_database");
Datastore ds = manager.openDatastore("my_datastore");

// Create a replicator that replicates changes from the local
// datastore to the remote database.
Replicator replicator = ReplicatorFactory.oneway(ds, uri);

// Use a CountDownLatch to provide a lightweight way to wait for completion
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
// username/password can be Cloudant API keys
URI uri = new URI("https://username:password@username.cloudant.com/my_database");
Datastore ds = manager.openDatastore("my_datastore");

// Create a replictor that replicates changes from the remote
// database to the local datastore.
replicator = ReplicatorFactory.oneway(uri, ds);

// Use a CountDownLatch to provide a lightweight way to wait for completion
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

And running a full sync, that is, two one way replicaitons:

```java
// username/password can be Cloudant API keys
URI uri = new URI("https://username:password@username.cloudant.com/my_database");
Datastore ds = manager.openDatastore("my_datastore");

replicator_pull = ReplicatorFactory.oneway(uri, ds);
replicator_push = ReplicatorFactory.oneway(ds, uri);

// Use a latch starting at 2 as we're waiting for two replications to finish
latch = new CountDownLatch(2);
Listener listener = new Listener(latch);

// Set the listener and start for both pull and push replications
replicator_pull.setListener(listener);
replicator_pull.start();
replicator_push.setListener(listener);
replicator_push.start();

// Wait for both replications to complete, decreasing the latch via listeners
latch.await();

// Unfortunately in this implementation we'll only record the last error
// the listener saw
if (replicator_pull.getState() != Replicator.State.COMPLETE) {
    System.out.println("Error replicating FROM remote");
    System.out.println(listener.error);
}
if (replicator_push.getState() != Replicator.State.COMPLETE) {
    System.out.println("Error replicating FROM remote");
    System.out.println(listener.error);
}
```

## Conflicts

A document is really a tree of the document and its history. This is neat
because it allows us to store multiple versions of a document. In the main,
there's a single, linear tree -- just a single branch -- running from the
creation of the document to the current revision. It's possible, however,
to create further branches in the tree.

When a document has been replicated to more than one place, it's possible to
edit it concurrently in two places. When the datastores storing the document
then replicate with each other again, they each add their changes to the
document's tree. This causes an extra branch to be added to the tree for
each concurrent set of changes. When this happens, the document is said to be
_conflicted_. This creates multiple current revisions of the document, one for
each of the concurrent changes.

To make things easier, calling `Datastore#getDocument(...)` returns one of
the leaf nodes of the branches of the conflicted document. It selects the
node to return in an arbitrary but deterministic way, which means that all
replicas of the database will return the same revision for the document. The
other copies of the document are still there, however, waiting to be merged.

See more information on document trees in the [javadocs][jd] for `DBObjectTree`.

[jd]: docs/

In v1 of the EAP, searching for and resolving conflicts isn't supported, but
it'll be one of the first features we add.

## Finding data

At the moment, you can only retrieve a document using its ID. However, more
sophisticated indexing and querying is on the way.




