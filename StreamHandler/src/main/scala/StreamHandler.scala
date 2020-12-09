import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.types._

//case class IOTData(device: String, temp: Double, humd: Double, pres: Double)
//case class WordsData(author: String, text: String, words: Int, length: Int)
//case class BikeData(
//  stationId: String, stationName: String, rackCnt: Int, parkingBikeCnt: Int, parkingRate: Int,
//  stationLat: Double, stationLng: Double, 
//  ts: java.sql.Timestamp
//)
case class BikeData(
  rackTotCnt: Int, stationName: String, parkingBikeTotCnt: Int, shared: Int,
  stationLatitude: Double, stationLongitude: Double,
  stationId: String
)

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
        "subscribe" -> "iot,words,bike")
      )
      .load()

    val rawDF = inputDF.selectExpr("topic", "CAST(value AS STRING)", "timestamp")

    val query = rawDF.writeStream
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .foreachBatch { (batchDF: DataFrame, _) =>
        // cache
        batchDF.persist()

        // Topic: bike
        //batchDF.where($"topic" === "bike")
        //  .select($"value")
        //  .select(from_json($"value", Encoders.product[BikeData].schema) as "value")
        //  .select("value.*")
        //  .write
        //  .format("console")
        //  .mode("append")
        //  .save()

        // Topic: bike
        val bikeDF = batchDF.where($"topic" === "bike")
        val parsedBikeDF = {
          val tmpDS: Dataset[String] = parseData(bikeDF)
          spark.read.json(tmpDF)
        }
        if (parsedBikeDF.columns.size != 0) {
          //parsedBikeDF.select($"rackTotCnt")
          parsedBikeDF.select("*")
            .withColumn("ts", current_timestamp())
            .write
            //.format("console")
            .format("jdbc")
            .options(jdbcOptions(dbtable="bike_tb"))
            .mode("append")
            .save()
        }
        
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
            "user" -> result.get("user").get
            //"user" -> result("user")
            //"password" -> result.get("password").get
          )
        }
        case _ => sys.exit(-1)
      }
  }

  def parseData(df: DataFrame): Dataset[String] = {
    val a = df.select($"value")

    a map {
      x => x.toString
    } map {
      y => y.slice(1, y.length - 1)
    }
  }
}
