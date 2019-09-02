1. 建议配置Linux系统网络信息
````
* 在 /etc/sysctl.conf 最后加入如下代码
   #jgroups required 5M
   net.core.rmem_max = 5242880
   net.core.wmem_max = 5242880
   
* 执行命令 sysctl -p （或者： sudo sysctl -p）
````
