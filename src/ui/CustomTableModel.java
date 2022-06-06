package ui;

import burp.IHttpRequestResponsePersisted;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class CustomTableModel extends AbstractTableModel {
    int type;
    public List<List> userData = new ArrayList();
    public List<IHttpRequestResponsePersisted> taskData = new ArrayList();
    public CustomTableModel(int type){
        this.type = type;
    }
    @Override 
    public int getRowCount() {
        return userData.size();
    }

    @Override
    public int getColumnCount() {
        if(this.type==1){
            return 5;
        }
        return 7;
    }
    public void addData(List rowData,IHttpRequestResponsePersisted oldreqrep,IHttpRequestResponsePersisted newreqrep){
        userData.add(rowData);
        if(oldreqrep!=null){
            taskData.add(oldreqrep);
            taskData.add(newreqrep);
        }
        fireTableDataChanged();
    }
    public  void deleteData(int rowIndex){
        userData.remove(rowIndex);
        fireTableDataChanged();
    }

    public void changeData(int rowIndex, List rowData){
        userData.remove(rowIndex);
        userData.add(rowIndex,rowData);
        fireTableDataChanged();
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        return userData.get(rowIndex).get(columnIndex);
    }

    @Override
    public String getColumnName(int columnIndex){
        if(type==1){
            switch (columnIndex){
                case 0:
                    return "Name";
                case 1:
                    return "Host";
                case 2:
                    return "Path";
                case 3:
                    return "Cookie";
                case 4:
                    return "Headers";
                default:
                    return null;
            }
        }else{
            switch (columnIndex){
                case 0:
                    return "User";
                case 1:
                    return "Host";
                case 2:
                    return "Path";
                case 3:
                    return "Code";
                case 4:
                    return "Statu";
                case 5:
                    return "ORepLen";
                case 6:
                    return "MRepLen";
                default:
                    return null;
        }

    }
    }
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){
//        if(columnIndex == 0){
//            return false;
//        }
//        return true;
        return false;
    }
}
