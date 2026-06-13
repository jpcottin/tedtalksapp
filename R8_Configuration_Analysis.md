# R8 Configuration Analysis

## Build configuration

- **AGP:** 9.2.1 — app optimization (R8) is the modern default; no upgrade needed.
- **Release build type** (`app/build.gradle.kts`):
  - `isMinifyEnabled = true` — R8 shrinking + obfuscation enabled.
  - `isShrinkResources = true` — unused resources stripped.
  - `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
  - `signingConfig = signingConfigs.getByName("debug")` — debug-signed so the
    minified build is installable for local verification. **Replace with a real
    release key before publishing.**

## Result

- Debug APK: ~23.7 MB → Release APK: **2.4 MB** (~90% smaller).
- R8 produced **no `missing_rules.txt`** — nothing in the app or its
  dependencies required additional keep rules.

## Keep-rule analysis

The app initially carried three custom rules for the `@Serializable` Navigation 3
nav keys (`TalksList`, `TalkDetail`):

```
-keep @kotlinx.serialization.Serializable class com.jpcexample.tedtalks.** { *; }
-keepclassmembers class com.jpcexample.tedtalks.** { *** Companion; }
-keepclasseswithmembers class com.jpcexample.tedtalks.** { kotlinx.serialization.KSerializer serializer(...); }
```

**Action taken: all three removed.**

- They were **package-wide wildcards** (`com.jpcexample.tedtalks.**`) keeping all
  members and blocking obfuscation — the broadest, least optimization-friendly
  shape of rule.
- They **duplicated library consumer rules.** `kotlinx-serialization-core:1.7.3`
  (on the release classpath transitively via Navigation 3) ships its own
  `META-INF/proguard/` rules that keep generated serializers and `Companion`
  accessors for `@Serializable` types.
- Removal was **empirically validated**: a rules-free minified release build
  parses the live RSS feed, navigates list↔detail, and restores the back stack
  across simulated process death (`am kill` from background → relaunch lands on
  the detail pane). APK size is identical with and without the rules, confirming
  they contributed nothing.

No keep rules remain. `proguard-rules.pro` documents why it is intentionally empty.

## Note on kxml2

`net.sf.kxml:kxml2` is `testImplementation` only. At runtime the RSS parser uses
the Android platform's built-in `XmlPullParser`, so no keep rule is needed for
XML parsing in the release APK.

## Recommended verification

Run the instrumented UI tests against the **release** variant, plus a manual
pass on the packages exercised by serialization (navigation / back-stack
restore), to confirm shrinking introduced no regressions:

```
./gradlew :app:connectedReleaseAndroidTest   # needs a release-signed test build
```
