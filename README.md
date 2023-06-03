### Build

```bash
mvn install
```


### Run

```bash
java -javaagent:./agent/target/agent-1.0-SNAPSHOT.jar -jar ./test-app/target/test-app-1.0-SNAPSHOT.jar
```


Run with debug including agent code

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -javaagent:./agent/target/agent-1.0-SNAPSHOT.jar -jar ./test-app/target/test-app-1.0-SNAPSHOT.jar
```

Then just attach to the port by Idea