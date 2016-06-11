# music-share

项目直接使用Android Studio导入即可

把手机连接上电脑，用Android Studio直接在手机上调试，或者导出APK到手机直接运行。

# 依赖

Android SDK版本15以上

手机操作系统Andoird 4.0以上，需要支持Wi-Fi Direct功能

无持久层，无外部接口

# 步骤

下载zip，解压到一个目录里

打开Android Studio，点击Import project，导入该目录

根据提示安装相应的依赖就行了，如果连不上google需要使用VPN

# 程序结构

res文件夹里是资源文件

程序入口为MainActivity，由MainActivity启动WifiDirectActivity

WifiDirectActivity里有两个Fragment，分别是DeviceListFragment和DeviceDetailFragment

两个Service是用于文件发送的，一个广播接收器，接收API发出的广播
