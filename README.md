# MiniDB

## Introduction

Mini is a simple database implemented in Java, with some principles borrowed from MySQL, PostgreSQL, and SQLite. It implements the following features:

- Data reliability and recovery
- Two-phase locking protocol (2PL) to achieve serializable scheduling
- MVCC (Multi-Version Concurrency Control)
- Two transaction isolation levels (Read Committed and Repeatable Read)
- Deadlock handling
- Basic table and field management
- Basic SQL parsing (due to laziness in writing lexical analysis and automata, the parsing is relatively simple)
- Server and client based on sockets

## How to Run

First, adjust the compilation version in pom.xml. If you import the project into an IDE, change the project's compilation version to suit your JDK.

First, compile the source code by executing the following command:

```shell
mvn compile
```

Then create a database at the path /tmp/minidb by executing the following command:

```shell
mvn exec:java -Dexec.mainClass="com.hnv99.minidb.backend.Launcher" -Dexec.args="-create /tmp/minidb"
```

Then start the database service with the default parameters by executing the following command:

```shell
mvn exec:java -Dexec.mainClass="com.hnv99.minidb.backend.Launcher" -Dexec.args="-open /tmp/minidb"
```

At this point, the database service has started on port 9999 on the local machine. Restart another terminal and execute the following command to start the client to connect to the database:

```shell
mvn exec:java -Dexec.mainClass="com.hnv99.minidb.client.Launcher"
```

This will start an interactive command-line interface where you can input SQL-like syntax. Press Enter to send the statements to the service and output the execution results.

