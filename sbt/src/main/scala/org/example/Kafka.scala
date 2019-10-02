package org.example

import java.util.Properties
import java.util.concurrent.TimeUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

// import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode
import scala.util.parsing.json.JSONObject
import org.apache.flink.api.scala._
import org.apache.flink.api.java.utils.ParameterTool
// import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema // 廃止されたっぽい
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor

object Kafka {
  val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def main(args: Array[String]): Unit = {

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // Event Time
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    // Parameters
    val params: ParameterTool = ParameterTool.fromArgs(args)
    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)
    println(params.get("broker"))
    println(params.get("topic"))

    // Kafka connector properties
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("broker"))
    properties.setProperty("group.id", "org.apache.flink")

    // Kafka consumer withe schema to deserialize the data
    val consumer = new FlinkKafkaConsumer010(params.get("topic"), new JSONKeyValueDeserializationSchema(true), properties)
    val publisher = new FlinkKafkaProducer010[String](params.get("broker"), "out", new SimpleStringSchema)

    // Kafka start position
    consumer.setStartFromLatest()

    val events = env.addSource(consumer)

    val timestamped = events.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[ObjectNode](Time.seconds(10)) {
          override def extractTimestamp(element: ObjectNode): Long = {
            val v = x.get("value")
            val t = v.get("created").asLong
            return t * 1000 // to msec ?
          }
    })

    // Assign watermarks
    // val withTimestampsAndWatermarks = stream.assignAscendingTimestamps( _.getCreationTime )

    val data = timestamped
//    val data = events
        .map{ x =>
          val v = x.get("value")
          val key = v.get("category").asText
          val score = v.get("score").asDouble
          val cost = v.get("cost").asDouble
          println(v)
          (key, cost, score)
        }
//        .keyBy(0)
//        .timeWindow(Time.of(2500, TimeUnit.MILLISECONDS), Time.of(500, TimeUnit.MILLISECONDS)) // Should be after Key
//        .timeWindow(Time.of(2500, TimeUnit.MILLISECONDS))
//        .countWindow(5, 1)
//        .window(SlidingEventTimeWindows.of(Time.seconds(5), Time.seconds(1)))
//        .min(1)
//        .map { v =>
//          println(v)
//          v
//          // val zdt = new Date(v.time).toInstant().atZone(ZoneId.systemDefault())
//          // val time = fmt.format(zdt)
//          // val json = Map("time" -> time, "bid" -> v.bid, "min" -> v.min)
//          // val retval = JSONObject(json).toString()
//          // println(retval)
//          // retval
//        }
//      .sum(1)
//      .addSink(publisher)
//      .name("kafka")

    // execute and print result
    data.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")

    env.execute("Flink Scala Kafka Word Count Example")
  }
}
