## Swrve Kit Integration

This repository contains the [Swrve](https://www.swrve.com/) integration for the [mParticle Android SDK](https://github.com/mParticle/mparticle-android-sdk).

### Adding the integration
1. Add the Swrve repository and the Google repository to your project-level build.gradle:
	```groovy
	repositories {
		jcenter { url = 'https://dl.bintray.com/swrve-inc/android' }
		maven { url = 'https://maven.google.com' }
	}
	```

2. Add the kit dependency to your app's build.gradle:

    ```groovy
    dependencies {
        compile 'com.swrve.mparticle:mparticle-android-integration-swrve:1.0.0'
    }
    ```
2. Follow the mParticle Android SDK [quick-start](https://github.com/mParticle/mparticle-android-sdk), then rebuild and launch your app, and verify that you see `"Swrve detected"` in the output of `adb logcat`.
3. Reference mParticle's integration docs below to enable the integration.

### Documentation

[Swrve integration](https://docs.mparticle.com/integrations/swrve/event/)

### License

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
