import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.types._

//case class IOTData(device: String, temp: Double, humd: Double, pres: Double)
//case class WordsData(author: String, text: String, words:Int, length: Int)

object StreamHandler {
  def main(args: Array[String]): Unit = {
    //import java.util.Properties
    //
    //// properties for jdbc
    //val properties = new Properties()
    //properties.put("user", "postgres")
    //properties.put("password", "**************")

    val spark = SparkSession
      .builder
      .master("yarn")
      .appName("Stream Handler")
      .getOrCreate()

    import spark.implicits._

    val inputDF = spark.readStream
      .format("kafka")
      .options(Map(
        "kafka.bootstrap.servers" -> "kafka-single-node:9092",
        "subscribe" -> "iot,words")
      )
      .load()

    val rawDF = inputDF.selectExpr("topic", "CAST(value AS STRING)", "timestamp")

    val query = rawDF.writeStream
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .foreachBatch { (batchDF: DataFrame, _) =>
        // cache
        batchDF.persist()

        // Topic: iot
        batchDF.where($"topic" === "iot")
          .withColumn("_tmp", split($"value", ","))
          .select(
            $"_tmp".getItem(0).as("created_time"),  // timestamp in value column
            $"_tmp".getItem(1).as("device"),
            $"_tmp".getItem(2).as("temp"),
            $"_tmp".getItem(3).as("humd"),
            $"_tmp".getItem(4).as("pres")
          )
          .groupBy("device")
          .agg(avg("temp"), avg("humd"), avg("pres"), avg("created_time"))
          .withColumnRenamed("avg(temp)", "temp")
          .withColumnRenamed("avg(humd)", "humd")
          .withColumnRenamed("avg(pres)", "pres")
          .withColumnRenamed("avg(created_time)", "created_time")
          .withColumn("created_time", to_timestamp($"created_time"))
          .write
          .format("jdbc")
          .options(jdbcOptions(dbtable="iot_tb"))
          .mode("append")
          .save()

        // Topic: words
        batchDF.where($"topic" === "words")
          .withColumn("_tmp", split($"value", ","))
          .select(
            $"timestamp".alias("created_time"),  // timestamp in timestamp column
            $"_tmp".getItem(0).as("author"),
            $"_tmp".getItem(1).as("text")
          )
          .withColumn("number_of_words", size(split($"text", " ")) - 1)
          .withColumn("length", length($"text") - 1)
          .write
          .format("jdbc")
          .options(jdbcOptions(dbtable="words_tb"))
          .mode("append")
          .save()

        println("write to postgresql")

        // uncache
        batchDF.unpersist()
      }
      .outputMode("update")
      .start()

    query.awaitTermination()

    //val query = rawDS.writeStream
    //  .trigger(Trigger.ProcessingTime("5 seconds"))
    //  .foreachBatch { (batchDS: Dataset[(String, String)], _) =>
    //    // cache
    //    batchDS.persist()

    //    // Topic: iot
    //    batchDS.filter(_._1 == "iot")
    //      .map { _._2.split(",") }
    //      .map { x => 
    //        IOTData(x(1), x(2).toDouble, x(3).toDouble, x(4).toDouble)
    //      }
    //      .groupBy("device")
    //      .agg(avg("temp"), avg("humd"), avg("pres"))
    //      .withColumnRenamed("avg(temp)", "temp")
    //      .withColumnRenamed("avg(humd)", "humd")
    //      .withColumnRenamed("avg(pres)", "pres")
    //      .write
    //      .format("jdbc")
    //      .options(jdbcOptions(dbtable="iot_tb"))
    //      .mode("append")
    //      .save()

    //    // Topic: words
    //    batchDS.filter(_._1 == "words")
    //      .map { _._2.split(",") }
    //      .map { x => 
    //        WordsData(x(0), x(1), x(1).split(" ").length, x(1).length) 
    //      }
    //      .write
    //      .format("jdbc")
    //      .options(jdbcOptions(dbtable="words_tb"))
    //      .mode("append")
    //      .save()

    //    println("write to postgresql")

    //    // uncache
    //    batchDS.unpersist()
    //  }
    //  .outputMode("update")
    //  //.format("console")
    //  .start()

    //query.awaitTermination()
  }

  def jdbcOptions(url: String = "jdbc:postgresql://postgresql:5432/pipeline_db", 
    dbtable: String): Map[String, String] = {

      import scala.util.parsing.json._

      val source = JSON.parseFull(
        scala.io.Source.fromFile("src/resources/secrets/secrets.json").mkString
      )

      source match {
        case Some(e) => {
          val result = e.asInstanceOf[Map[String, String]]
          Map(
            "url" -> url,
            "dbtable" -> dbtable,
            "user" -> result.get("user").get,
            "password" -> result.get("password").get
          )
        }
        case _ => sys.exit(-1)
      }
  }
}
