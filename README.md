# [Permission Nanny][app]
[![Download](https://api.bintray.com/packages/littledot/maven/permission-nanny-sdk/images/download.svg)](https://bintray.com/littledot/maven/permission-nanny-sdk/_latestVersion)
[![Build Status](https://travis-ci.org/littledot/Permission-Nanny.svg?branch=master)](https://travis-ci.org/littledot/Permission-Nanny)
[![Coverage Status](https://coveralls.io/repos/github/littledot/Permission-Nanny/badge.svg?branch=master)](https://coveralls.io/github/littledot/Permission-Nanny?branch=master)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/Permission-Nanny/Lobby)

Permission Nanny is an application that can access resources which are protected by permissions on your behalf,
so that your application does not need to declare permission usage in your AndroidManifest.xml. With Permission Nanny,
it is possible for your application to not require ***any*** permissions at all, yet still be able to access
permission-protected resources.

# Why?

Android M introduces a new permission model - [Runtime Permissions][runtime-permissions] - where 3rd party applications
are no longer granted permissions at install time.
Instead, a dialog is displayed to the user, requesting authorization to access permission-protected resources when 3rd
party applications would like to use them during runtime.
However, the new Runtime Permissions model is built into the operating system and is only available in Android M and
onwards.
Pre-M users will have to wait for system updates, which unfortunately could take quite a while.

Permission Nanny is an attempt to backport Runtime Permissions to pre-M devices, all the way to Gingerbread 2.3

# How does it work?

From a high-level perspective, Permission Nanny acts as a proxy server between client applications and the Android
operating system. When a client needs to access a resource that is protected by Android permissions, the client will
send a request to Permission Nanny. Permission Nanny will then show a dialog to the user, asking the user for
authorization to grant the client access to the resource. If the user allows the request, Permission Nanny will
access the resource and return results to the client; if the user denies the request, Permission Nanny will simply
return an error response.

# Will my existing code work with Permission Nanny?

Unfortunately, code changes are required to integrate your app with Permission Nanny. Fortunately, the Permission
Nanny SDK is designed to mimic the Android SDK, hence the amount of work required should be minimal. Since you are
going to make code changes to make your app work with M's Runtime Permissions anyways; why not try something that is
supported all the way back to Gingerbread 2.3?

# How do 3rd party apps communicate with Permission Nanny?

Clients communicate with Permission Nanny using broadcast Intents following the Permission Police Protocol (PPP). PPP
is heavily inspired by HTTP with a few minor tweaks, borrowing attributes such as status codes, headers and entity.
See [Nanny.java][nanny-java] for the full PPP specification.

# How do I integrate my apps with Permission Nanny?

A Permission Nanny SDK is provided to facilitate issuing requests to and handling responses from Permission Nanny.

```groovy
dependencies {
    compile 'com.permission-nanny:permission-nanny-sdk:a.b.c'
}
```

## How do I make a request?

Use one of the static factory methods to create a request; attach a listener to receive results; finally send the
request to Permission Nanny with `.startRequest()`. See [PermissionRequest.java][permission-request-java] for more
documentation. For example, to issue a `WifiManager.getConnectionInfo()` request:

```java
WifiRequest request = WifiRequest.getConnectionInfo().listener(new SimpleListener() {
    public void onResponse(Bundle response) {
        Bundle entity = response.getBundle(Nanny.ENTITY_BODY);
        if (Nanny.SC_OK != response.getInt(Nanny.STATUS_CODE)) {
            NannyException error = (NannyException) entity.getSerializable(Nanny.ENTITY_ERROR);
            return;
        }
        WifiInfo info = entity.getParcelable(WifiRequest.GET_CONNECTION_INFO);
    }
}).startRequest(context, "Trust me");
```

## How do I know if the user grants or denies my request for resources?

Permission Nanny will return a response in the form of a Bundle. The response will contain a status code which will
tell you the user's decision. If the user authorizes the request, the response will contain the requested resources;
otherwise, it will contain an error message.

# What resources are supported?

Currently, AccountManager, LocationManager, SmsManager, TelephonyManager, WifiManager and all Content Providers are
supported. See [request factories][simple-pkg] for a detailed list of all available requests.

# Are there any examples?

Check out the [/appDemo][appdemo-main-activity-java] directory or the demo app [Permission Nanny Demo][demo-app]
on Google Play.

# Where can I find complete documentation?

[Documentation][docs] is available but still a work-in-progress. [Nanny.java][nanny-java] & [PermissionRequest.java]
[permission-request-java] are the most extensive documentation and should cover most areas of Permission Nanny.

# Where can I ask questions?

Please direct your questions to the [Permission Nanny mailing list][mailing-list]; I will try my best to answer them.

# License

See [LICENSE](LICENSE.md) for details.

[nanny-java]: http://littledot.github.io/Permission-Nanny/com/permissionnanny/lib/Nanny.html
[permission-request-java]: http://littledot.github.io/Permission-Nanny/com/permissionnanny/lib/request/PermissionRequest.html
[simple-pkg]: http://littledot.github.io/Permission-Nanny/com/permissionnanny/lib/request/simple/package-summary.html
[appdemo-main-activity-java]: appDemo/src/main/java/com/permissionnanny/demo/MainActivity.java
[app]: https://play.google.com/store/apps/details?id=com.permissionnanny
[demo-app]: https://play.google.com/store/apps/details?id=com.permissionnanny.demo
[runtime-permissions]: https://developer.android.com/preview/features/runtime-permissions.html
[docs]: http://littledot.github.io/Permission-Nanny/
[mailing-list]: https://groups.google.com/forum/#!forum/permission-nanny/
