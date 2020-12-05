#!/usr/bin/env python3


import json
import requests


API_KEY = "<my-key>"

res = requests.get('http://openapi.seoul.go.kr:8088/{}/json/bikeList'.format(API_KEY) + \
                   '/1/1000/')

if not res.ok:
    sys.exit(res)

res_json = json.loads(res.text)
rent_bike_status = res_json['rentBikeStatus']
result = rent_bike_status['RESULT']

if result['CODE'] == 'INFO-000':
    rows: list[dict] = rent_bike_status['row']
