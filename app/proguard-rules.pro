-keepclassmembers class com.a10miaomiao.bilimiao.comm.store.** {
    <init>(org.kodein.di.DI);
    <init>(org.kodein.di.DI, ...);
}

-keep class com.a10miaomiao.bilimiao.comm.store.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*