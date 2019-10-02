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

    // Kafka consumer/publisher withe schema to deserialize/serialize the data
    val consumer = new FlinkKafkaConsumer010(params.get("topic"), new JSONKeyValueDeserializationSchema(true), properties)
    val publisher = new FlinkKafkaProducer010[String](params.get("broker"), "out", new SimpleStringSchema)

    // Kafka start position
    consumer.setStartFromLatest()

    val events = env.addSource(consumer)

    val timestamped = events.assignTimestampsAndWatermarks(
      new BoundedOutOfOrdernessTimestampExtractor[ObjectNode](Time.seconds(10)) {
          override def extractTimestamp(element: ObjectNode): Long = {
            val v = element.get("value")
            val t = v.get("created").asLong
            return t * 1000 // to msec ?
          }
    })

    val data = timestamped
        .map{ x =>
          val v = x.get("value")
          val key = v.get("category").asText
          val score = v.get("score").asDouble
          val cost = v.get("cost").asDouble
          val id = v.get("insert-id").asText
          val time = v.get("timestamp").asText
          println(v)
          (key, cost, score, id)
        }
        .keyBy(0)
        .window(SlidingEventTimeWindows.of(Time.seconds(30), Time.seconds(10))) // 短すぎると安定しないので注意
        .min(1)
        .map { v =>
          println(v)
          val json = Map("category" -> v._1, "cost" -> v._2, "time" -> v._5, "id" -> v._4 )
          val retval = JSONObject(json).toString()
          println(retval)
          retval
        }
        .addSink(publisher)
        .name("kafka")

    // execute and print result
    // data.print()

    env.execute("Flink Scala Kafka Word Count Example")
  }
}
