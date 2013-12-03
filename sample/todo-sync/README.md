## Example app: Todo Sync

### Preparation

Gather the following information:

1. Your cloudant account username, 
2. Database name the app to sync with
3. API key 
4. API password, 

The database must exist in your cloudant account, otherwise the app will
report error when it syncs. 

Find the file _res/values/settings.xml_, and fill in the following default 
settings using the information above.
    
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    ....
    <string name="default_user"></string>
    <string name="default_dbname"></string>
    <string name="default_api_key"></string>
    <string name="default_api_password"></string>
    ....
</resources>
```

For example, this is file might look like afterwards:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    ....
    <string name="default_user">dongshengcn</string>
    <string name="default_dbname">example_app_todo</string>
    <string name="default_api_key">dongshengcn</string>
    <string name="default_api_password">secretpassword</string>
    ....
</resources>
```

### Build

Connect your android device, assume you have gradle installed, and 
you can simple run this

    gradle installDebug

The example app will be built and installed on your device. 
