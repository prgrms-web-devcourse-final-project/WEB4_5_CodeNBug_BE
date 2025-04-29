<<<<<<<< HEAD:service/main-server/src/main/java/org/codeNbug/mainserver/MainServerApplication.java
package org.codeNbug.mainserver;
========
package org.codeNbug.queueserver;
>>>>>>>> develop:service/queue-server/src/main/java/org/codeNbug/queueserver/QueueServerApplication.java

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QueueServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueueServerApplication.class, args);
	}

}
