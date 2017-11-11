-keep public class com.holger.mqttliga.Scorer
-keep public class com.holger.mqttliga.Events
-keep class com.holger.mqttliga.games

-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

-libraryjars libs

-keep class org.eclipse.paho.client.mqttv3.** { *; }
