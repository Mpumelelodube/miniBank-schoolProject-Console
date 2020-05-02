package controller;

import DatabaseConnection.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Controller {
    private int loggedAccount;
    private String loggedAccountId;

    public void mainMenu(){

        System.out.println("Select an option\n1. login\n2. Register");
        int selection = intCheck();

        if (selection < 3 && selection > 0){
            switch (selection){
                case 1:
                    login();
                    break;
                case 2:
                    createAccount();
                    break;
            }
        }else {
            System.out.println("INVALID SELECTION");
            mainMenu();
        }
    }

    public void dashBoard(){
        String sql = "select first_name,last_name from users where national_id = ?";

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1,this.loggedAccountId);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Welcome to Baclays Bank "+resultSet.getString(1)+" "+resultSet.getString(2)+" Account: "+this.loggedAccount);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        System.out.println("\nSELECT AN OPTION\n\n1. Deposit Money\n2. Withdraw Money\n3. Transfer Money" +
                "\n4. Check Balance\n5. Deactivate Account\n6. View Bank Statement\n7. Logout");

        int selection = intCheck();
        if (selection < 8 && selection > 0){
            switch (selection){
                case 1:
                    depositMoney();
                    break;
                case 2:
                    withdrawMoney();
                    break;
                case 3:
                    transferMoney();
                    break;
                case 4:
                    System.out.println("\nYour balance is: $"+checkBalance());
                    dashBoard();
                    break;
                case 5:
                    deactivateAccount();
                    break;
                case 6:
                    bankStatement();
                    break;
                case 7:
                    mainMenu();
            }
        }

        System.out.println("dashboard");
    }

    private void bankStatement() {
        String sql = "select transactions.amount, transaction_type.type, balance.account from transactions," +
                "transaction_type,balance where balance.reference_number == transactions.reference_number and " +
                "balance.type == transaction_type.id and balance. account = ?";
        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1, this.loggedAccount);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Amount       type   account");
            while (resultSet.next()){
                System.out.println(resultSet.getDouble(1)+" | "+resultSet.getString(2)+" | "+resultSet.getInt(3));
            }
            connection.close();
            dashBoard();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void depositMoney() {
        System.out.print("Enter amount to deposit: $");
        double amount = doubleCheck();

        String sql = "insert into transactions(date,amount)values(?,?)";

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1,getDate());
            preparedStatement.setDouble(2,amount);

            preparedStatement.execute();

            connection.close();

            insertBalance(1,this.loggedAccount);
            System.out.println("deposit successful new balance: $"+checkBalance());
            dashBoard();
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void insertBalance(int type, int account){
        int reference = 0;

        String sql;

        try {
            Connection connection = DatabaseConnection.getConnection();

            sql = "select reference_number from transactions";

            ResultSet resultSet = connection.createStatement().executeQuery(sql);


            while (resultSet.next()){
                reference = resultSet.getInt(1);
            }

            System.out.println("ref  "+reference+" logged "+account);

            sql = "insert into balance(reference_number,account,type)values(?,?,?)";

            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1,reference);
            preparedStatement.setInt(2,account);
            preparedStatement.setInt(3,type);

            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void withdrawMoney() {
        System.out.println("enter amount to Withdraw");
        double withdraw = doubleCheck();

        if (withdraw < checkBalance()-20){
            String sql = "insert into transactions(date,amount)values(?,?)";

            try {
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);

                preparedStatement.setString(1,getDate());
                preparedStatement.setDouble(2,withdraw);

                preparedStatement.execute();

                connection.close();

                insertBalance(2,this.loggedAccount);
                dashBoard();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void transferMoney() {
        System.out.println("Enter account number to transfer to");
        int account = intCheck();

        boolean transferStatus = false;

        String sql = "select national_id from accounts where account = ?";

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1, account);

            ResultSet resultSet = preparedStatement.executeQuery();

            String id = resultSet.getString(1);

            if (!id.equals(null)){
                sql = "select first_name,last_name from users where national_id = ?";

                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, id);

                resultSet = preparedStatement.executeQuery();

                System.out.println("Transfering money to "+resultSet.getString(1)+" "+resultSet.getString(2)+" Account number: "+account);

                System.out.println("enter amount to transfer");
                double transfer = doubleCheck();

                connection.close();

                if (transfer < checkBalance() - 20){
                    sql = "insert into transactions(date,amount)values(?,?)";

                    try {
                        connection = DatabaseConnection.getConnection();
                        preparedStatement = connection.prepareStatement(sql);

                        preparedStatement.setString(1,getDate());
                        preparedStatement.setDouble(2,transfer);

                        preparedStatement.execute();

                        connection.close();

                        insertBalance(1,account);
                        insertBalance(3,this.loggedAccount);

                        System.out.println("The transfer was successful new balance: $"+checkBalance());
                        dashBoard();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }else {
                    connection.close();
                    System.out.println("Your have insufficient funds to perform this transaction\nyour balance is $"+checkBalance());
                    dashBoard();
                }

            }else {
                System.out.println("The account you entered does not exist");
                connection.close();
                dashBoard();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double checkBalance() {
        String sql = "select transactions.amount,balance.type from transactions, balance where balance.account = ? and transactions.reference_number == balance.reference_number";
        List<Balance> balanceList = new ArrayList<>();

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1, this.loggedAccount);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                balanceList.add(new Balance(resultSet.getInt(1),resultSet.getInt(2)));
            }

            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        double balance = 0;

        for (Balance bal : balanceList ){
            if (bal.getType() == 1){
                balance += bal.getAmount();
                System.out.println(balance+"-------"+bal.getAmount());
            }
            if (bal.getType() == 2){
                balance -= bal.getAmount();
                System.out.println(balance+"-------"+bal.getAmount());
            }
            if (bal.getType() == 3){
                balance -= bal.getAmount();
            }
        }
        return balance;
    }

    private void deactivateAccount() {

        System.out.println("Are you sure you want to deactivate your account?\n1.YES\n2.NO");
        int selection = intCheck();
        if (selection > 0 && selection < 3){
            switch (selection){
                case 1:
                    System.out.println("your account has: $"+checkBalance()+"\n1. Transfer the money to an existing account\n2. withdraw the money");
                    selection = intCheck();
                    switch (selection){
                        case 1:
                            System.out.println("Enter account number to transfer to");
                            int account = intCheck();

                            String sql = "select national_id from accounts where account = ?";

                            try {
                                Connection connection = DatabaseConnection.getConnection();
                                PreparedStatement preparedStatement = connection.prepareStatement(sql);

                                preparedStatement.setInt(1, account);

                                ResultSet resultSet = preparedStatement.executeQuery();

                                String id = resultSet.getString(1);

                                if (!id.equals(null)){
                                    sql = "select first_name,last_name from users where national_id = ?";

                                    preparedStatement = connection.prepareStatement(sql);
                                    preparedStatement.setString(1, id);

                                    resultSet = preparedStatement.executeQuery();

                                    System.out.println("Transfering money to "+resultSet.getString(1)+" "+resultSet.getString(2)+" Account number: "+account);

                                    System.out.println("enter amount to transfer");
                                    double transfer = checkBalance();

                                    connection.close();

                                    sql = "insert into transactions(date,amount)values(?,?)";

                                    try {
                                        connection = DatabaseConnection.getConnection();
                                        preparedStatement = connection.prepareStatement(sql);

                                        preparedStatement.setString(1,getDate());
                                        preparedStatement.setDouble(2,transfer);

                                        preparedStatement.execute();

                                        connection.close();

                                        insertBalance(1,account);
                                        System.out.println("The transfer was successful new balance: $"+checkBalance());

                                        sql = "delete from accounts where account = ?";

                                        preparedStatement = connection.prepareStatement(sql);

                                        preparedStatement.setInt(1,this.loggedAccount);

                                        preparedStatement.execute();

                                        connection.close();

                                        System.out.println("Your account hs been successfully deactivated");
                                        mainMenu();

                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }else {
                                    System.out.println("The account you entered does not exist");
                                    connection.close();
                                    dashBoard();
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            break;
                        case 2:
                            System.out.println("Your Account balance is now $0.000");
                            sql = "delete from accounts where account = ?";

                            try {
                                Connection connection = DatabaseConnection.getConnection();
                                PreparedStatement preparedStatement = connection.prepareStatement(sql);

                                preparedStatement.setInt(1,this.loggedAccount);

                                preparedStatement.execute();

                                connection.close();

                                System.out.println("Your account hs been successfully deactivated");
                                mainMenu();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                    }

                    break;
                case 2:
                    dashBoard();
                    break;
            }
        }else {
            System.out.println("invalid selection");
            dashBoard();
        }
    }

    public void createAccount(){
        Scanner input = new Scanner(System.in);

        System.out.println("Enter First Name");
        String name = input.next();
        System.out.println("Enter Last Name");
        String lastName = input.next();
        System.out.println("Enter D.O.B dd/mm/yyyy");
        String dob = input.next();
        System.out.println("National ID");
        String national_id = input.next();
        System.out.println("enter Address");
        String address = input.next();

        String sql = "insert into users(national_id,first_name,last_name,dob,address)values(?,?,?,?,?)";

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1,national_id);
            preparedStatement.setString(2,name);
            preparedStatement.setString(3,lastName);
            preparedStatement.setString(4,dob);
            preparedStatement.setString(5,address);

            preparedStatement.execute();

            sql = "select account from accounts";

            ResultSet resultSet = connection.createStatement().executeQuery(sql);

            int acc = 0;
            while (resultSet.next()){
                acc = resultSet.getInt(1);
            }

            System.out.println("You have successfully created your baclays account\nName: "+name+"\nSurname: "+lastName+
                    "\nNational Id: "+national_id+"\nAddress: "+address+"\nYour account number: "+(acc+1)+"\n\nplease enter a 4 digit pin\n");

            int pin = intCheck();

            sql = "insert into accounts(national_id,pin)values(?,?)";

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,national_id);
            preparedStatement.setInt(2,pin);

            preparedStatement.execute();
            connection.close();

            System.out.println("Login with your account number ans pin");
            mainMenu();

        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void login(){
        Scanner input = new Scanner(System.in);

        System.out.println("Enter Account Number");
        int account = intCheck();
        System.out.println("Enter Pin");
        int pin = intCheck();

        String sql = "select account,pin,national_id from accounts where account = ? and pin = ?";

        try {
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setInt(1, account);
            preparedStatement.setInt(2, pin);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                System.out.println(resultSet.getInt(1)+"----"+resultSet.getInt(2)+"-----"+resultSet.getString(3));
                this.loggedAccount = resultSet.getInt(1);
                this.loggedAccountId = resultSet.getString(3);
                connection.close();
                dashBoard();
            }

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("wrong credentials");
        mainMenu();
    }

    public double doubleCheck(){
        Scanner input = new Scanner(System.in);
        try {
            double num = input.nextInt();
            return num;
        }catch (InputMismatchException e){
            System.out.println("wrong input format, expected integer format please re-enter");
            return doubleCheck();
        }
    }

    public int intCheck(){
        Scanner input = new Scanner(System.in);
        try {
            int num = input.nextInt();
            return num;
        }catch (InputMismatchException e){
            System.out.println("wrong input format, expected integer format please re-enter");
            return intCheck();
        }
    }

    private String getDate() {

        LocalDateTime localDateTime = LocalDateTime.now();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM-dd-YYYY hh:mm a");

        return dtf.format(localDateTime);
    }

}
