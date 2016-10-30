import redis
import csv
import numpy as np
import random

def get_age(age10):
    start   = age10
    end     = age10 + 10
    age = random.choice(np.arange(start, end))
    return age


r_server = redis.Redis('localhost') #this line creates a new Redis object and
                                    #connects to our redis server
# read customer info from file(csv)
with open('./cust.csv', 'rb') as csvfile:
    reader = csv.DictReader(csvfile, delimiter = ',')
    next(reader, None)
    i = 1 
    # save to redis as hashmap type
    for row in reader:
    	age10 = random.choice([10, 20, 20, 20, 20, 30, 30, 30, 40, 40, 50, 60])
        r_server.hmset(row['CustID'], {'name': row['Name'], 'gender': int(row['Gender'])})
        r_server.hmset(row['CustID'], {'age': int(get_age(age10))})
        r_server.hmset(row['CustID'], {'zip': row['zip'], 'Address': row['Address']})
        r_server.hmset(row['CustID'], {'SignDate': row['SignDate'], 'Status': row['Status']})
        r_server.hmset(row['CustID'], {'Level': row['Level'], 'Campaign': row['Campaign']})
        r_server.hmset(row['CustID'], {'LinkedWithApps': row['LinkedWithApps']})
        if(i % 100 == 0):
        	print('insert %dth customer info.' % i)
       	i += 1

    print('importing Completed (%d)' % i)



    
# for i in range(1, 10):   
#     age10 = random.choice([10, 20, 20, 20, 30, 40])
#     get_age(age10)