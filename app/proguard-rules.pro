<<<<<<< HEAD
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
-keep class org.kodein.** { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(org.kodein.di.DI);
}

-keep class com.a10miaomiao.bilimiao.store.** { *; }#for WindowStore 
=======
>>>>>>> aaafd0e3 (引入koin计划迁移kodein到koin&proguard-rules.pro留空不再keep kodein相关 ci开启debug构建禁用Release构建)
