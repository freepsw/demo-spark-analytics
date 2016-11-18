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
- java download site : http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
```
> sudo rts
> yum install wget
> mkdir apps
> cd apps
> wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u102-b14/jdk-8u102-linux-x64.rpm
# 8.11 버전 url
http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-linux-x64.rpm

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

# install ruby
- centos6
```
> curl -L get.rvm.io | bash -s stable
> source /home/rts/.rvm/scripts/rvm
> rvm install 1.9.3
> rvm use 1.9.3 --default
> ruby -version
```

- centos7 
```
# Step 1: Install Required Packages
> sudo yum install gcc-c++ patch readline readline-devel zlib zlib-devel
> sudo yum install libyaml-devel libffi-devel openssl-devel make
> sudo yum install bzip2 autoconf automake libtool bison iconv-devel sqlite-devel

# Step 2: Install RVM
> curl -sSL https://rvm.io/mpapis.asc | gpg --import -
> curl -L get.rvm.io | bash -s stable
-> 위 명령어를 수행하면 아래와 같은 메세지가 보인다
* WARNING: You have '~/.profile' file, 
    you might want to load it,
    to do that add the following line to '/home/rts/.bash_profile':
      source ~/.profile

# 위 메세지 대로 "source ~/.profile" 이 문구를 ~/.bash_profile에 추가한다.
# 그리고 직접 실행도 한번 해 준다. 
> source ~/.profile 
> rvm reload
RVM reloaded!

# Step 3: Verify Dependencies
> rvm requirements run
Checking requirements for centos.
Requirements installation successful

# Step 4: Install Ruby 2.2
> rvm install 2.2.4
> ruby --version
```

# time 동기화
- time 서버가 다른 지역의 서버로 설정되어 있을 경우,
- kibana의 ui(client pc)의 시간과 달라 데이터가 보아지 않을 수 있다.
- http://webdir.tistory.com/120
```
> sudo yum install ntp
....
```
# timezone 변경
- 만약 timezone이 중국(CST)과 같은 지역으로 표시된다면, 원하는 지역으로 변경해야함.
```
# 기존 timezone 백업
> date
2016. 11. 16. (수) 20:28:56 CST
> sudo mv /etc/localtime /etc/localtime_org
# 서울로 timezone 변경
> sudo ln -s /usr/share/zoneinfo/Asia/Seoul /etc/localtime
> date
2016. 11. 17. (목) 11:30:43 KST
```

# Issue list
## VMWare 이미지를 다른 PC로 이동한 후 network이 정상적으로 연결되지 않는 오류 발생
- 현상
 * ping www.google.com 접속이 되지 않음.
 * service network restart 실행시 아래 오류
 * Bringing up interface eth0:  Device eth0 does not seem to be present, delaying initialization.

- 해결
 * http://netmaid.tistory.com/94 참고

```
>  vi /etc/udev/rules.d/70-persistent-net.rules
```
 * 아래 내용에서 NAME="eth0" line을 삭제하고
 * NAME="eth1"  --> NAME="eth0"로 변경
```
# PCI device 0x8086:0x100f (e1000)
SUBSYSTEM=="net", ACTION=="add", DRIVERS=="?*", ATTR{address}=="00:0c:29:2c:ff:a7", ATTR{type}=="1", KERNEL=="eth*", NAME="eth0"  --> 삭제

# PCI device 0x8086:0x100f (e1000)
SUBSYSTEM=="net", ACTION=="add", DRIVERS=="?*", ATTR{address}=="00:0c:29:da:06:f2", ATTR{type}=="1", KERNEL=="eth*", NAME="eth1"  --> eth0로 변경
```

 * /etc/sysconfig/network-scripts/ifcfg-eth0 파일 수정
  - MAC 주소를 위의 변경한 eth0의 ATTR{address}로 변경한다.
 * 시스템 reboot 하면 정상적으로 네트워크 접속됨.
