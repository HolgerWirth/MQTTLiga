-keep public class com.holger.mqttliga.Scorer
-keep public class com.holger.mqttliga.Events
-keep class com.holger.mqttliga.games

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-keepclassmembers class ** {
public void onEvent*(**);
} 

-libraryjars libs

-keep class org.eclipse.paho.client.mqttv3.** { *; }
