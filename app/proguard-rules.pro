# Lahzei keeps minification disabled, so these rules are precautionary.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
