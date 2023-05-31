### Build

```bash
mvn install
```


### Run

```bash
java -javaagent:./agent/target/agent-1.0-SNAPSHOT.jar -jar ./test-app/target/test-app-1.0-SNAPSHOT.jar
```