#!/usr/bin/env python3

import sys
import random
from time import sleep
from kafka import KafkaProducer


if len(sys.argv) > 2:
    print('Usage: ./words.py')
    print('or')
    print('Usage: ./words.py <author>')
    sys.exit(0)

BROKER = 'kafka-single-node:9092'
TOPIC = 'words'

#WORD_FILE = '/usr/share/dict/american-english'
WORD_FILE = './american-english'
WORDS = open(WORD_FILE, encoding='utf-8').read().splitlines()

AUTHOR = ['JHLeeeMe', 'first_user', 'second_user', 'third_user']

try:
    producer = KafkaProducer(bootstrap_servers=BROKER)
except Exception as e:
    print(f'ERROR --> {e}')
    sys.exit(0)

while True:
    try:
        if len(sys.argv) == 1:
            msg = f'{AUTHOR[random.randint(0, len(AUTHOR) - 1)]},'
        else:
            msg = f'{sys.argv[1]},'

        for _ in range(random.randint(2, 7)):
            msg += WORDS[random.randint(0, len(WORDS) - 1)] + ' '

        print(f">>> '{msg}'")
        producer.send(TOPIC, bytes(msg, encoding='utf8'))
        sleep(random.randint(1, 3))
    except UnicodeEncodeError as err:
        print(f'ERROR --> {err}')
        continue
