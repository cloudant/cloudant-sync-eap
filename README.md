# Cloudant Sync EAP

Early Access Program repository for Cloudant Sync. 

For details on how to get started with the library, see
http://cloudant.github.io/cloudant-sync-eap.

There is a [sample project](sample/todo-sync/README.md).

## Contributors

The Cloudant Sync Datastore Android library embeds Apache 2.0 licensed
code. As the library is currently provided in binary form, in addition
to crediting the valuable contributions of others, we've reproduced the
headers for the affected files.

The MVCC layer (the `com.cloudant.sync.datastore` package) holds 
code adapted from [TouchDB][touchdb].

```java
/**
 * Original iOS version by  Jens Alfke, ported to Android by Marty Schoch
 * Copyright (c) 2012 Couchbase, Inc. All rights reserved.
 *
 * Modifications for this distribution by Cloudant, Inc., Copyright (c) 2013 Cloudant, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
```

Classes inside the `com.cloudant.android` package are originally from the
Android project, with minor modifications to allow them to work outside the
Android environment for use during development and desktop-testing. Their
header is as follows:

```java
/**
 * Modifications allow class to work outside Android framework by
 * Cloudant, Inc., Copyright (C) 2013 Cloudant, Inc.
 *
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

[touchdb]: https://github.com/couchbaselabs/TouchDB-Android
