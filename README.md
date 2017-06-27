1.概述

Esper是什么，为何我们采用Esper作为风控实时规则的解决方案之一？这个问题在大家运行我们的第一个例子之后，应该会有所体会。
简而言之，一句话说明：Esper是一个有时间窗口管理能力的单节点（可以通过集群变成分布式）内存数据库。其特点就是两个字：快易。响应速度快，上手和使用容易。
2.第一个例子

风控的各种规则计算，其性能瓶颈在获取计算所需的规则数据上面。常见的如限额限次的数据：1天内指定商户号的交易累计金额或次数。这种常见的做法就是通过数据库查询，sql 如 select count( * ) as countTarget,sum(tradeAmount) as sumTarget from T_tradeEvent where custId = ‘xxx’ and requestDate=’当前日期’ group by custId;
那同样的功能在Esper中是如何实现的呢？
让我们进入第一个例子来理解esper的使用方式，其中esper的版本是5.0.0。
【demo场景】不断有新的交易事件到来，需要实时获取每个交易事件在到达时的当天已经存在的交易（含当前这笔）的累计数量和金额（根据查询条件过滤）。同时系统会输出一些性能统计的结果，主要是发送事件数和响应时间。
在这个例子中，发送交易和处理交易是”客户端-服务端”模式，客户端通过socket发送event，服务端监听socket端口，获取event并处理，得到即使的统计结果，输出到屏幕上。
【运行例子】
使用Eclipse导入此maven工程，build之后，用run as -> javaApplication的方式执行，先执行 com.espertech.esper.example.limit.server.Server.java，再执行com.espertech.esper.example.limit.client.Client.java

系统启动，开始处理event,并把结果输出到屏幕终端。
输出的内容示意
Statements registered # 1 only
Using direct handoff, cpu#4
Server accepting connections on port 6789.
Client connected to server.
//这里打印发送事件触发的esper的subscriber统计结果
count : [1]; sum : [4.7138489E7]; eventRelNo : [f0142b61-7baa-4945-b915-d3695ce0c6d2]
count : [1]; sum : [2.2640715E7]; eventRelNo : [ca730551-0feb-4d3d-a00b-c21ced7ae43f]
count : [1]; sum : [3.2245814E7]; eventRelNo : [29b51fcb-c6fa-42c8-a9cb-473158a9c05f]
count : [1]; sum : [3.5041704E7]; eventRelNo : [19ac227d-2a24-4f77-9ae4-c4602087e337]
---Stats - engine (unit: ns)
Avg: 2720349 #4 //这里统计平均发送的event数量的分布情况
0 < 5000: 0.00% 0.00% #0
5000 < 10000: 0.00% 0.00% #0
10000 < 15000: 0.00% 0.00% #0
15000 < 20000: 0.00% 0.00% #0
20000 < 25000: 0.00% 0.00% #0
25000 < 50000: 0.00% 0.00% #0
50000 < 100000: 0.00% 0.00% #0
100000 < 500000: 0.00% 0.00% #0
500000 < 1000000: 75.00% 75.00% #3
1000000 < 2500000: 0.00% 75.00% #0
2500000 < 5000000: 0.00% 75.00% #0
5000000 < more: 25.00% 100.00% #1
---Stats - endToEnd (unit: ms) //这里统计平均发送的event影响时间的分布情况
Avg: 3 #4
0 < 1: 0.00% 0.00% #0
1 < 5: 50.00% 50.00% #2
5 < 10: 25.00% 75.00% #1
10 < 50: 25.00% 100.00% #1
50 < 100: 0.00% 100.00% #0
100 < 250: 0.00% 100.00% #0
250 < 500: 0.00% 100.00% #0
500 < 1000: 0.00% 100.00% #0
1000 < more: 0.00% 100.00% #0
Throughput 0 (active 0 pending 0 cnx 1)
到这里，这个例子就跑完了。
3.例子中的功能，Esper是如何实现的

我们类比数据库的实现，说一下esper的实现。
首先数据库要实现查询功能，需要先到表空间中建表，
【与之对应】esper需要定义事件，目前esper的事件支持几种类型， java.util.Map, Object[] 和 javaBean(生成get和set方法的java数值对象),其中最灵活的一种就是javaBean。
【代码位置】
com.espertech.esper.example.limit.TradeEvent (事件定义)
com.espertech.esper.example.limit.server.CEPProvider (esper引擎核心)其中这行是增加事件定义到esper: configuration.addEventType("TradeEvent", TradeEvent.class);
2.“数据表”有了，接着就是编写SQL，对数据表的记录进行查询统计。
【与之对应】esper使用EPL语句，在其引擎中查询已经放进来的event，根据EPL中设定的不同查询条件过滤得到最终的查询统计结果。
【代码位置】
/esper_examples_limit/src/main/resources/statements.properties 其中的
#增加限额限次的例子
LIMITDEMO = select count( * ) as countTarget,sum(pw.tradeAmount) as sumTarget,* from TradeEvent.win:time(120 seconds) pw where pw.requestDate='#requestDate' group by pw.custId
里面的'#requestDate'在注册到esper前会被替换成今天的日期。
com.espertech.esper.example.limit.server.Server中的public synchronized void start() 方法: cepProvider.registerStatement(stmtString.replaceAll("[#]requestDate", CURRENT_DATE), mode);
3.通过jdbc把查询结果取回，并交由程序做后续处理。
【与之对应】esper有几种方式可以进行查询并得到结果，在这个例子中，我们使用了subscriber方式，这个方式下，我们需要定义一个subscriber(其实就是一个含有update关键字方法的任意名称的java对象)，在其update方法中对统计结果进行后续处理。 Subscriber方式是异步执行的，在每个event被sendEvent到esper后，都会触发已经注册过的subscriber，传递EPL的统计结果到update方法。
其中update方法的参数跟EPL的查询字段数是一一对应的。例子中EPL输出了3个字段，countTarget、sumTarget和*，其中*代表了当前event本身的对象。
【代码位置】
com.espertech.esper.example.limit.server.TradeEventSubscriber (subscriber件定义)
在注册EPL的时候绑定subscriber:
com.espertech.esper.example.limit.server. CEPProvider 的
public void registerStatement(String statement, String statementID)方法的这行进行绑定
stmt.setSubscriber(subscriber);
