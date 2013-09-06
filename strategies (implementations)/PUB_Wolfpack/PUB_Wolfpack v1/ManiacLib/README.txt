
OlsrInfo
========

OlsrInfo provides Java representations of mesh status and config data
retrieved from a running olsrd via a network socket.  This library includes
two key classes: JsonInfo and TxtInfo.  These two classes are matched to olsrd
plugins of the same name.

OLSR is a protocol for mesh networking, and the olsrd implementation from
http://olsr.org is the focus of this library.


TxtInfo
-------

This plugin provides basic overview information and should work with olsrd
versions 0.6 and newer. It is possible to use the TxtInfo.java file on its own
without any other external dependencies.


JsonInfo
--------

This plugin provides thorough information, including some information about
the wifi settings.  It required olsrd 0.6.4 or newer. It is built on top of
the Jackson v1.9.7 JSON parsing library (http://jackson.codehaus.org/).  In
particular, it was developed using jackson-core.jar and jackson-mapper.jar
from here: http://wiki.fasterxml.com/JacksonDownload



This was written as part of the Commotion project:
http://commotionwireless.net/
