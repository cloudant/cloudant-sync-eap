## Exanoke app: Todo Sync

### Preparation

Gather the following information 1. your cloudant account username, 
2. database you want the app to sync with 3. api key and 4. api 
password, 

Find the file _res/values/strings.xml_, and fill the following default settings
    
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

Fill in your informaiton, and for example, this is what it should look like:

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

    gradle installDebug
