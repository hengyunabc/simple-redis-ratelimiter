package hello;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootApplication
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) throws InterruptedException {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);
		
		RedisConnectionFactory redisConnectionFactory = ctx.getBean(RedisConnectionFactory.class);
		
		MyRedisAtomicLong counter = new MyRedisAtomicLong("counter", redisConnectionFactory);
		
		RedisRateLimiter limiter = new RedisRateLimiter(counter, 100);
		
		int count = 0;
		while(true){
		    boolean acquire = limiter.tryAcquire();
		    if(acquire == true){
		        System.err.println("count:" + count++);
		    }else{
		        TimeUnit.MILLISECONDS.sleep(1);
		    }
		}

//		System.exit(0);
	}
}
