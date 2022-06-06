package burp;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import ui.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BurpExtender implements IBurpExtender,ITab,IContextMenuFactory,IExtensionStateListener{

    private JPanel jPanelMain;
    private IBurpExtenderCallbacks callbacks;
    private boolean autoTaskStaus = false;
    BurpGUI ui;
    IExtensionHelpers helpers;
    Thread autotaskThread;
    String autoTaskUser="";
    int failedCount = 0;
    //    public BurpExtender(){
//
//    }
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks cb) {
        this.callbacks = cb;
        this.callbacks.setExtensionName("AuthTester");
        this.callbacks.registerContextMenuFactory(this);
        this.callbacks.registerExtensionStateListener(this);
        this.helpers = cb.getHelpers();
        buildUI();

    }
    private void buildUI(){
        SwingUtilities.invokeLater(()->{
            this.ui = new BurpGUI(this.callbacks);
            jPanelMain = ui.root;
            this.callbacks.customizeUiComponent(jPanelMain);
            this.callbacks.addSuiteTab(BurpExtender.this);
        });
    }

    @Override
    public String getTabCaption() {
        return "AuthTester";
    }

    @Override
    public Component getUiComponent() {
        return jPanelMain;
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        ArrayList<JMenuItem> menus = new ArrayList<>();
        // 在请求是历史中，可发送请求给插件自动测试
        JMenuItem addmenuItem = new JMenuItem("Add to");
        addmenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //添加到config
                String user = JOptionPane.showInputDialog(null, "Enter New User: ");
                if (user!=null) {
                    IHttpRequestResponse selectedMessage = invocation.getSelectedMessages()[0];
                    IRequestInfo iRequestInfo = helpers.analyzeRequest(selectedMessage);
                    List<String> headers = iRequestInfo.getHeaders();
                    String cookie = null;
                    String host = iRequestInfo.getUrl().getHost();
                    String path = iRequestInfo.getUrl().getPath();
                    for(String header:headers){
                        if(header.startsWith("Cookie: ")){
                            cookie = header.substring(7);
                            break;
                        }
                    }
                    if (cookie==null){
                        cookie = "";
                    }
                    ui.configModel.addData(Arrays.asList(user,host,path,cookie,""),null,null);
                }

            }
        });
        menus.add(addmenuItem);

        if(IContextMenuInvocation.CONTEXT_PROXY_HISTORY == invocation.getInvocationContext() ||
                IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST == invocation.getInvocationContext() ||
                IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE == invocation.getInvocationContext() || invocation.getToolFlag() == callbacks.TOOL_PROXY || invocation.getToolFlag() == callbacks.TOOL_REPEATER){

            for(int i=0;i<ui.configModel.getRowCount();i++){
                //获取到对应用户的配置信息
                List userdata = ui.configModel.userData.get(i);
                String username = (String) userdata.get(0);
                String cookie = (String) userdata.get(3);
                String headers = (String) userdata.get(4);
                JMenuItem menuItem = new JMenuItem("Test With "+ username);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //发送自动测试请求
                         //callbacks.printError(cookie);
                        new Thread(new Runnable(){
                            public void run() {
                                runRequests(invocation.getSelectedMessages(),userdata,1);
                            }
                        }).start();
                    }
                });
                menus.add(menuItem);
            }
        }
        if((invocation.getToolFlag() == callbacks.TOOL_PROXY && (IContextMenuInvocation.CONTEXT_PROXY_HISTORY != invocation.getInvocationContext() &&
                IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST != invocation.getInvocationContext() &&
                IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE != invocation.getInvocationContext())) || invocation.getToolFlag() == callbacks.TOOL_REPEATER){
            // 在proxy拦截以及repeater中额外提供替换cookie及headers的功能

            for(int i=0;i<ui.configModel.getRowCount();i++){
                //获取到对应用户的配置信息

                List userdata = ui.configModel.userData.get(i);
                String username = (String) userdata.get(0);
                String cookie = (String) userdata.get(3);
                String headers = (String) userdata.get(4);

                JMenuItem menuItem = new JMenuItem("Replace With "+ username);
                menuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //替换cookie和headers
                        IHttpRequestResponse selectedMessage = invocation.getSelectedMessages()[0]; // 获取选中的消息
                        byte[] newRequest = changeRequest(selectedMessage, userdata);
                        selectedMessage.setRequest(newRequest);
                    }
                });
                menus.add(menuItem);
            }
        }


        for(List userinfo:this.ui.configModel.userData){
            String user = (String) userinfo.get(0);
            JMenuItem automenuItem = new JMenuItem("AutoTask With "+ user);
            autoTaskUser = user;
            automenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //弹出提示框，要求输入从哪个history开始
                    if(!autoTaskStaus){
                        String indexHistorystr = JOptionPane.showInputDialog(null,"请输入扫描任务从哪个历史请求开始：");
                        if(indexHistorystr!=null){
                            int indexHistory = Integer.parseInt(indexHistorystr);
                            autoTaskStaus = true;
                            autotaskThread = new Thread(new Runnable(){
                                public void run() {
                                    try {
                                        AutoTask(userinfo,indexHistory);
                                    } catch (InterruptedException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                            });
                            autotaskThread.start();
                        }
                    }else{
                        JOptionPane.showMessageDialog(null, "autoTask is running!");
                    }


                }
            });
            menus.add(automenuItem);
        }
        JMenuItem stopmenuItem = new JMenuItem("Stop AutoTask");
        stopmenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAutoTask();
            }});
        menus.add(stopmenuItem);
        return menus;
    }

    private byte[] changeRequest(IHttpRequestResponse httpRequestResponse, List userdata){
        IRequestInfo iRequestInfo = helpers.analyzeRequest(httpRequestResponse); // 解析请求信息结构
        String request = new String(httpRequestResponse.getRequest());
        byte[] body = request.substring(iRequestInfo.getBodyOffset()).getBytes();
        List<String> headers = iRequestInfo.getHeaders();
        String newHeadersStr = (String) userdata.get(4);
        String[] newHeaders = newHeadersStr.split("\n");

        // 替换请求中cookies
        int index = -1;     // cookie头在headers中的位置
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.startsWith("Cookie:")) {
                index = i;
            }
        }
        // 如果请求中不存在Cookies，添加cookies头
        if (index == -1){
            headers.add("Cookie: "+(String)userdata.get(3));
        } else {
            // 替换headers中Cookie，Cookie在headers中位置保持不变
            headers.remove(index);
            headers.add(index, "Cookie: "+(String) userdata.get(3));
        }

        if(!newHeadersStr.equals("")){
            // 如果new header不为空，替换请求中new header
            for(int i=0;i<newHeaders.length;i++){
                //获取头
                int indexOf = newHeaders[i].indexOf(":");
                if (indexOf > 0){
                    int index2 = -1;    // newHeader头在headers中的位置
                    String newHeader = newHeaders[i].substring(0, indexOf);     // 如"Auth: xxxxx"请求头中的 "Auth"

                    for (int j = 0; j < headers.size(); j++) {
                        String header = headers.get(j);
                        if ( header.startsWith(newHeader)){
                            index2 = j;
                        }
                    }

                    // 如果请求中不存在new header，添加new header
                    if (index2 == -1){
                        headers.add(newHeaders[i]);
                    } else {
                        // 替换headers中new header，new header在headers中位置保持不变
                        headers.remove(index2);
                        headers.add(index2, newHeaders[i]);
                    }
                }
            }
        }

        // 重新组装请求
        return helpers.buildHttpMessage(headers, body);
    }
    private void AutoTask(List userinfo,int indexHistory) throws InterruptedException {

        //拿到user host path，然后开始扫描任务

        //start
        IHttpRequestResponse[] historymsg = this.callbacks.getProxyHistory();
        if (indexHistory==0)indexHistory=1;
        IHttpRequestResponse[] target = new IHttpRequestResponse[historymsg.length];

        System.arraycopy(historymsg,indexHistory-1,target,0,historymsg.length+1-indexHistory);
        runRequests(target,userinfo,2);
        autoTaskStaus = false;
        ui.autoTaskRadio.setSelected(false);
        ui.statusText.setText("自动扫描已关闭");
        JOptionPane.showMessageDialog(null,(String)userinfo.get(0)+"扫描任务完成！");
    }
    private void stopAutoTask(){
        autotaskThread.interrupt();
        autoTaskStaus = false;
        ui.autoTaskRadio.setSelected(false);
        ui.statusText.setText("自动扫描已关闭");
        JOptionPane.showMessageDialog(null,autoTaskUser + "用户进程结束！");
    }
    private void runRequests(IHttpRequestResponse[] HttpRequestResponses,List userdata,int flag){
        int len = HttpRequestResponses.length;
        int index = 1;
        if(flag==2){
            //展示任务进度
            ui.autoTaskRadio.setSelected(true);
            ui.statusText.setText("自动扫描已开启");
        }
        for(IHttpRequestResponse oReqRep: HttpRequestResponses){
            if(flag==2){
                ui.taskProcess.setText(index+"/"+len);
                index++;
                //拿到path host看是否匹配，不匹配则continue
                String host = (String)userdata.get(1);
                String path = (String)userdata.get(2);
                String[] paths = path.split(";");
                IRequestInfo repinfo =  helpers.analyzeRequest(oReqRep);
                URL url = repinfo.getUrl();

                if(!url.getHost().startsWith(host)){
                    continue;
                }
                boolean targetflag = false;
                for(String mypath:paths){
                    if(url.getPath().startsWith(mypath)){
                        targetflag = true;
                    }
                }
                if(!targetflag){
                    continue;
                }

            }
            IHttpRequestResponse newHttpRequestResponse = null;
            try{
                byte[] newRequest = changeRequest(oReqRep, userdata);
                newHttpRequestResponse = callbacks.makeHttpRequest(oReqRep.getHttpService(),newRequest);
                //请求失败的话，不会添加到记录里
                addRequestRecord(userdata,newHttpRequestResponse,oReqRep,flag);
                 //1表示为Usertask，2为Autotask
            }catch (Throwable e){
                callbacks.printOutput("Request no Response!");
                failedCount++;
                ui.failedCount.setText(Integer.toString(failedCount));
            }


        }
//        try{
//            String user = "";
//
//            List<UserEntry> userEntries = userTableModel.getAllEntries();
//            for ( UserEntry userEntry: userEntries){
//                byte[] newRequest = changeRequest(originallHttpRequestResponse, userEntry);
//
//                IHttpRequestResponse newHttpRequestResponse = callbacks.makeHttpRequest(
//                        originallHttpRequestResponse.getHttpService(),
//                        newRequest);
//                user = userEntry.getName();
//                addRequestRecord(newHttpRequestResponse, user);  // 给History表添加一行请求记录
//            }
//        } catch (Throwable e){
//            PrintWriter writer = new PrintWriter(callbacks.getStderr());
//            writer.println(e.getMessage());
//            e.printStackTrace(writer);
//        }
    }
    private void addRequestRecord(List userdata,IHttpRequestResponse newReqRep,IHttpRequestResponse oReqRep,int flag){


        IHttpRequestResponsePersisted newrequestResponsePersisted = callbacks.saveBuffersToTempFiles(newReqRep);
        IHttpRequestResponsePersisted oldrequestResponsePersisted = callbacks.saveBuffersToTempFiles(oReqRep);

        IRequestInfo newrequestInfo = helpers.analyzeRequest(newReqRep);

        byte[] newresponseByte = newReqRep.getResponse();
        int newlength;
        short newstatusCode;
        if(newresponseByte.length!=0){
            IResponseInfo  newresponseInfo = helpers.analyzeResponse(newresponseByte);
            newlength = newresponseByte.length-newresponseInfo.getBodyOffset();
            newstatusCode = newresponseInfo.getStatusCode();
        }else{
            newlength = 0;
            newstatusCode = 0;
        }

        byte[] oldreponseByte = oReqRep.getResponse();
        int oldlength;
        //需要处理响应为空的状况
        if(oldreponseByte.length!=0){
            IResponseInfo  oldresponseInfo = helpers.analyzeResponse(oldreponseByte);
            oldlength = oldreponseByte.length-oldresponseInfo.getBodyOffset();
        }else{
            oldlength = 0;
        }


        URL url = newrequestInfo.getUrl();
        short Statu;
        if(oldlength==newlength){
            Statu = 1;
        }else{
            Statu = 0;
        }
        List data = Arrays.asList((String) userdata.get(0),(String)url.getHost(),(String)url.getPath(),newstatusCode,Statu,oldlength,newlength,oldrequestResponsePersisted,newrequestResponsePersisted);
//        PrintWriter writer = new PrintWriter(callbacks.getStdout());
//        writer.println(data);
        callbacks.printOutput(String.valueOf(data));
//        List<IHttpRequestResponsePersisted> taskData = new ArrayList();
//        taskData.add(newrequestResponsePersisted);
//        taskData.add(oldrequestResponsePersisted);
        if(flag==1){
            ui.userTaskModel.addData(data,oldrequestResponsePersisted,newrequestResponsePersisted);
        }
        else{
            ui.autoTaskModel.addData(data,oldrequestResponsePersisted,newrequestResponsePersisted);
        }



    }

    @Override
    public void extensionUnloaded() {
        autotaskThread.interrupt();
    }


}
