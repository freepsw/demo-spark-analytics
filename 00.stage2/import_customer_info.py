import redis
import csv
import numpy as np
import random

# 사용자에게 임의의 나이를 부여한다. 
# return age : 10세 단위의 나이에서 특정 나이 (11, 14..)를 랜덤으로 추출
def get_age():
    # age10 : 10, 20..과 같은 나이대
    # 나이대 별로 가중치를 부여하여, 20가 가장 많이 선택되고, 
    # 그 다음으로 30, 40.. 으로 추출 되도록 설정
    age10 = random.choice([10, 20, 20, 20, 20, 30, 30, 30, 40, 40, 50, 60])
    start   = age10
    end     = age10 + 10
    # 선택된 나이대의 1자리 숫자도 랜덤으로 부여
    age = random.choice(np.arange(start, end))
    return age

# 1. redis server에 접속한다. 
r_server = redis.Redis('localhost') 

# 2. read customer info from file(csv)
with open('./cust.csv', 'rb') as csvfile:
    # csv 파일에서 고객정보를 읽어온다.
    reader = csv.DictReader(csvfile, delimiter = ',')
    next(reader, None)
    i = 1 
    # save to redis as hashmap type
    # hash map 자료구조가 1개의 key에 다양한 사용자 정보를 저장/조회 할 수 있다.
    for row in reader:
    	# 고객 id를 Key로 지정하고, name, gender, age, address..등 다양한 정보를 저장
        # db table로 비교하면, 1개의 레코드를 생성(pk는 custid, field는 name, gender....)
        r_server.hmset(row['CustID'], {'name': row['Name'], 'gender': int(row['Gender'])})
        r_server.hmset(row['CustID'], {'age': int(get_age())})
        r_server.hmset(row['CustID'], {'zip': row['zip'], 'Address': row['Address']})
        r_server.hmset(row['CustID'], {'SignDate': row['SignDate'], 'Status': row['Status']})
        r_server.hmset(row['CustID'], {'Level': row['Level'], 'Campaign': row['Campaign']})
        r_server.hmset(row['CustID'], {'LinkedWithApps': row['LinkedWithApps']})

        # 진행상황을 확인하기 위한 프린트...
        if(i % 100 == 0):
        	print('insert %dth customer info.' % i)
       	i += 1

    print('importing Completed (%d)' % i)
