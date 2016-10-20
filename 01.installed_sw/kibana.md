# kibana 4.6.1 설치 및 실행

## Basic concept (Getting Started)
https://www.elastic.co/guide/en/kibana/current/getting-started.html


## [visualize] menu guide
 * pie chart 선택 (전체 날짜 선택)
  - spilit chart -> missing value가 있는 경우 그래프에서 삭제 (-모양의 돗보기 사용), top n개만 보여지도록 설정
  - split chart (하나의 항목별로 다른 항목의 비중 조회), 예를 들면 서울지역에서 남/여 비율
  - 2개의 split의 순서를 조정(increase priority, 화살표 버튼)
  - save current visualization as visualization component (this can be used for dashboard later)

 * line chart (시간별 추세 확인 용도)
  - 초기에 데이터를 split하지 않으면 x축이 all, y축이 count로 전체 데이터의 건수가 하나의 포인트로 표시된다. 
  - X-Axis > split > date histogram -> x축의 date column 선택 -> interval은 auto로 조회
  - 위에서는 단순히 record 건수만 표시되므로, 이를 특정 field의 값을 표시하도록 하자 (이때, time interval이 1 day인 경우 1일 동안의 값을 어떻게 보여주지 선택)
  - Y-Axis의 aggregation에서 field명과 선택된 field에 대한 sum, average등을 선택(string type의 field는 선택하지 못함)
  - X-Axis > split line -> Sub aggregation(Terms) -> 특정 field를 선택하면 해당 field의 상위 n개에 대하여 line이 추가된다. (priority를 높여서 그래서 재생 -> missing value 제거 )
  - 여기서의 line은 average score만 y축으로 보여주므로, 해당 기간(x축 한 눈금)에 얼마나 많은 정보(field의 값 중에서 unique한 값이 count, 예를 들면 해당 기간동한 방문한 unique user id의 갯수를 표현)가 있는지 표현하고 싶을 수 있다. 
  - 이때  Y-Axis에서 dot-size를 이용하여 line의 node별로 dot의 크기를 다르게 설정함.
  - Data/Option메뉴에서 Option을 보면 그래프를 어떻게 보여줄 지에 대한 상세 설정이 있음. (hide line등)

* map

* metrics
 - 처음 metrics를 클릭하면 전체 record의 count가 출력된다.
 - fields에서 count하고자 하는 field를 선택하고, aggregation에서 count/unique count/sum/average 등의 집계방식을 선택할 수 있다. 
 - metric 추가도 위와 같은 방식으로 가능함.