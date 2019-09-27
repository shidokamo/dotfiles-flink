package org.example

import org.apache.flink.api.scala._
import org.apache.flink.api.java.utils.ParameterTool

object WordCountKafka {
  def main(args: Array[String]): Unit = {


    // set up the execution environment
    val env = ExecutionEnvironment.getExecutionEnvironment

    val params: ParameterTool = ParameterTool.fromArgs(args)
    // make parameters available in the web interface
    env.getConfig.setGlobalJobParameters(params)

    // Kafka connector properties
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", params.get("broker"))
    properties.setProperty("group.id", "test")

    // Kafka connector with schema to deserialize the data
    stream = env
            .addSource(new FlinkKafkaConsumer010[String]("test-topic", new SimpleStringSchema(), properties))
            .print()

    // Kafka Start position
    stream.setStartFromLatest()

    val counts = stream.flatMap { _.toLowerCase.split("\\W+") }
      .map { (_, 1) }
      .groupBy(0)
      .sum(1)

    // execute and print result
    counts.print()
    // counts.writeAsCsv(params.get("output"), "\n", " ")
    // execute program
    // env.execute("Flink Scala Word Count Example 2")
  }
}
