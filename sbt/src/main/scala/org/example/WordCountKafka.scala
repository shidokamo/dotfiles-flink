package org.example

import java.util.Properties
import org.apache.flink.api.scala._
import org.apache.flink.api.java.utils.ParameterTool
// import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010,FlinkKafkaProducer010}
// import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
// import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011
// import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
// import org.apache.flink.streaming.util.serialization.{JsonDeserializationSchema,SimpleStringSchema}
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

object WordCountKafka {
  def main(args: Array[String]): Unit = {

    // set up the execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment()

    val params: ParameterTool = ParameterTool.fromArgs(args)
    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    // Kafka connector properties
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("broker"))
    properties.setProperty("group.id", "org.apache.flink")

    // Kafka consumer withe schema to deserialize the data
    val consumer = new FlinkKafkaConsumer010[String]("test-topic", new SimpleStringSchema(), properties)

    // Kafka start position
    // consumer.setStartFromLatest()

    val stream = env.addSource(consumer).print()
    env.execute("Flink Scala Kafka Word Count Example")
    // stream.print()

    // val counts = stream.map { _.toLowerCase.split("\\W+") }
    //   .map { (_, 1) }
    //   .groupBy(0)
    //   .sum(1)

    // execute and print result
    // counts.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")
    // execute program
    // env.execute("Flink Scala Word Count Example 2")
  }
}
