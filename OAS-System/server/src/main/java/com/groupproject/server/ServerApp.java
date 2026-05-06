// Class chính để setup server và các thứ các thứ

package com.groupproject.server;



import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServerApp {

    /* 
    private static Scene scene;

    @Override
    public void start(Stage stage) throws Exception {
        scene = new Scene(loadFXML("server"), 640, 480);

        stage.setTitle("OAS-App Server Console");
        stage.setScene(scene);

        // Đảm bảo luồng server nền sẽ dừng lại khi nhấn nút x để đóng cửa sổ
        stage.setOnCloseRequest(event -> System.exit(0));

        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Load file fxml
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApp.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    */

    // Danh sách tổng hợp tất cả các đường dẫn output của các máy client được kết nối
    public static final List<ObjectOutputStream> clientWriters = new ArrayList<>();

    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        // Khởi tạo database người dùng
        initDatabse();

        // launch(args);
        int port = 8080; // Cổng mà server sẽ lắng nghe

        // ServerSocket chính là thứ lắng nghe lưu lượng truy cập internet
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Thông báo để kiểm tra xem server có chạy được hay không
            System.out.println("Server is online and listening on port " + port + "...");

            // Vòng lặp vô hạn để server tồn tại mãi mãi đợi clients
            while (true) {
                // Đợi đến khi có 1 client kết nối tới server
                // Code sẽ dừng ở dòng này và chỉ chạy tiếp khi có client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());

                // Đưa client đến một luồng mới để server không bị đơ
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            // Dừng vòng lặp lại tại đây
            log("Server Error: " + e.getMessage());
        }
    }

    public static void initDatabse() {
        try (Connection conn = DriverManager.getConnection(DB_URL); 
             Statement stmt = conn.createStatement()) {

            // Tạo bảng users nếu chưa tồn tại 
            String sql = "CREATE TABLE IF NOT EXISTS users (" + 
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL)";
            stmt.execute(sql);
            
            String auctionSql = "CREATE TABLE IF NOT EXISTS auctions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "item_name TEXT NOT NULL," +
                                "starting_price REAL NOT NULL," +
                                "auction_duration LONG NOT NULL," +
                                "current_bid REAL NOT NULL," +
                                "highest_bidder TEXT," +
                                "end_time DATETIME NOT NULL," + 
                                "is_active BOOLEAN DEFAULT 1)";
            stmt.execute(auctionSql);

            // Xóa trước khi tạo bảng để test(sau này sẽ không dùng)
            stmt.execute("DROP TABLE IF EXISTS category_fields");
            stmt.execute("DROP TABLE IF EXISTS categories");

            // Tạo bảng danh sách các danh mục hàng
            String categoriesSql = "CREATE TABLE IF NOT EXISTS categories (" +
                                   "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                   "name TEXT NOT NULL, " +
                                   "parent_id INTEGER, " +
                                   "FOREIGN KEY(parent_id) REFERENCES categories(id))";
            stmt.execute(categoriesSql);

            // Tạo bảng chứa các yêu cầu trong danh mục hàng
            String fieldsSql = "CREATE TABLE IF NOT EXISTS category_fields (" +
                               "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                               "category_id INTEGER NOT NULL, " +
                               "field_name TEXT NOT NULL, " +
                               "FOREIGN KEY(category_id) REFERENCES categories(id))";
            stmt.execute(fieldsSql);

            String answersSql = "CREATE TABLE IF NOT EXISTS item_attributes (" +
                                "auction_id INTEGER NOT NULL, " +
                                "field_name TEXT NOT NULL, " +
                                "field_value TEXT NOT NULL, " + // e.g., "Apple", "XL", "Red"
                                "FOREIGN KEY(auction_id) REFERENCES auctions(id))";
            stmt.execute(answersSql);

            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (1, 'Electronics', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (11, 'Laptops', 1);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Brand');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'Model');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (11, 'RAM (GB)');");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (12, 'Smartphones', 1);");

            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (2, 'Clothing', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (21, 'Mens Shoes', 2);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (22, 'Womens Shirts', 2);");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Size');");
            stmt.execute("INSERT OR IGNORE INTO category_fields (category_id, field_name) VALUES (22, 'Color');");


            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (3, 'Home & Garden', NULL);");
            stmt.execute("INSERT OR IGNORE INTO categories (id, name, parent_id) VALUES (31, 'Furniture', 3);");

            

            
            CategoryManager.getCategories();
            
            System.out.println("Database initialized successfully!");
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // --- HÀM BÁO TIN/THÔNG BÁO ---
    // Gửi 1 tin nhắn cho mỗi client trong danh sách
    public static void broadcast(String message, ObjectOutputStream senderOut) {
        synchronized (clientWriters) {
            for (ObjectOutputStream writer : clientWriters) {
                if (writer != senderOut) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (Exception e) {
                        // Nếu có lỗi thì bỏ qua
                        // Hàm catch sẽ xử ly vẫn đề chết kết nối
                    }
                }
            }
        }
    }

    

}
