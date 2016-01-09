package hello;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundKeyOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

/**
 * 比RedisAtomicLong增加了这个函数
 * 
 * <pre>  {@code
 * public Long getExpire(TimeUnit unit)
 * }
 * </pre>
 * 
 * Atomic long backed by Redis. Uses Redis atomic increment/decrement and watch/multi/exec operations for CAS
 * operations.
 * 
 * @see java.util.concurrent.atomic.AtomicLong
 * @author Costin Leau
 * @author Thomas Darimont
 */
public class MyRedisAtomicLong extends Number implements Serializable, BoundKeyOperations<String> {

    private static final long serialVersionUID = 1L;

    private volatile String key;
    private ValueOperations<String, Long> operations;
    private RedisOperations<String, Long> generalOps;

    /**
     * Constructs a new <code>RedisAtomicLong</code> instance. Uses the value existing in Redis or 0 if none is found.
     * 
     * @param redisCounter redis counter
     * @param factory connection factory
     */
    public MyRedisAtomicLong(String redisCounter, RedisConnectionFactory factory) {
        this(redisCounter, factory, null);
    }

    /**
     * Constructs a new <code>RedisAtomicLong</code> instance.
     * 
     * @param redisCounter
     * @param factory
     * @param initialValue
     */
    public MyRedisAtomicLong(String redisCounter, RedisConnectionFactory factory, long initialValue) {
        this(redisCounter, factory, Long.valueOf(initialValue));
    }

    private MyRedisAtomicLong(String redisCounter, RedisConnectionFactory factory, Long initialValue) {
        Assert.hasText(redisCounter, "a valid counter name is required");
        Assert.notNull(factory, "a valid factory is required");

        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<String, Long>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.setExposeConnection(true);
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();

        this.key = redisCounter;
        this.generalOps = redisTemplate;
        this.operations = generalOps.opsForValue();

        if (initialValue == null) {
            if (this.operations.get(redisCounter) == null) {
                set(0);
            }
        } else {
            set(initialValue);
        }
    }

    /**
     * Constructs a new <code>RedisAtomicLong</code> instance. Uses the value existing in Redis or 0 if none is found.
     * 
     * @param redisCounter the redis counter
     * @param template the template
     * @see #MyRedisAtomicLong(String, RedisConnectionFactory, long)
     */
    public MyRedisAtomicLong(String redisCounter, RedisOperations<String, Long> template) {
        this(redisCounter, template, null);
    }

    /**
     * Constructs a new <code>RedisAtomicLong</code> instance.
     * <p>
     * Note: You need to configure the given {@code template} with appropriate {@link RedisSerializer} for the key and
     * value. The key serializer must be able to deserialize to a {@link String} and the value serializer must be able to
     * deserialize to a {@link Long}.
     * <p>
     * As an alternative one could use the {@link #MyRedisAtomicLong(String, RedisConnectionFactory, Long)} constructor
     * which uses appropriate default serializers, in this case {@link StringRedisSerializer} for the key and
     * {@link GenericToStringSerializer} for the value.
     * 
     * @param redisCounter the redis counter
     * @param template the template
     * @param initialValue the initial value
     */
    public MyRedisAtomicLong(String redisCounter, RedisOperations<String, Long> template, long initialValue) {
        this(redisCounter, template, Long.valueOf(initialValue));
    }

    private MyRedisAtomicLong(String redisCounter, RedisOperations<String, Long> template, Long initialValue) {

        Assert.hasText(redisCounter, "a valid counter name is required");
        Assert.notNull(template, "a valid template is required");
        Assert.notNull(template.getKeySerializer(), "a valid key serializer in template is required");
        Assert.notNull(template.getValueSerializer(), "a valid value serializer in template is required");

        this.key = redisCounter;
        this.generalOps = template;
        this.operations = generalOps.opsForValue();

        if (initialValue == null) {
            if (this.operations.get(redisCounter) == null) {
                set(0);
            }
        } else {
            set(initialValue);
        }
    }

    /**
     * Gets the current value.
     * 
     * @return the current value
     */
    public long get() {
        return operations.get(key);
    }

    /**
     * Sets to the given value.
     * 
     * @param newValue the new value
     */
    public void set(long newValue) {
        operations.set(key, newValue);
    }

    /**
     * Atomically sets to the given value and returns the old value.
     * 
     * @param newValue the new value
     * @return the previous value
     */
    public long getAndSet(long newValue) {
        return operations.getAndSet(key, newValue);
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
     * 
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that the actual value was not equal to the expected value.
     */
    public boolean compareAndSet(final long expect, final long update) {
        return generalOps.execute(new SessionCallback<Boolean>() {

            @SuppressWarnings("unchecked")
            public Boolean execute(RedisOperations operations) {
                for (;;) {
                    operations.watch(Collections.singleton(key));
                    if (expect == get()) {
                        generalOps.multi();
                        set(update);
                        if (operations.exec() != null) {
                            return true;
                        }
                    }
                    {
                        return false;
                    }
                }
            }
        });
    }

    /**
     * Atomically increments by one the current value.
     * 
     * @return the previous value
     */
    public long getAndIncrement() {
        return incrementAndGet() - 1;
    }

    /**
     * Atomically decrements by one the current value.
     * 
     * @return the previous value
     */
    public long getAndDecrement() {
        return decrementAndGet() + 1;
    }

    /**
     * Atomically adds the given value to the current value.
     * 
     * @param delta the value to add
     * @return the previous value
     */
    public long getAndAdd(final long delta) {
        return addAndGet(delta) - delta;
    }

    /**
     * Atomically increments by one the current value.
     * 
     * @return the updated value
     */
    public long incrementAndGet() {
        return operations.increment(key, 1);
    }

    /**
     * Atomically decrements by one the current value.
     * 
     * @return the updated value
     */
    public long decrementAndGet() {
        return operations.increment(key, -1);
    }

    /**
     * Atomically adds the given value to the current value.
     * 
     * @param delta the value to add
     * @return the updated value
     */
    public long addAndGet(long delta) {
        return operations.increment(key, delta);
    }

    /**
     * Returns the String representation of the current value.
     * 
     * @return the String representation of the current value.
     */
    public String toString() {
        return Long.toString(get());
    }

    public int intValue() {
        return (int) get();
    }

    public long longValue() {
        return get();
    }

    public float floatValue() {
        return (float) get();
    }

    public double doubleValue() {
        return (double) get();
    }

    public String getKey() {
        return key;
    }

    public Boolean expire(long timeout, TimeUnit unit) {
        return generalOps.expire(key, timeout, unit);
    }

    public Boolean expireAt(Date date) {
        return generalOps.expireAt(key, date);
    }

    public Long getExpire() {
        return generalOps.getExpire(key);
    }
    
    public Long getExpire(TimeUnit unit) {
        return generalOps.getExpire(key, unit);
    }

    public Boolean persist() {
        return generalOps.persist(key);
    }

    public void rename(String newKey) {
        generalOps.rename(key, newKey);
        key = newKey;
    }

    public DataType getType() {
        return DataType.STRING;
    }
}
