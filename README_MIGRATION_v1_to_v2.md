<p align="center">
<a href="http://www.customerly.io">
<img src="https://customerly.github.io/logo.svg">
</a>
</p>


  [![Android API14+](https://img.shields.io/badge/Android-API_14+-green.svg)]()
  [![Java 6+](https://img.shields.io/badge/Java-6+-red.svg)]()
  [![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-red.svg)]()

<p align="center">
  <img src="https://cdn.worldvectorlogo.com/logos/kotlin-1.svg" width=25 alt="Icon"/> <b>Kotlin oriented</b> (Java fully supported)
</p>

## Migration doc from v1.x.x to v2.0.0

### Customerly.configure (immutated)

`Java v1.x.x`
```java
Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID");
//or
Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID", Color.RED);
```
`Kotlin v2.0.0`
```kotlin
Customerly.configure(application = this, customerlyAppId = "YOUR_CUSTOMERLY_APP_ID")
//or
Customerly.configure(application = this, customerlyAppId = "YOUR_CUSTOMERLY_APP_ID", widgetColorInt = Color.RED)
```
`Java v2.0.0`
```java
Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID");
//or
Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID", Color.RED);
```

### Customerly.get().setVerboseLogging (just remove .get())
`Java v1.x.x`
```java
Customerly.get().setVerboseLogging(BuildConfig.DEBUG);
```
`Kotlin v2.0.0`
```kotlin
Customerly.setVerboseLogging(enabled = BuildConfig.DEBUG)
```
`Java v2.0.0`
```java
Customerly.setVerboseLogging(BuildConfig.DEBUG);
```

### Customerly.get().openSupport (just remove .get())
`Java v1.x.x`
```java
Customerly.get().openSupport(Activity.this);
```
`Kotlin v2.0.0`
```kotlin
Customerly.openSupport(activity = this@Activity)
```
`Java v2.0.0`
```java
Customerly.openSupport(Activity.this);
```

### Customerly.get().registerUser (see below)
`Java v1.x.x`
```java
//Complete use case:
Customerly.get()
    .registerUser("axlrose@example.com")
    .user_id("12345")
    .name("Gianni")
    .attributes(attributesMap)
    .company(companyMap)
    .successCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task completes successfully
        }
    })
    .failureCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task fails
        }
    })
    .start();

//Just by email:
Customerly.get()
    .registerUser("axlrose@example.com")
    .start();

//Just by email and name:
Customerly.get()
    .registerUser("axlrose@example.com")
    .name("Gianni")
    .start();
```
`Kotlin v2.0.0`
```kotlin
//Complete use case:
Customerly.registerUser(
    email = "axlrose@example.com",
    userId = "12345",
    name = "Gianni",
    attributes = attributesMap,
    company = companyMap,
    success = { /*Called if the task completes successfully */ },
    failure = { /*Called if the task fails */ })

//Just by email:
Customerly.registerUser(email = "axlrose@example.com")

//Just by email and name:
Customerly.registerUser(email = "axlrose@example.com", name = "Gianni")
```
`Java v2.0.0`
```java
//Complete use case:
Customerly.registerUser(
    "axlrose@example.com",
    "12345",
    "Gianni",
    attributesMap,
    companyMap,
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task completes successfully
            return null;
        }
    },
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task fails
            return null;
        }
    });

//Just by email:
Customerly.registerUser("axlrose@example.com");

//Just by email and name:
Customerly.registerUser("axlrose@example.com", null, "Gianni");
```

### Customerly.get().logoutUser (just remove .get())
`Java v1.x.x`
```java
Customerly.get().logoutUser();
```
`Kotlin v2.0.0`
```kotlin
Customerly.logoutUser()
```
`Java v2.0.0`
```java
Customerly.logoutUser();
```

### Building attributes map
`Java v1.x.x`
```java
HashMap<String,Object> attributesMap = new HashMap<>();
attributesMap.put("prova",true);
attributesMap.put("country", "IT");
```
`Kotlin v2.0.0`
```kotlin
val attributesMap = HashMap<String,Any>()
attributesMap.put("prova",true)
attributesMap.put("country", "IT")
//or
val attributesMap = Customerly.attributeJson("prova" to true, "country" to "IT")
```
`Java v2.0.0`
```java
HashMap<String,Object> attributesMap = HashMap<>()
attributesMap.put("prova",true);
attributesMap.put("country", "IT");
```

### Customerly.get().setAttributes
`Java v1.x.x`
```java
Customerly.get()
    .setAttributes(attributesMap)
    .successCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task completes successfully
        }
    })
    .failureCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task fails
        }
    })
    .start();
```
`Kotlin v2.0.0`
```kotlin
Customerly.setAttributes(
    attributes = attributesMap,
    success = { /* Called if the task completes successfully */ },
    failure = { /* Called if the task fails */ })
//or
Customerly.setAttributes(
    "prova" to true,
    "country" to "IT",
    success = { /* Called if the task completes successfully */ },
    failure = { /* Called if the task fails */ })
```
`Java v2.0.0`
```java
Customerly.setAttributes(
    attributesMap,
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task completes successfully
            return null;
        }
    },
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task fails
            return null;
        }
    });
```

### Building company map
`Java v1.x.x`
```java
HashMap<String, Object> companyMap = new HashMap<String, Object>();
companyMap.put("company_id", "123abc");
companyMap.put("name", "Customerly");
companyMap.put("foundation year", 2017);
//or
HashMap<String, Object> companyMap = new Customerly.CompanyBuilder("123abc", "Customerly").put("foundation year", 2017).build();
```
`Kotlin v2.0.0`
```kotlin
val companyMap = HashMap<String,Any>()
companyMap.put("company_id", "123abc")
companyMap.put("name", "Customerly")
companyMap.put("foundation year", 2017)
//or
val attributesMap = Customerly.companyJson("foundation year" to 2017, companyId = "123abc", companyName = "Customerly")
```
`Java v2.0.0`
```java
HashMap<String,Object> attributesMap = HashMap<>();
companyMap.put("company_id", "123abc");
companyMap.put("name", "Customerly");
companyMap.put("foundation year", 2017);
//or (stil existing but deprecated)
HashMap<String, Object> companyMap = new Customerly.CompanyBuilder("123abc", "Customerly").put("foundation year", 2017).build();
```

### Customerly.get().setCompany
`Java v1.x.x`
```java
Customerly.get()
    .setCompany(attributesMap)
    .successCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task completes successfully
        }
    })
    .failureCallback(new Customerly.Callback() {
        @Override
        public void callback() {
            //Called if the task fails
        }
    })
    .start();
```
`Kotlin v2.0.0`
```kotlin
Customerly.setCompany(
    company = companyMap,
    success = { /* Called if the task completes successfully */ },
    failure = { /* Called if the task fails */ })
//or
Customerly.setCompany(
    "foundation year" to 2017,
    companyId = "123abc",
    companyName = "Customerly",
    success = { /* Called if the task completes successfully */ },
    failure = { /* Called if the task fails */ })
```
`Java v2.0.0`
```java
Customerly.setCompany(
    companyMap,
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task completes successfully
            return null;
        }
    },
    new Callback() {
        @Override
        public Unit invoke() {
            //Called if the task fails
            return null;
        }
    });
```

### Customerly.get().trackEvent (just remove .get())
`Java v1.x.x`
```java
Customerly.get().trackEvent("added_to_cart");
```
`Kotlin v2.0.0`
```kotlin
Customerly.trackEvent(eventName = "added_to_cart")
```
`Java v2.0.0`
```java
Customerly.trackEvent("added_to_cart");
```

### Customerly.get().disableOn e Customerly.get().enableOn (just remove .get())
`Java v1.x.x`
```java
Customerly.get().disableOn(SplashActivity.class);
Customerly.get().enableOn(SplashActivity.class);
```
`Kotlin v2.0.0`
```kotlin
Customerly.disableOn(activityClass = SplashActivity::class)
Customerly.enableOn(activityClass = SplashActivity::class)
```
`Java v2.0.0`
```java
Customerly.disableOn(SplashActivity.class);
Customerly.enableOn(SplashActivity.class);
```

## Proguard

The following rules has been added to the proguard file
```
-keepclasseswithmembers class android.animation.ValueAnimator {
    public static void setDurationScale(float);
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
```

By the way, like as before, should not be necessary to explicitly add them in your proguard file because they should be automatically inherited.

## License
Customerly Android SDK is available under the Apache License 2.0.
See the [Customerly_LICENSE.txt](https://github.com/customerly/Customerly-Android-SDK/blob/master/Customerly_LICENSE.txt) file for more info.
