# 미완 미완미완 미완미원 미완미완 미완미완 미완미완 미완미완 미완미완 미완


# fake-data-pipeline
Data Generators -> Kafka -> Spark Streaming -> PostgreSQL -> Grafana

### 1. fake data -> Kafka
- 랜덤 생성된 데이터들을 카프카 토픽으로 보냄 (Topics: iot, words)
- [data-generators/iot_devices.py](https://github.com/JHLeeeMe/fake-data-pipeline/blob/master/data-generators/iot_devices.py)
- [data-generators/words.py](https://github.com/JHLeeeMe/fake-data-pipeline/blob/master/data-generators/words.py)

### 2. Spark Structured Streaming
- iot, words 토픽을 구독하고 처리함
- 5초에 한번씩 토픽별로 필터링 후 집계 및 timestamp관련 작업(for 실시간 시각화)을 한 후 PostgreSQL의 pipeline_db로 쓰기작업.
- [StreamHandler.scala](https://github.com/JHLeeeMe/fake-data-pipeline/blob/master/StreamHandler/src/main/scala/StreamHandler.scala)

### 3. Grafana
- PostgreSQL에 담겨지는 데이터들을 시각화

**IOT Dashboard**
![IOT_Dashboard](https://user-images.githubusercontent.com/31606119/98818999-ab5e8400-246f-11eb-8b6c-cc4a220758bd.png)

**Words Dashboard**
![Words_Dashboard](https://user-images.githubusercontent.com/31606119/98819004-ac8fb100-246f-11eb-9f1e-99548f1cd30f.png)

---

# 한번 해보기
Docker & docker-compose로 작성했으므로 간단히 실행해보자.  
Dockerfile 링크: [Docker Images repo](https://github.com/JHLeeeMe/docker-images)
### 1. git clone
```bash
git clone https://github.com/JHLeeeMe/fake-data-pipeline.git
```
### 2. Run docker-compose
```bash
cd fake-data-pipeline && docker-compose up -d

# Check
docker-compose ps
```

여기까지하면 아래와 같은 컨테이너가 실행된다.  

**Spark**: master(10.0.0.10), slave-1, slave-2, slave-3 (10.0.0.11 ~ 10.0.0.13)  
**Kafka**: kafka-single-node(10.0.0.30)  
**PostgreSQL**: postgresql(10.0.0.40)  
**Grafana**: grafana(10.0.0.50)

### 3. Run data-generators
master container에서 python script들을 실행하자.
```bash
docker exec -it master /bin/bash

# master container
master$ cd ~/fake-data-pipeline
master$ pip3 install -r requirements.txt
master$ python3 data-generators/iot_devices.py <home || seoul>
```
작성한 스크립트가 데몬실행에 적절하지 않음.  
그러므로 위 3번 과정을 3번(iot_devices.py <home && seoul>, words.py) 실행시켜주어야함.  
아니면 스크립트 수정

### 4. Run spark-submit
3번 과정에 의해 fake 데이터들이 iot, words 토픽으로 보내지는 중  
spark-submit을 실행시켜 데이터를 정제 후 PostgreSQL로 저장.
```bash
docker exec -it master /bin/bash

# master container
master$ cd /root/fake-data-pipeline/StreamHandler
master$ ./run-spark-submit.sh
```

### 5. Grafana로 시각화
웹 브라우저를 켜고 ```localhost:3000```로 접속  
초기 id: admin, password: admin  
data source를 postgresql로 만들고 시각화  

위 스크린샷에서 보여지는 대쉬보드는 [grafana/dashboards](https://github.com/JHLeeeMe/fake-data-pipeline/tree/master/grafana/dashboards/)에 올려둠
import해서 쓰면 될것이다. (이것까지 자동화해서 작성할 수 있어보이던데 귀찮아서 더 안찾아봄...)

