/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SemanticParser;

import java.awt.Toolkit;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Vector;
import javax.swing.JTable;

/**
 *
 * @author Sourav
 */
//class for LR Items' objects
class LRItem {

    int prevState;
    String gotoSymbol;
    Vector leftOfProd = new Vector();
    Vector rightOfProd = new Vector();
    Vector pointerPos = new Vector();
}

public class NewJFrame extends javax.swing.JFrame {

    //
    int nx;
    //parsing table
    final int TABLESIZE = 1000;
    String[][] parseTable = new String[TABLESIZE][TABLESIZE];

    //Vector for storing Contexts
    Vector context = new Vector();

    //Vector for storing productions
    Vector lhs = new Vector();//{ "S'","S","PN","FN","LN" };
    Vector rhs = new Vector();//{ "S,$","PN,$","FN,LN,$","tushar,$","gautam,$" };

    //Vector to for indices of terminals and non-terminals
    Vector index = new Vector();
    Vector terminals = new Vector();
    Vector nonTerminals = new Vector();

    //Vector for LR items
    Vector<LRItem> LRI = new Vector();

    //stack for parsing Strings
    final int STACKSIZE = 100000;
    int top = -1;
    String[] parseStack = new String[STACKSIZE];

    /////
    //Functions
    //String parsing stack's push and pop function
    //push and pop function defination
    void pop() {
        if (top < 0) {
            System.out.println("Underflow");
        } else {
            top--;
        }
    }

    void push(String token) {
        if (top == STACKSIZE) {
            System.out.println("Overflow");
        } else {
            top++;
            parseStack[top] = token;
        }
    }

    //function to fill index Vector
    void fillIndex() {
        index = new Vector();
        //Index for Action part of Parsing Table
        for (int i = 0; i < terminals.size(); i++) {
            index.add(terminals.get(i));
        }

        index.add("$");

        //Index for Goto part of Parsing Table
        for (int i = 0; i < nonTerminals.size(); i++) {
            index.add(nonTerminals.get(i));
        }
    }

    //function to retrive productions, terminals and non-terminals
    void retriveFromDB() {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/semanticParser", "root", "tushar@123");

            Statement stmt = (Statement) con.createStatement();

            //retrive productions
            ResultSet rs = stmt.executeQuery("select * from productions");
            //rs = stmt.executeQuery("alter user 'root'@'localhost' identified with mysql_native_password by 'tushar@123'");
            //rs = stmt.executeQuery("select * from productions");
            while (rs.next()) {
                lhs.add(rs.getString(1));
                rhs.add(rs.getString(2));
                if (rs.getString(3) != null) {
                    context.add(rs.getString(3));
                } else {
                    context.add("-");
                }
                //System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
            }

            //retrive terminals
            rs = stmt.executeQuery("select * from terminals");
            while (rs.next()) {
                terminals.add(rs.getString(1));
                //System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
            }

            //retrive non-terminals
            rs = stmt.executeQuery("select * from nonTerminals");
            while (rs.next()) {
                nonTerminals.add(rs.getString(1));
                //System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
            }

            con.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    //function to find index of terminals and non-terminals indicating columns in parse table
    int findIndex(String str) {
        for (int i = 0; i < index.size(); i++) {
            if (index.get(i).equals(str)) {
                return i;
            }
        }
        return -1;
    }

    //function to convert integer to String
    static String toString(int x) {
        return String.valueOf(x);
    }

    //function to convert String to integers
    static int toInteger(String str) {
        int i = 0;
        //need to change
        if (str.charAt(0) == 'R' || str.charAt(0) == 'S') {
            i = 1;
        }

        String temp = "";
        int n;
        n = str.length();
        int num;
        for (; i < n; i++) {
            temp += str.charAt(i);
        }

        num = Integer.parseInt(temp);
        return num;
    }

    //function to find the production number
    int findProductionNumber(String l, String r) {
        for (int i = 0; i < lhs.size(); i++) {
            if (l.equals(lhs.get(i)) && r.equals(rhs.get(i))) {
                return i;
            }
        }
        return -1;
    }

    //funtion to check weather String is Non-terminal or not
    boolean isNonTerminal(String str) {
        for (int i = 0; i < nonTerminals.size(); i++) {
            if (str.equals(nonTerminals.get(i))) {
                return true;
            }
        }
        return false;
    }

    //function to find length of production
    int getProductionLength(String str) {
        int count = 0, n = str.length();
        for (int i = 0; i < n; i++) {
            if (str.charAt(i) == ',') {
                count++;
            }
        }
        return count;
    }

    //function for variable after . in RHS of the production
    String getRHSVariable(String str, int index) {
        int n = str.length(), count = 0;
        String temp = "";
        for (int i = 0; i < n; i++) {
            if (str.charAt(i) == ',' && count < index) {
                temp = "";
                count++;
            } else if (str.charAt(i) == ',' && count == index) {
                return temp;
            } else {
                temp += str.charAt(i);
            }
        }
        return temp;
    }

    //Function for creating LR Items
    void createLRItems() {
        fillIndex();
        LRI = new Vector();
        LRItem ptr = new LRItem();
        ptr.leftOfProd.add("S'");
        ptr.rightOfProd.add("S,$");
        ptr.pointerPos.add(0);

        LRI.add(ptr);

        for (int nodeNo = 0; nodeNo < LRI.size(); nodeNo++) {
            ptr = LRI.get(nodeNo);
            //System.out.println("PTR:"+ptr.leftOfProd);
            boolean[] added = new boolean[lhs.size()];

            for (int i = 0; i < lhs.size(); i++) {
                added[i] = false;
            }
            ///need to fill added
            for (int i = 0; i < ptr.leftOfProd.size(); i++) {
                for (int j = 0; j < lhs.size(); j++) {
                    if (ptr.leftOfProd.get(i).equals(lhs.get(j)) && ptr.rightOfProd.get(i).equals(rhs.get(j))) {
                        added[j] = true;
                    }
                }
            }

            //Loop for traversing productions in the Node
            for (int i = 0; i < ptr.rightOfProd.size(); i++) {
                //Loop for Traversing lhs for adding productions
                //Clouser
                for (int j = 0; j < lhs.size(); j++) {
                    String currentVar;
                    currentVar = getRHSVariable(String.valueOf(ptr.rightOfProd.get(i)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(i))));
                    if (currentVar.equals(lhs.get(j)) && !added[j]) {
                        ptr.leftOfProd.add(lhs.get(j));
                        ptr.rightOfProd.add(rhs.get(j));
                        ptr.pointerPos.add(0);
                        //LRI.get(nodeNo).leftOfProd.add(lhs.get(j));
                        ///LRI.get(nodeNo).rightOfProd.add(rhs.get(j));
                        //LRI.get(nodeNo).pointerPos.add(0);

                        added[j] = true;
                        //System.out.println(lhs.get(j)+"->"+rhs.get(j));
                    }
                }
            }
            //LRI.add(nodeNo, ptr);
            //Goto
            for (int j = 0; j < ptr.leftOfProd.size(); j++) {
                if (!(getRHSVariable(String.valueOf(ptr.rightOfProd.get(j)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(j)))).equals("$"))) {
                    boolean[] addedGoto = new boolean[lhs.size()];
                    boolean formed = false;
                    LRItem newItem = new LRItem();

                    for (int k = 0; k < ptr.leftOfProd.size(); k++) {
                        int prodNo = findProductionNumber(String.valueOf(ptr.leftOfProd.get(k)), String.valueOf(ptr.rightOfProd.get(k)));
                        if (getRHSVariable(String.valueOf(ptr.rightOfProd.get(k)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(k)))).equals(getRHSVariable(String.valueOf(ptr.rightOfProd.get(j)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(j))))) && !addedGoto[prodNo] && !(getRHSVariable(String.valueOf(ptr.rightOfProd.get(k)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(k)))).equals("$"))) {
                            newItem.leftOfProd.add(ptr.leftOfProd.get(k));
                            newItem.rightOfProd.add(ptr.rightOfProd.get(k));
                            newItem.pointerPos.add(Integer.parseInt(String.valueOf(ptr.pointerPos.get(k))) + 1);
                            newItem.prevState = nodeNo;
                            newItem.gotoSymbol = getRHSVariable(String.valueOf(ptr.rightOfProd.get(k)), Integer.parseInt(String.valueOf(ptr.pointerPos.get(k))));
                            addedGoto[prodNo] = true;
                            formed = true;
                        }
                    }
                    if (formed) {
                        //find Weather same item is already in the item set or not
                        LRI.add(newItem);
                    }
                }
            }
            //System.out.println("Node:"+nodeNo+","+LRI.get(nodeNo).leftOfProd+"->"+LRI.get(nodeNo).rightOfProd);
        }
    }

    //Print LR Items
    void printLRItems() {
        System.out.println("LR Items:");
        for (int i = 0; i < LRI.size(); i++) {
            System.out.println("State:" + i);
            for (int j = 0; j < LRI.get(i).leftOfProd.size(); j++) {
                System.out.println(LRI.get(i).leftOfProd.get(j) + "->" + LRI.get(i).rightOfProd.get(j) + LRI.get(i).pointerPos.get(j) + "\n" + LRI.get(i).gotoSymbol + "," + LRI.get(i).prevState);
            }
            System.out.println();
        }

    }

    //function for Generating of Parsing Table
    void generateParsingTable() {
        addInitialProductions();
        createLRItems();

        for (int i = 0; i < TABLESIZE; i++) {
            for (int j = 0; j < TABLESIZE; j++) {
                parseTable[i][j] = "-";
            }
        }

        int tableIndex = findIndex("$");
        //System.out.println(parseTable[1][tableIndex]);
        parseTable[1][tableIndex] = "Accept";
        //System.out.println(parseTable[1][tableIndex]);

        for (int state = 1; state < LRI.size(); state++) {
            int row, col, prodNo;
            //System.out.println(getRHSVariable(String.valueOf(LRI.get(state).rightOfProd.get(0)), Integer.parseInt(String.valueOf(LRI.get(state).pointerPos.get(0)))));
            //Entries crossponding to final items (reduce entries)
            if (getRHSVariable(String.valueOf(LRI.get(state).rightOfProd.get(0)), Integer.parseInt(String.valueOf(LRI.get(state).pointerPos.get(0)))).equals("$") && state != 1) {
                row = state;
                col = findIndex("$");
                //System.out.println(row+","+col);

                prodNo = findProductionNumber(String.valueOf(LRI.get(state).leftOfProd.get(0)), String.valueOf(LRI.get(state).rightOfProd.get(0)));
                String concat = toString(prodNo);
                for (int i1 = 0; i1 <= col; i1++) {
                    parseTable[row][i1] = "R" + concat;
                    //System.out.println(parseTable[row][i1]);
                }
            }
            //Entries crossponding to state numbers
            if (isNonTerminal(LRI.get(state).gotoSymbol)) {
                row = LRI.get(state).prevState;
                col = findIndex(LRI.get(state).gotoSymbol);
                String concat = toString(state);

                //System.out.println(row+","+col+LRI.get(state).gotoSymbol);
                parseTable[row][col] = concat;
                //System.out.println(parseTable[row][col]);
            } //Entries crossponding to shift
            else {
                row = LRI.get(state).prevState;
                col = findIndex(LRI.get(state).gotoSymbol);
                String concat = toString(state);
                parseTable[row][col] = "S" + concat;

                //System.out.println(parseTable[row][col]);
            }
        }
    }

    //function for printing Parsing table
    void printParsingTable() {
        System.out.println("Parsing Table:");
        for (int i = 0; i < index.size(); i++) {
            System.out.print(" " + index.get(i) + "\t");
        }

        System.out.println();
        for (int i = 0; i < LRI.size(); i++) {
            System.out.print(i + " ");
            for (int j = 0; j < index.size(); j++) {
                System.out.print(parseTable[i][j] + "\t");
            }
            System.out.println();
        }
    }

    //function for parsing String
    void parseString(String str) {
        //Parsing starts here
        Vector parseStr = new Vector();
        String temp = "";

        //System.out.println("String:" + str);
        resultContextField.setText("");
        parseResult.setText("String:" + str + "\n");

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ' ') {
                parseStr.add(temp);
                temp = "";
            } else {
                temp += str.charAt(i);
            }
        }
        parseStr.add(temp);
        //cout<<parseStr[parseStr.size()-1]<<"\n";
        parseStr.add("$");

        push("0");

        //Parsing Loop
        int n = parseStr.size();
        for (int i = 0; i < n;) {
            int row, col;
            String var = "";
            var += parseStr.get(i);
            col = findIndex(var);
            if (col == -1) {
                parseResult.setText(parseResult.getText() + "ERROR:Unknown Terminal\n");
                //System.out.println("ERROR:Unknown Terminal");
                break;
            }
            //System.out.println(parseTable[toInteger(parseStack[top])][col]);
            //cout<<parseTable[toInteger(parseStack[top])][col]<<",";
            //cout<<"Stack:"<<parseStack[top]<<"\n";
            //cout<<toInteger(parseStack[top])<<","<<col<<"\n";
            if (parseTable[toInteger(parseStack[top])][col].charAt(0) == '-') {
                parseResult.setText(parseResult.getText() + "ERROR\n");
                //System.out.println("ERROR");
                break;
            }
            if (parseTable[toInteger(parseStack[top])][col].charAt(0) == 'S') {
                String pushSymbol = "";
                pushSymbol += parseStr.get(i);
                push(pushSymbol);
                //cout<<"parse table:"<<toInteger(parseTable[toInteger(parseStack[top-1])][col])<<",";
                pushSymbol = toString(toInteger(parseTable[toInteger(parseStack[top - 1])][col]));
                push(pushSymbol);
                //cout<<toString(toInteger(parseTable[toInteger(parseStack[top])][col]))<<"\n";
                i++;
            } else if (parseTable[toInteger(parseStack[top])][col].charAt(0) == 'R') {
                int prodToReduce = toInteger(parseTable[toInteger(parseStack[top])][col]);
                int len, l;
                String pushSymbol = String.valueOf(lhs.get(prodToReduce));
                len = getProductionLength(String.valueOf(rhs.get(prodToReduce)));
                len *= 2;
                l = findIndex(pushSymbol);

                for (int k = 0; k < len; k++) {
                    pop();
                }
                push(pushSymbol);
                push(parseTable[toInteger(parseStack[top - 1])][l]);

                //Print Reduced String
                //System.out.print("Reduced String: ");
                //parseResult.setText(parseResult.getText() + "Reduced String: ");
                parseResult.append("Reduced String: ");
                for (int m = 1; m < top; m += 2) {
                    parseResult.append(parseStack[m] + " ");
                    //parseResult.setText(parseResult.getText() + parseStack[m] + " ");
                    //System.out.print(parseStack[m] + " ");
                }
                for (int m = i; m < n - 1; m++) {
                    parseResult.append(parseStr.get(m) + " ");
                    //parseResult.setText(parseResult.getText() + parseStr.get(m) + " ");
                    //System.out.print(parseStr.get(m) + " ");
                }
                if (!(context.get(prodToReduce).equals("-"))) {
                    //resContextField.append(context.get(prodToReduce)+" ");
                    resultContextField.setText(resultContextField.getText() + " " + context.get(prodToReduce));
                    //parseResult.setText(parseResult.getText() + "\nContext:" + context.get(prodToReduce) + "\n");
                }
                parseResult.append("\n");
                //System.out.println(" Context:"+context.get(prodToReduce));

            } else if (parseTable[toInteger(parseStack[top])][col].equals("Accept")) {
                parseResult.append("Accepted\n");
                //parseResult.setText(parseResult.getText() + "Accepted\n");
                //System.out.println("Accepted");
                break;
            }
        }
        top = -1;
    }

    //function for printing productions
    void printProductions() {
        //Printing all productions
        for (int i = 0; i < lhs.size(); i++) {
            System.out.println(lhs.get(i) + "->" + rhs.get(i));
            //cout << lhs[i] << "->" << rhs[i] << "\n";
        }
        System.out.println();
    }

    void addInitialProductions() {

        //{ "S'","S","PN","FN","LN" };
        //{ "S,$","PN,$","FN,LN,$","tushar,$","gautam,$" };
        /*
        if (nx == 0) {
            nx = 1;
            lhs.add("S'");
            lhs.add("S");
            lhs.add("PN");
            lhs.add("FN");
            lhs.add("LN");
            ////////////
            lhs.add("PN");
            lhs.add("FN");
            lhs.add("MN");
            lhs.add("LN");
            //////
            lhs.add("FN");
            lhs.add("LN");

            //kendriya vidyalaya NFC
            //lhs.add("S");
            //lhs.add("SN");
            //lhs.add("FN");
            //lhs.add("MN");
            //lhs.add("LN");
            /*
            lhs.add("DN");
            lhs.add("DG");  //desigination
            lhs.add("C");   //connector
            lhs.add("A");   //authority
         */
 /*
            context.add("-");
            context.add("-");
            context.add("PERSON NAME");
            context.add("-");
            context.add("-");
            /////
            context.add("PERSON NAME");
            context.add("-");
            context.add("-");
            context.add("-");

            context.add("-");
            context.add("-");

            //context.add("-");
            //context.add("SCHOOL NAME");
            //context.add("-");
            //context.add("-");
            //context.add("-");

            rhs.add("S,$");
            rhs.add("PN,$");
            rhs.add("FN,LN,$");
            rhs.add("tushar,$");
            rhs.add("gautam,$");

            ///////////
            rhs.add("FN,MN,LN,$");
            rhs.add("amit,$");
            rhs.add("kumar,$");
            rhs.add("kanoria,$");
            ///////////
            rhs.add("narendra,$");
            rhs.add("modi,$");

            //rhs.add("SN,$");
            //rhs.add("FN,MN,LN,$");
            //rhs.add("kendriya,$");
            //rhs.add("vidyalaya,$");
            //rhs.add("NFC,$");

            terminals.add("tushar");
            terminals.add("gautam");
            terminals.add("amit");
            terminals.add("kumar");
            terminals.add("kanoria");

            terminals.add("narendra");
            terminals.add("modi");

            //terminals.add("kendriya");
            //terminals.add("vidyalaya");
            //terminals.add("NFC");

            nonTerminals.add("S");
            nonTerminals.add("PN");
            nonTerminals.add("FN");
            nonTerminals.add("LN");
            nonTerminals.add("MN");
            //nonTerminals.add("SN");
            //nonTerminals.add("MN");
            //nonTerminals.add("MN");
            
        }
         */
        lhs = new Vector();
        rhs = new Vector();
        context = new Vector();

        terminals = new Vector();
        nonTerminals = new Vector();

        retriveFromDB();

        printProductions();
        /*
        for(int i=0;i<lhs.size();i++){
            System.out.println(lhs.get(i)+"->"+rhs.get(i));
        }*/

        for (int i = 0; i < terminals.size(); i++) {
            System.out.println(terminals.get(i));
        }
        System.out.println();
        for (int i = 0; i < nonTerminals.size(); i++) {
            System.out.println(nonTerminals.get(i));
        }
        nx = 1;
    }
    //////////

    public NewJFrame() {
        this.setIconImage(Toolkit.getDefaultToolkit().getImage("C:\\Users\\Tushar\\Neatbeans Projects\\Semantic_Parser\\src\\SemanticParser\\logo.png"));
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel4 = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        parseField = new javax.swing.JTextField();
        parseButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        parseResult = new javax.swing.JTextArea();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        resultContextField = new javax.swing.JTextField();
        pannel1 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        parseTableField = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        prodField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        contextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        terminalField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        nonTerminalField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        productionTable = new javax.swing.JTable();
        errorField = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jTextField1.setText("jTextField1");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Semantic Parser");
        setBackground(new java.awt.Color(255, 255, 255));
        setResizable(false);

        jTabbedPane1.setBackground(new java.awt.Color(255, 255, 255));
        jTabbedPane1.setVerifyInputWhenFocusTarget(false);
        jTabbedPane1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTabbedPane1FocusGained(evt);
            }
        });

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel6.setText("Enter String");

        parseButton.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        parseButton.setText("PARSE");
        parseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parseButtonActionPerformed(evt);
            }
        });

        parseResult.setEditable(false);
        parseResult.setColumns(20);
        parseResult.setFont(new java.awt.Font("Monospaced", 0, 16)); // NOI18N
        parseResult.setRows(5);
        jScrollPane1.setViewportView(parseResult);

        jLabel10.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Parse String");

        jLabel11.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel11.setText("Result:");

        jLabel13.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel13.setText("Context:");

        resultContextField.setEditable(false);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(parseButton))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel11)
                                    .addComponent(jLabel13))
                                .addGap(44, 44, 44)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(parseField)
                                    .addComponent(resultContextField))))))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel10)
                .addGap(28, 28, 28)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(parseField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(parseButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(resultContextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 92, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.addTab("Parse String", jPanel3);

        pannel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Parsing Table");

        parseTableField.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        parseTableField.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        parseTableField.setEnabled(false);
        jScrollPane4.setViewportView(parseTableField);

        javax.swing.GroupLayout pannel1Layout = new javax.swing.GroupLayout(pannel1);
        pannel1.setLayout(pannel1Layout);
        pannel1Layout.setHorizontalGroup(
            pannel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        pannel1Layout.setVerticalGroup(
            pannel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pannel1Layout.createSequentialGroup()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 495, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jTabbedPane1.addTab("Parsing Table", pannel1);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel1.setText("Enter Production:");

        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel3.setText("Context:");

        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel4.setText("Terminals:");

        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel5.setText("Non-Terminals:");

        jButton1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jButton1.setText("ADD");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Add Productions & Symbols");

        jButton2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jButton2.setText("ADD");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(52, 52, 52))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(122, 122, 122)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(prodField)
                            .addComponent(contextField))
                        .addGap(10, 10, 10))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(89, 89, 89)
                        .addComponent(terminalField)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jButton2))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(50, 50, 50)
                                .addComponent(nonTerminalField)))
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(37, 37, 37)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(prodField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(37, 37, 37)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(30, 30, 30)
                .addComponent(jButton1)
                .addGap(47, 47, 47)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(terminalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(54, 54, 54)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(nonTerminalField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(39, 39, 39)
                .addComponent(jButton2)
                .addContainerGap(101, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Add Productions & Symbols", jPanel1);

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel12.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Productions");

        productionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        productionTable.setEnabled(false);
        jScrollPane5.setViewportView(productionTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(66, 66, 66)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 396, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("View Productions", jPanel2);

        errorField.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        errorField.setForeground(new java.awt.Color(204, 0, 0));
        errorField.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 538, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(errorField, javax.swing.GroupLayout.PREFERRED_SIZE, 505, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorField, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    //Show Parsing Table and Production
    private void jTabbedPane1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTabbedPane1FocusGained
        errorField.setText("");
        generateParsingTable();
        String column[] = new String[(index.size() + 1)];
        column[0] = "States";

        for (int i = 1; i < index.size() + 1; i++) {
            column[i] = String.valueOf(index.get(i - 1));
        }

        String[][] table = new String[LRI.size()][index.size() + 1];
        for (int i = 0; i < LRI.size(); i++) {
            for (int j = 0; j < index.size() + 1; j++) {
                if (j == 0) {
                    table[i][j] = toString(i);
                } else {
                    table[i][j] = parseTable[i][j - 1];
                }
            }
        }

        JTable pTable = new JTable(table, column);

        pTable.setEnabled(false);
        //pTable.setBounds(5,5,500,500);

        parseTableField.setModel(pTable.getModel());

        //Show Productions
        String[] productionColumn = {"Productions", "Context"};
        String[][] prodContextTable = new String[lhs.size() - 1][2];
        String prod = " ", temp, rhsProd;

        for (int i = 1; i < lhs.size(); i++) {
            prod = lhs.get(i) + " -> ";
            temp = "";
            rhsProd = String.valueOf(rhs.get(i));
            for (int j = 0; j < rhsProd.length() - 1; j++) {
                if (rhsProd.charAt(j) != ',') {
                    temp += rhsProd.charAt(j);
                } else {
                    prod += temp + " ";
                    temp = "";
                }
            }
            prod += temp;
            prodContextTable[i - 1][0] = prod;
            prodContextTable[i - 1][1] = String.valueOf(context.get(i));
        }
        //JButton buttons[]=new JButton[10];
        JTable pCTable = new JTable(prodContextTable, productionColumn);
        //pCTable.getColumn(2);
        pTable.setEnabled(false);
        //pTable.setBounds(5,5,500,500);

        productionTable.setModel(pCTable.getModel());
        //System.out.println(prod+"hello");
    }//GEN-LAST:event_jTabbedPane1FocusGained

    //Add Terminals and Non-Terminals
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        String terminalText = terminalField.getText();
        String nonTerminalText = nonTerminalField.getText();
        String res = "";
        String temp = "";

        if (terminalText.length() != 0 || nonTerminalText.length() != 0) {
            terminalText += ',';
            for (int i = 0; i < terminalText.length(); i++) {
                if (terminalText.charAt(i) == ',') {
                    terminals.add(temp);
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        Connection con = DriverManager.getConnection(
                                "jdbc:mysql://localhost:3306/semanticParser", "root", "tushar@123");

                        Statement stmt = (Statement) con.createStatement();
                        String sql = "INSERT INTO terminals " + "VALUES ('" + temp + "')";
                        stmt.executeUpdate(sql);
                        //ResultSet rs = stmt.executeQuery("select * from productions");

                        con.close();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    temp = "";
                } else {
                    temp += terminalText.charAt(i);
                }
            }

            errorField.setForeground(java.awt.Color.GREEN);
            errorField.setText("Added");

            nonTerminalText += ',';
            temp = "";
            for (int i = 0; i < nonTerminalText.length(); i++) {
                if (nonTerminalText.charAt(i) == ',') {
                    nonTerminals.add(temp);
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        Connection con = DriverManager.getConnection(
                                "jdbc:mysql://localhost:3306/semanticParser", "root", "tushar@123");

                        Statement stmt = (Statement) con.createStatement();
                        String sql = "INSERT INTO nonterminals " + "VALUES ('" + temp + "')";
                        stmt.executeUpdate(sql);
                        //ResultSet rs = stmt.executeQuery("select * from productions");

                        con.close();
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    temp = "";
                } else {
                    temp += nonTerminalText.charAt(i);
                }
            }
            terminalField.setText("");
            nonTerminalField.setText("");

            errorField.setForeground(java.awt.Color.GREEN);
            errorField.setText("Added");

        } else {
            errorField.setForeground(java.awt.Color.red);
            errorField.setText("ERROR: Terminal & Non-Terminal Field is Empty");
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    //Add Productions
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        String prodText = prodField.getText();
        String contextText = contextField.getText();
        String lhsText, rhsText, sql;

        if (prodText.length() != 0) {
            //Lhs of Production
            lhsText = prodText.substring(0, prodText.indexOf('-'));
            lhs.add(lhsText);
            //System.out.println(lhsText);

            //Rhs of Production
            rhsText = prodText.substring(prodText.indexOf('>') + 1, prodText.length());
            rhsText = rhsText.replace(' ', ',');
            rhsText += ",$";
            rhs.add(rhsText);
            //System.out.println(rhsText);

            //Add Context
            if (contextText.length() == 0) {
                context.add("-");
                sql = "INSERT INTO productions " + "VALUES ('" + lhsText + "','" + rhsText + "',"+null+")";
            } else {
                context.add(contextText);
                sql = "INSERT INTO productions " + "VALUES ('" + lhsText + "','" + rhsText + "','" + contextText + "')";
            }
            prodField.setText("");
            contextField.setText("");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection con = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/semanticParser", "root", "tushar@123");

                Statement stmt = (Statement) con.createStatement();

                stmt.executeUpdate(sql);
                //ResultSet rs = stmt.executeQuery("select * from productions");

                con.close();
            } catch (Exception e) {
                System.out.println(e);
            }

            errorField.setForeground(java.awt.Color.GREEN);
            errorField.setText("Production Added");
        } else {
            errorField.setForeground(java.awt.Color.red);
            errorField.setText("ERROR: Field is Empty");
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    //Parse String
    private void parseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parseButtonActionPerformed
        errorField.setText("");
        String str = parseField.getText();
        if (str.length() != 0) {
            //fillIndex();
            //printProductions();
            //createLRItems();
            //printLRItems();
            generateParsingTable();

            //printParsingTable();
            parseString(str);
            errorField.setForeground(java.awt.Color.GREEN);
            errorField.setText("Parsing is Successful");
        } else {
            errorField.setForeground(java.awt.Color.red);
            errorField.setText("ERROR: Parse Text is Empty");
        }
    }//GEN-LAST:event_parseButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField contextField;
    private javax.swing.JLabel errorField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField nonTerminalField;
    private javax.swing.JPanel pannel1;
    private javax.swing.JButton parseButton;
    private javax.swing.JTextField parseField;
    private javax.swing.JTextArea parseResult;
    private javax.swing.JTable parseTableField;
    private javax.swing.JTextField prodField;
    private javax.swing.JTable productionTable;
    private javax.swing.JTextField resultContextField;
    private javax.swing.JTextField terminalField;
    // End of variables declaration//GEN-END:variables
}
