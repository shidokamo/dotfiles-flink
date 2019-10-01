package org.example

import java.util.Properties
import java.util.concurrent.TimeUnit
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.api.scala._
import org.apache.flink.api.java.utils.ParameterTool
// import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema // 廃止されたっぽい
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time

object Kafka {
  def main(args: Array[String]): Unit = {

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

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

    val stream = env.addSource(consumer)

    val data = stream
        .map{ x =>
          val v = x.get("value")
          val key = v.get("category").asText
          val score = v.get("score").asDouble
          val cost = v.get("cost").asDouble
          println(v)
          (key, cost, score)
        }
        .keyBy(0)
//        .timeWindow(Time.of(2500, TimeUnit.MILLISECONDS), Time.of(500, TimeUnit.MILLISECONDS)) // Should be after Key
        .timeWindow(Time.of(2500, TimeUnit.MILLISECONDS))
        .minBy(0)
//      .sum(1)
//      .addSink(publisher)
//      .name("kafka")

    // execute and print result
    data.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")

    env.execute("Flink Scala Kafka Word Count Example")
  }
}
