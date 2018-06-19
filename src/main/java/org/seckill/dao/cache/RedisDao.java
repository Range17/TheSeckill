package org.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDao {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    jedis连接池
    private final JedisPool jedisPool;

//    利用protostuff，
    private RuntimeSchema<Seckill> schema=RuntimeSchema.createFrom(Seckill.class);

//    构造方法
    public RedisDao(String ip, int port) {
        jedisPool = new JedisPool(ip, port);
    }




//    从redis中得到seckill对象
    public Seckill getSeckill(long seckillId) {
            //redis操作逻辑
            try {
            Jedis jedis = jedisPool.getResource();
                try {
                    String key = "seckill:" + seckillId;
                    // 并没有实现内部序列化操作
                    // get -> byte[] -> 反序列化 -> object[Seckill]
                    // 采用自定义序列化，直接写成二进制，传递给redis缓存起来
                    // protostuff使用的对象 : pojo.

//                    传递字节数组
                    byte[] bytes = jedis.get(key.getBytes());
                    if (bytes != null) {//说明获取到，需要转换
                        Seckill seckill = schema.newMessage();//创建了一个空对象
                        ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
    //                    上面之后seckill被反序列
                        return seckill;
                    }
                } finally {
                    jedis.close();
                }
            }catch (Exception e){
                logger.error(e.getMessage(), e);
            }
            return null;
        }

//    当缓存没有的时候从数据库拿出来
    public String putSeckill(Seckill seckill){
        // set Object(Seckill) -> 序列化 -> byte[]->发送给redis
        try {
            Jedis jedis=jedisPool.getResource();
            String key="seckill:"+seckill.getSeckillId();
            try {
                byte[] bytes=ProtostuffIOUtil.toByteArray(seckill,schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                // 超时缓存jedis.setex
                int timeout=60*60;//一小时
                String result=jedis.setex(key.getBytes(),timeout,bytes);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return null;
    }
}
