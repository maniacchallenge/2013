# ManiacLib
This is the part that goes on your flashed Android device. (See [how to get your nexus 7 ready](https://github.com/maniacchallenge/2013/wiki/How-to-get-your-Nexus-7-ready) on how to do that)  
Please note that you will have to start the [MANET manager](https://play.google.com/store/apps/details?id=org.span&hl=de) and open an ad hoc net with it before you run anything that uses the ManiacLib (otherwise, it will crash brutally).

``com.example.maniaclib`` will provide you with a dummy App that shows you how to use this Lib. *The App is neither stable nor setting a good example. Please write your own.*

## protobuf
The Eclipse project contains a file called ``protoPackets.proto`` in its ``/src``folder which specifies our protobuf message format, as well as the .java files compiled from it.