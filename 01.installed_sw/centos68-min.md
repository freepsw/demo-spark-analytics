# install Cent OS 6.8 min

# set network
```
> vi /etc/sysconfig/network-scripts/ifcfg-eth0
ONBOOT=yes # 변경
> service network restart
```

# install ssh server
```
> yum install openssh-server openssh-clients openssh-askpass
> vi /etc/ssh/sshd_config
PermitRootLogin no #루트접속을 제한
> service sshd restart
```

# create user
```
> adduser rts
> passwd rts
# 비밀번호 설정

> visudo -f /etc/sudoers
root ALL=(ALL) ALL
rts ALL=(ALL) ALL # 추가
```

# install java 8
- http://tecadmin.net/install-java-8-on-centos-rhel-and-fedora/#
```
> sudo rts
> yum install wget
> mkdir apps
> cd apps
> wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u102-b14/jdk-8u102-linux-x64.rpm
> sudo yum localinstall jdk-8u102-linux-x64.rpm
> 
```

# install maven 
- http://xxun.tistory.com/233
```
> sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
> sudo yum install apache-maven
```

# install python 2.7
- http://slowcode.tistory.com/16
```
> cd ~/apps
#추가로 파이썬에 필요한 개발도구 설치 명령어이다.
> sudo yum groupinstall "Development tools"
> sudo yum install zlib-devel bzip2-devel openssl-devel ncurses-devel sqlite-devel readline-devel tk-devel
> ..링크된 웹페이 따라서 해보기..
```

- 필요한 python util설치
- http://grompany.blogspot.kr/2013/11/python-266-centos-62-python-273.html
```
> wget https://pypi.python.org/packages/source/d/distribute/distribute-0.6.49.tar.gz --no-check-certificate
> tar xf distribute-0.6.49.tar.gz
> cd distribute-0.6.49
> sudo python setup.py install
> sudo easy_install pip
```

# time 동기화
- time 서버가 다른 지역의 서버로 설정되어 있을 경우,
- kibana의 ui(client pc)의 시간과 달라 데이터가 보아지 않을 수 있다.
- http://webdir.tistory.com/120
```
> sudo yum install ntp
....
```

