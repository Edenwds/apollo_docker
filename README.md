# Apollo & Docker 
## 简介
本文档依据Apollo官方的分布式部署文档，介绍如何实现根据源码定制安装包并实现docker容器化分布式部署。
# 目的
- 修改默认端口，日志输出地址、等级，jvm参数等参数默认值
- 使用独立的注册中心，禁止configservice作为注册中心
- 使用容器化部署的Mysql数据库和容器化部署的Eureka集群
- 容器化部署Apollo并使用docker compose实现容器编排
## 要点
- 根据文档，对脚本和项目配置文件进行修改
- docker-compose.yml文件编写
- 容器间的通信

## 步骤
1. 根据需求从 [GitHub Release](https://github.com/ctripcorp/apollo/releases)页面下载所需版本的source code包或者直接clone [源码](https://github.com/ctripcorp/apollo)。
  
2. 修改项目默认端口，日志输出路径，等级。这里主要是针对apollo-configservice apollo-adminservice apollo-portal三个项目进行修改。

> 修改端口：分别修改三个项目scripts/startup.sh的SERVER_PORT。这里我分别修改为9003,9004,9005。

> 修改日志配置：  
    调整日志输出路径分别修改三个项目scripts/startup.sh和apollo-{project}.conf中的LOG_DIR。  
    调整日志等级分别修改三个项目的logback.xml配置文件。  
    
3. 禁止apollo-configservice作为注册中心，编辑bootstrap.yml文件，添加以下配置：
  
```
apollo:
  eureka:
    server:
      enabled: false
```
4. 调整网络策略，忽略Apollo客户端和Portal无法访问的网卡，分别编辑apollo-configservice和apollo-adminservice的application.yml文件，添加以下配置：

```
spring:
 cloud:
    inetutils:
      ignoredInterfaces:
      - docker0
      - veth.*
```
5. 修改数据库中注册中心的地址，直接修改ApolloConfigDB中ServerConfig表的eureka.service.url字段的值。这里因为使用的同时容器化部署的Eureka集群，所以**地址为container_name:port，多个地址用逗号分隔**。

```
http://eureka-server1:3030/eureka/,http://eureka-server2:3031/eureka/
```
  
6. 配置数据库连接信息，直接编辑项目中的 scripts/build.sh 文件，配置ApolloConfigDB和ApolloPortalDB相关的连接信息。由于我的数据库也是docker部署的，这里的IP为容器名称。
```
# apollo config db info
apollo_config_db_url=jdbc:mysql://mysql5.7:3306/ApolloConfigDB?characterEncoding=utf8
apollo_config_db_username=root
apollo_config_db_password=

# apollo portal db info
apollo_portal_db_url=jdbc:mysql://mysql5.7:3306/ApolloPortalDB?characterEncoding=utf8
apollo_portal_db_username=root
apollo_portal_db_password=
```
7. 配置各环境的meta service地址，**由于meta service和config-service是在同一个jvm部署的，所以这个地址就是apollo-configservice的地址 container_name:port**。同样直接编辑scripts/build.sh文件。

```
dev_meta=http://apollo-configservice:9003
```
8. 根据需求修改配置后，进行编译打包。执行./build.sh脚本，该脚本会依次打包apollo-configservice, apollo-adminservice, apollo-portal。
> 由于不同环境所用的数据库信息不同，所以针对不同环境apollo-configservice和apollo-adminservice需要使用不同的数据信息重新打包，apollo-protal只需要打包一次。

9. 分别在三个项目的target文件夹下获取安装包，名称分别为apollo-configservice-x.x.x-github.zip, apollo-adminservice-x.x.x-githup.zip, apollo-portal-x.x.x-github.zip
  
10. 构建镜像，Apollo官方已经提供了Dockerfile文件。直接将安装包和Dockerfile上传到服务器，使用命令 docker build -t image_name . 构建镜像
> 注意   
    Docfile中的变量VERSION应该和安装包的X.X.X值一致  
    当修改了监听端口时，SERVER_PORT值应该与使用值一致
11. 编写docker-compose文件。注意，这里连接mysql，eureka,以及portal需要从meta service获取服务地址信息都涉及到了容器间的通信，由于**这些容器都部署在同一个docker daemon进程下，所以使用bridge networks 进行容器间的通信**。
> 这里使用了三个bridge network, 分别为configservice, mysql, eureka_net  
  eureka_net 为eureka容器化部署集群时创建，其他两个可以提前创建，也可以在启动容器时创建。  
  注意： 提前创建后，在docker-compose文件中在networks需要将external设置为true  
  提前创建后，使用命令将正在运行的容器连接到network。如： dockcer network connect [network_name] [container_name]

```
version: '3'
services:
  apolloconfigservice:
    image: apollo-configservice
    container_name: apollo-configservice
    networks:
      - mysql
      - eureka_eureka-net
      - configservice
    volumes:
      - /opt/docker/apollo/logs/config:/opt/logs/configservice
    ports:
      - 9003:9003
  
  apolloadminservice:
    image: apollo-adminservice
    container_name: apollo-adminservice
    depends_on: 
      - apolloconfigservice
    networks:
      - mysql
      - eureka_eureka-net
    volumes:
      - /opt/docker/apollo/logs/admin:/opt/logs/adminservice  
    ports:
      - 9004:9004

  apolloportalservice:
    image: apollo-portal
    container_name: apollo-portal
    depends_on: 
      - apolloconfigservice
      - apolloadminservice
    networks:
      - mysql
      - configservice
    volumes:
      - /opt/docker/apollo/logs/portal:/opt/logs/apolloportal   
    ports:
      - 9005:9005

networks:
  eureka_eureka-net:
    external: true
  mysql:
    external: true
  configservice:
    external: true  

```
12. 将docker-compose.yml上传到服务器，使用命令docker-compose up -d 启动服务。
> 在docker-compose.yml文件中使用了depends_on来确定容器的启动顺序，确保apollo-configservice和apollo-adminservcie服务在apollo-portal服务前启动。  
  在实际操作中，我将apollo-configservice和apollo-adminservice服务使用一个docker-compose.yml文件编排，apollo-portal单独使用一个。

更多详细信息请看 [wiki](https://github.com/ctripcorp/apollo/wiki/%E5%88%86%E5%B8%83%E5%BC%8F%E9%83%A8%E7%BD%B2%E6%8C%87%E5%8D%97)
