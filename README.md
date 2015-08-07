# MQTTLiga
![Logo](/app/src/main/res/drawable-hdpi/ic_launcher.png?raw=true "MQTTLiga")
<br>
<b>An Android app for live results of the German Bundesliga based on MQTT</b>
<br><br>
MQTTLiga is an app created for football lovers of the German Bundesliga. It supports the 1st Bundesliga and was recently
updated due to a very sad event for the 2nd Bundesliga.
The app receives the messages via MQTT (http://mqtt.org). No polling, no screenscraping, no waste of bandwith - just
the events in small messages with a very small payload.
<br><br>
MQTT is a messaging protocol desinged for the "Internet of Things", connecting different types of small devices
with one extremely lightweight and reliable protocol.
The MQTT server is gathering information about the games from public sources and publishes the results as soon as
an event occurs (start of games, goals, scorers, halftime and end of games). The server publishes the events immediately to the connected subscribers. Sure, there is a small delay compared with a live game....
<br>
If you connect (![discon](/app/src/main/res/drawable/connected.png?raw=true "connected")) with MQTTliga to the server you'll create a durable subscription on the server. Your subscription
stays active for 7 days and will be renewed with every reconnect. During this period all messages from the games
are queued on the server and are sent to your device as soon as you are online again.
Only subscribed devices are receiving live results. So stay connected or reconnect at least once in 7 days and you'll
get the live results of the German Bundesliga.
<br><br>
MQTTLiga is using the well known port 1883 for the MQTT connection. This port may be closed on some networks. Support of websockets in MQTTLiga is planned for future releases.
<br><br>
You can direclty download the latest app release under 'Release' on this page.
<br><br>
Have fun with MQTTLiga - may your favourite team always win (except the games against my favourite team :-) )
