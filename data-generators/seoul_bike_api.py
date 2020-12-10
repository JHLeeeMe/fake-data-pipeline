#!/usr/bin/env python3

import sys
import json
from time import sleep
from urllib import request
from typing import List, Dict

from kafka import KafkaProducer


def get_source(API_KEY: str, _from: int = 1, _to: int = 1000) -> str:
    """Get response

    Keyword arguments:
        API_KEY: str -- api key
        _from: int = 1 -- start index
        _to: int = 1000 -- end index
    Return:
        res.read().decode('utf8'): str
    """
    res = request.urlopen(
            f'http://openapi.seoul.go.kr:8088/{API_KEY}/json/bikeList/{_from}/{_to}/')

    if res.status != 200:
        sys.exit(res)

    return res.read().decode('utf8')


BROKER = 'kafka-single-node:9092'
TOPIC = 'bike'

API_KEY_PATH = 'secrets/api_keys.json'

with open(API_KEY_PATH, 'r') as key_file:
    API_KEY: str = json.load(key_file)['key']

try:
    producer = KafkaProducer(
            bootstrap_servers=BROKER,
            value_serializer=lambda x: json.dumps(x).encode('utf-8')
        )
except Exception as e:
    sys.exit(e)

from_to = [1, 1000]
msg = []
while True:
    if from_to[1] > 2000 :
        producer.send(TOPIC, msg)
        print("sending...")
        from_to = [1, 1000]
        msg = []

    data = get_source(API_KEY, from_to[0], from_to[1])

    data_json: Dict = json.loads(data)
    bike_status: Dict = data_json['rentBikeStatus']
    result: Dict = bike_status['RESULT']

    if result['CODE'] == 'INFO-000':
        rows: List[Dict] = bike_status['row']
    msg += rows

    from_to[0] = from_to[1] + 1
    from_to[1] += 1000

    sleep(2)


