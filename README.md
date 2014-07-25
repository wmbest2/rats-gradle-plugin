[RATS](https://www.github.com/wmbest2/rats-server) plugin for the android gradle build tools

##Setup

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:0.10.+'
        classpath 'com.wmbest.gradle:rats:0.2.+'
    }
}

apply plugin: 'android'
apply plugin: 'rats'

rats {
    // Configuration
}
```

You must running against RATS server version 2.0 or newer

###Tasks

All testable build variants will obtain a specialized task

For example if I have build type `debug` I will want to run the
`remoteDebugTest` task.

###Parameters:

  * `server` The server on which to run (Default `http://localhost:3000`)
  * `count` Number of devices (Not set by default)
  * `serials` Comma separated list of device serials (Not set by default)
  * `strict` Strict mode (Default `false`, will run until timeout if devices dont match)
  * `timeout` Timeout period in millis (Default 20 minutes)
