##### 克隆整个项项目
git clone --recursive git@gitlab.ifchange.com:soa/nuclear/track-ssis.git

##### 将所有子模块都切到master分支
git submodule foreach git checkout master

#### 更新代码命令
git pull && git submodule foreach git pull

##### 项目启动
需要在开发工具中添加参数 program arguments     --spring.profiles.active=dev 
然后直接运行 TrackApplication 中的 main 方法即可

##### GIT 添加子模块
git submodule add <repository> <path> 
git add .
git commit -m "add submodule"
git push

##### 切换远程仓库地址
git remote set-url origin SRV_URL
cd tob-common
git remote set-url origin git@gitlab.ifchange.com:soa/tob-common.git
修改 .gitmodules 文件
[submodule "tob-common"]
	path = tob-common
	url = git@gitlab.ifchange.com:soa/tob-common.git
	
##### 依赖注入注解（更偏重使用 @Inject）
@Inject     这是jsr330规范的实现，
@Resource   是jsr250的实现，这是多年前的规范，
@Autowired  是spring的实现，如果不用spring一般用不上这个

##### 项目打包
````
参数： BRANCH 需要打包的GIT分支，默认值：master

build.sh $BRANCH
````
###### 注意：项目打包时 GIT 用户必须可以克隆 track-ssis 和 tob-common 两个项目的权限

##### Docker运行项目（HOST:PORT 是镜像仓库地址）
````
（可选）参数： JAR_URL  JAR的下载路径, 如果不传这个参数请确保最新JAR已经存放在本地挂载目录$HOST_VOLUME中  
      注意： $HOST_VOLUME目录最后的子目录名为 $JAR_NAME 
  更多可选参数请参考： tob-common/spring-boot-dok/README.md

删除之前启动的服务
docker  rm -f ssis-serve

创建需要挂载的目录, 目录以 ssis-serve 结尾
mkdir -p /home/${USER}/Documents/deploys/ssis-serve

docker run -d --restart always \
       -p 20008:20008 \
       -v /home/${USER}/Documents/deploys:/mnt \
       --name=ssis-serve \
       --hostname=ddocata-track-serve.001 \
       -e JAR_NAME=ssis-serve \
       -e JAR_ENV=dev \
       -e HOST_mapping=192.168.1.66__dev.gsystem.rpc \
       -e JAR_URL=http://192.168.20.141/download/jars/@release/ssis-serve.jar \
       192.168.1.233:5000/tob/spring-boot:2.0
````