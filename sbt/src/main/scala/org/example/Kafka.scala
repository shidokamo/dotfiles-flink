package org.example

import java.util.Properties
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.flink.api.scala._
import org.apache.flink.api.java.utils.ParameterTool
// import org.apache.flink.streaming.util.serialization.JSONDeserializationSchema // 廃止されたっぽい
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema
// import org.apache.flink.formats.json.JsonNodeDeserializationSchema
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010
import org.apache.flink.streaming.api.scala.DataStream
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object Kafka {
  def main(args: Array[String]): Unit = {

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val params: ParameterTool = ParameterTool.fromArgs(args)
    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    // Kafka connector properties
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("broker"))
    properties.setProperty("group.id", "org.apache.flink")

    // Kafka consumer withe schema to deserialize the data
    val consumer = new FlinkKafkaConsumer010[ObjectNode](params.get("topic"), new JSONKeyValueDeserializationSchema(true), properties)

    // Kafka start position
    consumer.setStartFromLatest()

    val stream = env.addSource(consumer)

    val data = stream
      .map{ v =>
        val key = v.get("category")
        val score = v.get("score")
        (key, score, cost)
      }
      .keyBy(0)
      .timeWindow(Time.of(2500, MILLISECONDS), Time.of(500, MILLISECONDS))
      .min(1)
//      .groupBy(0)
//      .sum(1)

    // execute and print result
    data.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")

    env.execute("Flink Scala Kafka Word Count Example")
  }
}
