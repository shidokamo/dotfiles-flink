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

    val stream = env.addSource(consumer)
    // stream.print()

    val lower = stream.iterate( iteration => {
          val lower = iteration.map(v => v.toLowerCase)
          (lower)
    })
//      .map((_,1))
//      .groupBy(0)
//      .keyBy(0)
//      .sum(1)
//      .map { x => (x[0]) }
//      .filter(_.noEmpty)
//      .map { (_, 1) }
//      .groupBy(0)
//      .keyBy(0)
//      .sum(1)

    // execute and print result
    lower.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")

    env.execute("Flink Scala Kafka Word Count Example")
  }
}
