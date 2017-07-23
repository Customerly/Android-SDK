<p align="center">
<a href="http://www.customerly.io">
<img src="https://www.cdn.customerly.io/assets/img/Logo_Customerly_Name_Colored.svg">
</a>
</p>

  
  [![Android API14+](https://img.shields.io/badge/Android-API_14+-green.svg)]()
  [![Java 6+](https://img.shields.io/badge/Java-6+-red.svg)]()
  [![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-red.svg)]()
  
**customerly.io** is the perfect tool to getting closer to your customers. Help them where they are with the customer support widget. Manage your audience based on their behaviours, build campaigns and automations.

Deliver Surveys directly into your app and get the responses in one place. Study your Net Promote Score and Skyrocket your Online Business right now.

The Customerly Android SDK is really simple to integrate in your apps, and allow your users to contact you via chat.

<p align="center">
  <img src="https://raw.githubusercontent.com/customerly/customerly.github.io/master/android/resources/chat-preview.jpg?raw=true" width=500 alt="Icon"/>
</p>

## Features

- [x] Register your users
- [x] Set attributes
- [x] Track events
- [x] Support via chat in real time
- [x] Surveys
- [x] English & Italian localizations
- [x] Many more is coming....

## Requirements

- Android 4.0.1+ (API level 14+)
- Android Studio 2.0+
- Java 6+

## Setup Customerly SDK

### Integration via Gradle or Maven dependency (Recommended)

To integrate the Customerly SDK we recommend to use Gradle or Maven dependency

Instead of VERSION_NAME use the latest version name hosted on bintray:  [ ![Download](https://api.bintray.com/packages/giannign1/maven/customerly-android-sdk/images/download.svg) ](https://bintray.com/giannign1/maven/customerly-android-sdk/_latestVersion)

##### Gradle
In your module `build.gradle` add:

```gradle
buildscript {
    repositories {
        jcenter()
    }
}

dependencies {
    compile 'io.customerly:customerly-android-sdk:VERSION_NAME'
}
```

##### Maven
```xml
<dependency>
  <groupId>io.customerly</groupId>
  <artifactId>customerly-android-sdk</artifactId>
  <version>VERSION_NAME</version>
  <type>pom</type>
</dependency>
```

### Manual integration (Discouraged)

We suggest to avoid manually integration of the SDK in your project, by the way it is still possible:

1. Download the `/customerly-android-sdk` folder and add it as module in your project.  
2. Congratulations!  

We recommend to use only the public methods of the class Customerly.

### Configuration (Mandatory)

**1)** Create a new AndroidStudio project or open an existing one

**2)** If you already have defined a custom Application class in your project you can skip to step 3).  
Create a class that extends the default android Application class
```java
public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //See step 3)
    }
}
```
Don't forget to declare this class in your `AndroidManifest.xml` file:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="YOUR.PACKAGE">

    <application
        android:name=".CustomApplication" >
        //...
    </application>
</manifest>
```

**3)** Now you need to configure the Customerly SDK in your custom Application class onCreate method:

```java
public class CustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID");
    }
}
```
*OPTIONALLY*, if you want to specify a custom widget_color for the application ignoring the server-defined one you have to specify it in the configure method:
```java
Customerly.configure(this, "YOUR_CUSTOMERLY_APP_ID", Color.RED);
```
*OPTIONALLY*, after the configure, if you want to enable the logging in console you have to call the following method.  
Our suggest is to call it soon after the configure:
```java
Customerly.get().setVerboseLogging(BuildConfig.DEBUG);//Passing BuildConfig.DEBUG, logging will be automatically disabled for the release apk
```

*If in doubt, you can look at the examples in the demo application. (See the demo's [CustomApplication.java](https://github.com/customerly/Customerly-Android-SDK-demo/blob/master/app/src/main/java/io/customerly/demo/CustomApplication.java))*

### Obtain the SDK singleton reference

You can obtain a reference to the SDK singleton by calling.

```java
Customerly reference = Customerly.get();
```

### Chat (Mandatory)

Don't forget to show a button or something to your user for opening the Support Activity. Just call the method `openSupport`:

```java
Customerly.get().openSupport(Activity.this);
```

<p align="center">
  <img src="https://raw.githubusercontent.com/customerly/customerly.github.io/master/android/resources/chat.gif?raw=true" width=200 alt="Chat"/>
</p>

### Survey (No action required)

With the Customerly SDK you can deliver surveys directly into your app without any lines of code.

They will be automatically displayed to your user as soon as possible.

<p align="center">
  <img src="https://raw.githubusercontent.com/customerly/customerly.github.io/master/android/resources/survey.gif?raw=true" width=200 alt="Survey"/>
</p>

### User registration and logout (Optional but recommended)

If you don't have a login method inside your apps don't worry, users can use the chat inserting their emails.

By the way, if users login in your application please register them into Customerly calling the method `registerUser:`.  

Example:

```java
Customerly.get().registerUser("axlrose@example.com").start();//Don't forget to call the start method. It starts the task!
```

As seen above, you only need an email address to register the user
*BUT, OPTIONALLY,* you can chain one or more other stuff to the registerUser task:

```java
Customerly.get().registerUser("axlrose@example.com")
.user_id("12345")                       //OPTIONALLY you can pass the user ID 
.name("Gianni")                         //OPTIONALLY you can pass the user name
.attributes(attributesMap)              //OPTIONALLY you can pass some custom attributes (See the *Attributes* section below for the map building)
.company(companyMap)                    //OPTIONALLY you can pass the user company informations (See the *Companies* section below for the map building)
.successCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the success of the task
              @Override
              public void callback() {
                 //Called if the task completes successfully
              }
          })
.failureCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the failure of the task
            @Override
            public void callback() {
               //Called if the task fails
            }
        })
.start();                               //Don't forget to call the start method. It starts the task!
```

So *user_id*, *name*, *attributes*, *company*, *success* and *failure* are totally optional.

Please remember to logout users from customerly when they logout in your application:

```java
Customerly.get().logoutUser();
```

### Attributes (Optional)

Inside attributes you can add every custom data you prefer to track for user segmentation.  
Attributes can be only String, char, int, long, float, double and boolean.

```java
// Eg. This is an attribute map that contains the experience in year and the job of the current user
HashMap<String, Object> attributesMap = new HashMap<String, Object>();
attributesMap.put("experience", 3);
attributesMap.put("job", "Employee");
```

The map above can be passed as parameter of the registerUser or passed in a second time for already registered user:
```java
Customerly.get().setAttributes(attributesMap)
    .successCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the success of the task
                  @Override
                  public void callback() {
                     //Called if the task completes successfully
                  }
              })
    .failureCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the failure of the task
                @Override
                public void callback() {
                   //Called if the task fails
                }
            })
    .start();                               //Don't forget to call the start method. It starts the task!
```

### Companies (Optional)

If in your application the user can handle two or more companies maybe you would like to add some attributes related to them
The Company map MUST contain the following two key/values:  
```
"company_id" -> containing the id of the company  
"name" -> containing the name of the company
```
Then you can add as many company attributes as you want, but remember: they can be only String, char, int, long, float, double and boolean.

```java
// Eg. This is a company map that contains the company mandatory fields (company_id and name) and a custom "foundation year" attribute
HashMap<String, Object> companyMap = new HashMap<String, Object>();
companyMap.put("company_id", "123abc");//Mandatory
companyMap.put("name", "Customerly");//Mandatory
companyMap.put("foundation year", 2017);//Optionally any other values
```

The map above can be passed as parameter of the registerUser or passed in a second time for already registered user:
```java
Customerly.get().setCompany(companyMap)
    .successCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the success of the task
                  @Override
                  public void callback() {
                     //Called if the task completes successfully
                  }
              })
    .failureCallback(new Customerly.Callback() {    //OPTIONALLY you can pass a callback to be notified of the failure of the task
                @Override
                public void callback() {
                   //Called if the task fails
                }
            })
    .start();                               //Don't forget to call the start method. It starts the task!
```

Un utility builder can be used to build a company hashmap correctly

```java
// This is the same hashmap created with the builder what helps with the mandatory fields (company_id and name)
HashMap<String, Object> companyMap = new Customerly.CompanyBuilder("123abc", "Customerly").put("foundation year", 2017).build();
```

### Events (Optional)

Send to Customerly every event you want to segment users better

```java
// Eg. This send an event that track a potential purchase
Customerly.get().trackEvent("added_to_cart");
```

### Disable popups and surveys on specific Activities

You can specify some Activities that will never display messages popup and/or surveys  
This feature is commonly used for SplashScreen that should never display nothing more than their self  
The best place for using it is in the Application onCreate after the Customerly configuration but you can call this from wherever you want  
Every Activity is enabled as default

```java
Customerly.get().disableOn(SplashActivity.class);
```

Once you have disabled and Activity, you can re-enable it by calling this method

```java
Customerly.get().enableOn(SplashActivity.class);
```

## JavaDoc

Explore the SDK [JavaDoc](https://customerly.github.io/android/javadoc/BETA-3.1.8+)

## Permissions

The following permission will be AUTOMATICALLY added to the merged AndroidManifest of your application:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="YOUR.PACKAGE">
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    //...
</manifest>
```
`ACCESS_NETWORK_STATE` is used to verify if an internet connection is available  
`INTERNET` is used to perform https requests  
`READ_EXTERNAL_STORAGE` is used to upload file attachments  
`WRITE_EXTERNAL_STORAGE` is used to save file attachments

## Proguard

The library needs the following three rules to work with proguard enabled
```
-dontwarn java.lang.invoke.*
-dontwarn okio.**
-dontwarn io.customerly.**
```

By the way should not be necessary to explicitly add them in your proguard file because they should be automatically inherited.

## Get updated

If you want to be notified for every updates of SDK please sign up in our telegram channel: https://t.me/joinchat/AAAAAEJZXkvxaa2HAS9EmQ

## Contributing

- If you **need help** or you'd like to **ask a general question**, open an issue or contact our support on [Customerly.io](https://www.customerly.io)
- If you **found a bug**, open an issue.
- If you **have a feature request**, open an issue.
- If you **want to contribute**, submit a pull request.


## Acknowledgements

Made with ❤️ by [Gianni Genovesi](https://www.linkedin.com/in/ggenovesi/) for Customerly.


## License

Customerly Android SDK is available under the Apache License 2.0.  
See the [Customerly_LICENSE.txt](https://github.com/customerly/Customerly-Android-SDK/blob/master/Customerly_LICENSE.txt) file for more info.
