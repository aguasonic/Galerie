# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /devel/Android_SDK/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
-dontshrink

-dontoptimize

-dontpreverify

-verbose

#- This is needed by okio, which is needed by OkHttp
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#- This is needed by okio, which is needed by OkHttp
-dontwarn java.nio.file.*

-dontwarn android.support.v4.app.*

-keepattributes Signature

