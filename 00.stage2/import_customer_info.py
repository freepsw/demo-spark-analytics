import redis
import csv

r_server = redis.Redis('localhost') #this line creates a new Redis object and
                                    #connects to our redis server


# read customer info file(csv)
with open('./cust.csv', 'rb') as csvfile:
    reader = csv.DictReader(csvfile, delimiter = ',')
    next(reader, None)
    i = 1 
    # save to redis as hashmap type
    for row in reader:
        r_server.hmset(row['CustID'], {'name': row['Name'], 'gender': int(row['Gender'])})
        r_server.hmset(row['CustID'], {'zip': row['zip'], 'Address': row['Address']})
        r_server.hmset(row['CustID'], {'SignDate': row['SignDate'], 'Status': row['Status']})
        r_server.hmset(row['CustID'], {'Level': row['Level'], 'Campaign': row['Campaign']})
        r_server.hmset(row['CustID'], {'LinkedWithApps': row['LinkedWithApps']})
        if(i % 100 == 0):
        	print('insert %dth customer info \n' % i)
       	i += 1

    print('importing Completed (%d)' % i)
