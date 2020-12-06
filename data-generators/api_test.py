#!/usr/bin/env python3

import sys
import json
from urllib import request
from typing import List, Dict


with open('secrets/api_keys.json', 'r') as key_file:
    API_KEY: str = json.load(key_file)['key']

from_to = [1, 1000]
while True:
    if from_to[1] > 2000 :
        from_to = [1, 1000]

    data = get_source(API_KEY, from_to[0], from_to[1])

    data_json: Dict = json.loads(data)
    bike_status: Dict = data_json['rentBikeStatus']
    result: Dict = bike_status['RESULT']

    if result['CODE'] == 'INFO-000':
        rows: List[Dict] = bike_status['row']

    from_to[0] += 1
    from_to[1] *= 2


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
