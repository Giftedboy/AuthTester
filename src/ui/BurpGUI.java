package ui;


import burp.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BurpGUI {
    public JPanel root;
    private JTabbedPane tabbedPane1;
    private JPanel configTab;
    private JPanel ResultBox;
    private JPanel topBox;
    private JPanel middleBox;
    private JPanel buttomBox;
    private JTabbedPane tabbedPane3;
    public JTable userTaskTable;
    public JTable autoTaskTable;
    private JScrollPane autoTaskscroll;
    private JScrollPane userTaskScroll;
    private JTextArea reqText;
    private JTextArea mReqText;
    private JTextArea mRepText;
    private JTextArea repText;
    private JTable configTable;
    private JTabbedPane taskTablePane;
    private JButton addButton;
    private JButton delButton;
    public JRadioButton autoTaskRadio;
    private JTextArea headers;
    private JTextField cookie;
    private JTextField host;
    private JTextField path;
    private JTextField username;
    public JLabel statusText;
    public JLabel taskProcess;
    private JButton editButton;
    public JLabel failedCount;
    public CustomTableModel configModel;
    public CustomTableModel userTaskModel;
    public CustomTableModel autoTaskModel;
    IBurpExtenderCallbacks callbacks;
    IExtensionHelpers helpers;
    int tablePaneFlag = 0;

    public BurpGUI(IBurpExtenderCallbacks cb){
//        autoTaskTable.setBackground();
        this.callbacks = cb;
        this.helpers = cb.getHelpers();

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userdata = username.getText();
                String pathdata = path.getText();
                String hostdata = host.getText();
                String cookiedata = cookie.getText();
                String headersdata = headers.getText();
                if(!Objects.equals(userdata, "") && !Objects.equals(hostdata, "")){
                    if(Objects.equals(pathdata, "")){pathdata="/";}//空则表示根路径
                    configModel.addData(Arrays.asList(userdata, hostdata, pathdata, cookiedata,headersdata),null,null);
                }
            }
        });
        delButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int flag = configTable.getSelectedRow();
                if(flag!=-1){
                    configModel.deleteData(flag);
                }
            }
        });
//        autoTaskRadio.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if(autoTaskRadio.isSelected()){
//                    statusText.setText("自动扫描已开启");
//                    //开启扫描任务
////                    new Thread(new Runnable(){
////                        public void run() {
////                            runRequests(invocation.getSelectedMessages(),userdata,flag);
////                        }
////                    }).start();
//
//                }else{
//                    statusText.setText("自动扫描已关闭");
//                }
//            }
//        });
        userTaskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getButton() == MouseEvent.BUTTON1){
                    int rowIndex = userTaskTable.getSelectedRow();
                    //然后获取userTaskModel中的数据
                    IHttpRequestResponsePersisted oldreqrep = userTaskModel.taskData.get(rowIndex*2);
                    IHttpRequestResponsePersisted newreqrep = userTaskModel.taskData.get(rowIndex*2+1);
                    reqText.setText(helpers.bytesToString(oldreqrep.getRequest()));
                    repText.setText(helpers.bytesToString(oldreqrep.getResponse()));
                    mReqText.setText(helpers.bytesToString(newreqrep.getRequest()));
                    mRepText.setText(helpers.bytesToString(oldreqrep.getResponse()));
                }
            }
        });
        autoTaskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getButton() == MouseEvent.BUTTON1){
                    int rowIndex = autoTaskTable.getSelectedRow();
                    //然后获取autoTaskModel中的数据
                    IHttpRequestResponsePersisted oldreqrep = autoTaskModel.taskData.get(rowIndex*2);
                    IHttpRequestResponsePersisted newreqrep = autoTaskModel.taskData.get(rowIndex*2+1);
                    reqText.setText(helpers.bytesToString(oldreqrep.getRequest()));
                    repText.setText(helpers.bytesToString(oldreqrep.getResponse()));
                    mReqText.setText(helpers.bytesToString(newreqrep.getRequest()));
                    mRepText.setText(helpers.bytesToString(oldreqrep.getResponse()));
                }
            }
        });

//        taskTablePane.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                super.mouseClicked(e);
//                tablePaneFlag = taskTablePane.getSelectedIndex();
//
//            }
//        });
        configTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int rowIndex = configTable.getSelectedRow();
                List<String> userinfo = configModel.userData.get(rowIndex);
                username.setText(userinfo.get(0));
                host.setText(userinfo.get(1));
                path.setText(userinfo.get(2));
                cookie.setText(userinfo.get(3));
                headers.setText(userinfo.get(4));
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowIndex = configTable.getSelectedRow();
                if(rowIndex!=-1){
                    String userdata = username.getText();
                    String pathdata = path.getText();
                    String hostdata = host.getText();
                    String cookiedata = cookie.getText();
                    String headersdata = headers.getText();
                    if(!Objects.equals(userdata, "") && !Objects.equals(hostdata, "")){
                        if(Objects.equals(pathdata, "")){pathdata="/";}//空则表示根路径
                        configModel.changeData(rowIndex,Arrays.asList(userdata, hostdata, pathdata, cookiedata,headersdata));
                    }
                }
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
         configModel = new CustomTableModel(1);
         userTaskModel = new CustomTableModel(2);
         autoTaskModel = new CustomTableModel(2);

        configTable = new JTable(configModel);
        userTaskTable = new JTable(userTaskModel);
        autoTaskTable = new JTable(autoTaskModel);

//        username = new JTextField();
//        host = new JTextField();
//        path = new JTextField();
//        cookie = new JTextField();
//        headers = new JTextField();
//        userTaskModel.addData(Arrays.asList("1","1","1","1","1"));


    }
    public static void setOneRowBackgroundColor(JTable table, int rowIndex,
                                                Color color) {
        try {
            DefaultTableCellRenderer tcr = new DefaultTableCellRenderer() {

                public Component getTableCellRendererComponent(JTable table,
                                                               Object value, boolean isSelected, boolean hasFocus,
                                                               int row, int column) {
                    if (row == rowIndex) {
                        setBackground(color);
                        setForeground(Color.WHITE);
                    }else if(row > rowIndex){
                        setBackground(Color.BLACK);
                        setForeground(Color.WHITE);
                    }else{
                        setBackground(Color.BLACK);
                        setForeground(Color.WHITE);
                    }

                    return super.getTableCellRendererComponent(table, value,
                            isSelected, hasFocus, row, column);
                }
            };
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                table.getColumn(table.getColumnName(i)).setCellRenderer(tcr);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
