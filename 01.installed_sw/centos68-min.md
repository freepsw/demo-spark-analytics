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

# install python 2.7
- http://slowcode.tistory.com/16
```
#추가로 파이썬에 필요한 개발도구 설치 명령어이다.
> yum groupinstall "Development tools"
> yum install zlib-devel bzip2-devel openssl-devel ncurses-devel sqlite-devel readline-devel tk-devel
```

# time 동기화
- time 서버가 다른 지역의 서버로 설정되어 있을 경우,
- kibana의 ui(client pc)의 시간과 달라 데이터가 보아지 않을 수 있다.
- http://webdir.tistory.com/120
```
> sudo yum install ntp
....
```