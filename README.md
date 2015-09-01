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

apply plugin: 'com.android.application'
apply plugin: 'com.wmbest.rats'

rats {
    // Configuration
}
```

You must running against RATS server version 2.0 or newer

###Tasks

All testable build variants will be tested using the `deviceCheck` task.

###Parameters:

  * `server` The server on which to run (Default `http://localhost:3000`)
  * `user` For use with basic auth protected sites
  * `password` For use with basic auth protected sites
  * `count` Number of devices (Not set by default)
  * `serials` Comma separated list of device serials (Not set by default)
  * `strict` Strict mode (Default `false`, will run until timeout if devices dont match)
  * `timeout` Timeout period in millis (Default 20 minutes)
  * `message` A field for describing the run (useful for git messages, etc.)
