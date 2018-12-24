# ant作业封装
	## 所需jar包
* commons-codec-1.10.jar
* commons-collections-3.2.1.jar
* fastjson-1.1.35.jar
* isrpClientApi-1.1.8.jar
* log4j-1.2.13.jar
* sun-ws-allV3.1.5.jar
* SunECMClientV3.1.5.jar
* thumbnailator-0.4.8.jar
* xstream-1.3.1.jar
* job
* commons-io-2.6.jar
* spring-2.0.jar
---
##isrp:内容平台上传电子凭证
### 功能描述：上传文件到内容平台，更新数据库 
1. clientIp // 连接内容平台的ip 必填
1. clientPort// 端口号 必填
1. clientName// 服务名 默认isrp 必填
1. clientAppid// 服务id 内容平台帮超柜注册的-101005 必填
1. clientUserid// 用户id stms 必填
1. clientPwd// 用户密码 stms@132 必填
1. clientjgh// 机构号 04051 默认省级 必填
1. clientfrh// 法人代码 0303 默认省级 必填
1. clientgyh// 柜员号 nhadmin 默认省级 必填
1. filepath// 上传的电子凭证的根路径必填 （根目录的三级目录是日期目录，有点局限）
1. filetype// 上传的文件类型,若填空，默认pdf
1. datasource 数据库连接 必填
---
### 返回的结果response--success，fail
***
##  order:工单处理
1. datasource 数据库连接 必填
1. smsclient:工单处理
1. machine ip必填
1. port 必填
1. timeOut 超时时间，有默认值 ;
1. message 发送的信息
1. phone 发送信息的手机号码 必填
---
###发送的结果response--success，fail