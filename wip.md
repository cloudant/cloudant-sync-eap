---
layout: default
---

# Cloudant Sync - Early Access Program

Cloudant Sync is a CouchDB-replication-protocol-compatible datastore for
devices that don't want or need to run a full CouchDB instance. It's built
by [Cloudant](https://cloudant.com), building on the work of many others, and
is available under the [Apache 2.0 licence][ap2].

[ap2]: http://www.apache.org/licenses/LICENSE-2.0.txt

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
  Cloudant or CouchDB databasees.

We currently do not support, but plan to add support for:

* Managing conflicted documents.
* Documents with attachments.
* Indexing and query support. For now only retrieving documents by ID is
  supported.

## Support

Get in contact with us via [support@cloudant.com](mailto:support@cloudant.com)
if you have any problems. We'll try to update both the libraries and this page
with updates to solve common issues.

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

### Document CRUD APIs

Once you have a `Datastore` instance, you can use it to create, update and
delete documents.

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

A document is really a tree of the document and its history.

As documents can be edited in more than one place at once, replication can
cause documents to become conflicted. The document holds two or more _current
revisions_ as branches within the document's tree. Alternatively, when a
document holds more than one current revision, it's conflicted. An arbitrary
one of the current revisions is selected as the _winning revision_, and is
the one returned by calls to `Datastore#getDocument(...)`.

See more information on document trees in the [javadocs][jd] for `DBObjectTree`.

[jd]: docs/

In v1 of the EAP, searching for and resolving conflicts isn't supported, but
it'll be one of the first features we add.

## Finding data

At the moment, you can only retrieve a document using its ID. However, more
sophisticated indexing and querying is on the way.




