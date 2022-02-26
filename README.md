---

tightrope
=============

tightrope is load balancer that I'm using to get better at Java development and learn Netty. DO NOT use this in production.

This thing is probably littered with mistakes. Check out HAProxy if you want something for real production use.

### Execution

```bash
$mvn clean package
$java -Dlisten.port=9002 -Dupstream.servers=127.0.0.1:8080,127.0.0.1:18080 -jar target/tightrope-1.0-SNAPSHOT.jar            
```