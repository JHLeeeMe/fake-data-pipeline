#!/usr/bin/env bash

api_key="<my-api>"

while true; do
    curl -XGET "http://openapi.seoul.go.kr:8088/"$api_key"/json/bikeList/1/1000/" >/dev/null
    curl -XGET "http://openapi.seoul.go.kr:8088/"$api_key"/json/bikeList/1001/2000/" >/dev/null

    sleep 0.2
done
