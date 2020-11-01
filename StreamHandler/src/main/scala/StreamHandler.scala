import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.types._

//import java.util.Properties

case class IOTData(device: String, temp: Double, humd: Double, pres: Double)
case class WordsData(author: String, text: String, words:Int, length: Int)

object StreamHandler {
  def main(args: Array[String]): Unit = {
    //// properties for jdbc
    //val properties = new Properties()
    //properties.put("user", "postgres")
    //properties.put("password", "**************")

    val spark = SparkSession
      .builder
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

    val rawDS = inputDF
      .selectExpr("topic", "CAST(value AS STRING)").as[(String,String)]

    val query = rawDS.writeStream
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .foreachBatch { (batchDS: Dataset[(String, String)], _) =>
        // cache
        batchDS.persist()

        // Topic: iot
        batchDS.filter(_._1 == "iot")
          .map { _._2.split(",") }
          .map { x => 
            IOTData(x(1), x(2).toDouble, x(3).toDouble, x(4).toDouble)
          }
          .groupBy("device")
          .agg(avg("temp"), avg("humd"), avg("pres"))
          .withColumnRenamed("avg(temp)", "temp")
          .withColumnRenamed("avg(humd)", "humd")
          .withColumnRenamed("avg(pres)", "pres")
          .write
          .format("jdbc")
          .options(jdbcOptions(dbtable="iot_tb"))
          .mode("append")
          .save()

        // Topic: words
        batchDS.filter(_._1 == "words")
          .map { _._2.split(",") }
          .map { x => 
            WordsData(x(0), x(1), x(1).split(" ").length, x(1).length) 
          }
          .write
          .format("jdbc")
          .options(jdbcOptions(dbtable="words_tb"))
          .mode("append")
          .save()

        println("write to postgresql")

        // uncache
        batchDS.unpersist()
      }
      .outputMode("update")
      .start()

    query.awaitTermination()
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
