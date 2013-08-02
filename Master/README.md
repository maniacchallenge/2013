
MANIAC Master
=============

Introduction
------------
The MANIAC Master is the central instance of the backbone. It initiates new transactions and logs all data from the backbone to a MongoDB instance.
The master is written in Google Go because of its concurrency capabilities.

Components
----------
The master is devided in serveral packets that contain related functionality.

- "betternode" contains the functions for handling the backbone nodes and the data they provide.
- "database" contains the functions related to the MongoDB.
- "log" contains the logger of the master.
- "profile" contains the interpreter for the test profiles (for automation).
- "rounds" contains functions for creating new transactions and new rounds.
- "server" contains a TCP server for the backbone connections.
- "tester" is a independent project for testing MapReduce functions in MongoDB.
- "transaction" contains functions for keeping track of running transactions.

How to compile? (Linux only)
---------------------------
- Install Go 1.1.1 on your machine and define the "$GOPATH" environment variable.
- Copy the master code to "$GOPATH/src/master".
- Install the Go-MongoDB-Bindings by running "go install labix.org/v2/mgo" and "go install labix.org/v2/mgo/bson".
- Go to "$GOPATH/src/master" and run "go build".

How to use?
-----------
Run the file "master" in the folder "$GOPATH/src/master".
The master then shows an interactive console. Type "help" for a list of commands.
To run a profile load it by typing "load" [enter] and the file name of the profile.
When no error message is displayed type "list" to list all loaded profiles.
The profile name should be the file name without the extension. To run the profile type "run" [enter] and the name of the profile.

Profiles
--------
The master uses profiles for automatation. The profile is a little script that is processed command by command. A line starting with "#" is a comment. Empty lines are ignored. There are a couple of commands.
- print - Prints text that is specified after the command.
- sleep [num] - Sleeps a given amount of seconds.
- send - Sends a single transaction.
- mode [num] - Enables autosending of transactions. One every [num] milliseconds.
- mode 0 - Disables autosending of transactions
- hops [num] - Sets the number of hops to set in the transactions.
- ceil [num] - Sets the maximal bidding for the transactions.
- fine [num] - Sets the fine to be paid when the transaction fails.
- next - Starts a new round.
