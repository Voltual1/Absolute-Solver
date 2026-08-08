-keep class com.a10miaomiao.bilimiao.comm.store.** { *; }
-keepattributes Signature

-keep,allowobfuscation,allowoptimization class org.kodein.type.TypeReference
-keep,allowobfuscation,allowoptimization class org.kodein.type.JVMAbstractTypeToken$Companion$WrappingTest

-keep,allowobfuscation,allowoptimization class * extends org.kodein.type.TypeReference
-keep,allowobfuscation,allowoptimization class * extends org.kodein.type.JVMAbstractTypeToken$Companion$WrappingTest

-keepclassmembers class com.a10miaomiao.bilimiao.comm.store.** {
    <init>(org.kodein.di.DI);
    <init>(org.kodein.di.DI, ...);
}
-keep class com.a10miaomiao.bilimiao.comm.store.** { *; }
