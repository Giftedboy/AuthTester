# AuthTester
BurpSuite 越权测试插件，直接下载`/out/artifacts/ext/AuthTester.jar`导入到 Burp 即可

## 使用
* 新建用户cookie
可通过在config Tab栏输入相关信息，也可以在请求信息中通过右键快速创建用户(Add to)
![image](https://user-images.githubusercontent.com/25588005/170309025-84156b91-2a3c-4efb-987c-e1207e18cce8.png)
![image](https://user-images.githubusercontent.com/25588005/170308455-78256629-5f43-4cb7-9274-faa1202c24fc.png)

* 右键替换当前请求Cookie及Headers(Replace with)
可在Proxy以及Repeater中通过右键快速替换当前请求的Cookie以及Headers
![image](https://user-images.githubusercontent.com/25588005/170308532-7447a8dc-d930-40a5-9cd8-5d57afdc3882.png)

* 测试
  * 右键点击即可使用某个用户信息进行快速测试(Test with)
  * 右键点击即可使用某个用户信息进行自动化的扫描，可通过 Host 以及 Path 设置测试目标(AutoTask with)

请求结果会在插件栏内显示
![image](https://user-images.githubusercontent.com/25588005/170308865-0e899167-f1f2-457b-8180-5dfa4d3ce993.png)

