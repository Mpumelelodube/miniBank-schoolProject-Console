package App;

import DatabaseConnection.DatabaseConnection;
import controller.Controller;

import java.sql.SQLException;

public class App {
    public static void main(String[] args) {
        Controller controller = new Controller();

        controller.mainMenu();
    }
}
