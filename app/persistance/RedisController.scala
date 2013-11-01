package persistance

import com.redis.RedisClient
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration._

object RedisController {
  implicit private val system = ActorSystem("redis-client")
  implicit private val executionContext = system.dispatcher
  implicit private val timeout: akka.util.Timeout = Timeout(5 seconds)
  val client = RedisClient("ec2-54-229-139-146.eu-west-1.compute.amazonaws.com", 6379)
  
  def expire(key: String, le_timeout: Int) = client.expire(key, le_timeout)
  def apply(key: String) = client.get(key)
  def update(key: String, value: String) = client.set(key, value)
}