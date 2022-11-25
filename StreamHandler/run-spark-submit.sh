#!/usr/bin/env bash


sbt package;

spark-submit \
  --class StreamHandler \
  --master yarn \
  --packages "org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.0" \
  --driver-class-path driver/postgresql-42.2.5.jar \
  --jars driver/postgresql-42.2.5.jar \
  target/scala-2.11/stream-handler_2.11-1.0.jar
