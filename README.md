Hệ Thống Đấu Giá Trực Tuyến (Online Auction System - OAS)

Một nền tảng đấu giá trực tuyến theo thời gian thực (real-time) toàn diện theo mô hình Client-Server, được xây dựng bằng Java, JavaFX và Maven. Hệ thống này cho phép người dùng lên lịch đấu giá, tham gia đấu giá trực tiếp, quản lý danh mục sản phẩm và theo dõi lịch sử biến động giá một cách trực quan.

Tính Năng Nổi Bật:

-Bidding Engine thời gian thực: Đồng bộ hóa dữ liệu phòng đấu giá đồng thời (concurrent rooms) với độ trễ cực thấp nhờ hệ thống phát sự kiện EventBus tập trung ở phía Server.

-Giao diện trực quan (UI/UX): Ứng dụng desktop JavaFX được thiết kế hiện đại với hệ thống CSS tùy biến, hỗ trợ co giãn linh hoạt (TableView) và đồng hồ đếm ngược thời gian thực.

-Biểu đồ biến động giá trực tiếp: Tích hợp LineChart ngay trong phòng đấu giá để cập nhật và hiển thị trực quan các lượt trả giá của người dùng theo thời gian thực.

-Tối ưu hóa quản lý vòng đời ứng dụng: Quản lý và lưu trữ bộ nhớ cache phía client thông qua một SessionManager hợp nhất, giúp đồng bộ trạng thái giao diện và ngăn chặn các yêu cầu gửi lên mạng (network calls) trùng lặp hoặc lặp vô hạn.

-Vòng đời đấu giá đa trạng thái: Tự động chuyển đổi mượt mà giữa các trạng thái WAITING $\rightarrow$ SCHEDULED $\rightarrow$ ACTIVATED $\rightarrow$ FINISHED / CANCELLED nhờ các luồng background quét và cập nhật cơ sở dữ liệu liên tục ở backend.

Công Nghệ Sử Dụng & Yêu Cầu Hệ Thống

-Ngôn ngữ: Java (Khuyến nghị JDK 17 trở lên)

-UI Framework: JavaFX (kết hợp FXML)

-Build System: Maven

-Cơ sở dữ liệu: SQL thông qua tầng JDBC Persistence layer

*Trước khi biên dịch, hãy đảm bảo bạn đã cài đặt Java Development Kit (JDK), Maven và cấu hình biến môi trường (Environment Variables) đầy đủ trên máy tính.

🏗️ Hướng Dẫn Cài Đặt & Khởi ChạyĐầu tiên, clone repository này về máy cục bộ của bạn:
git clone https://github.com/YOUR_USERNAME/OAS-Online-Auction-System-Big-Homework.git
cd OAS-Online-Auction-System-Big-Homework/OAS-System

🖥️ 1. Cấu Hình & Chạy Backend ServerThực hiện các bước sau để biên dịch và khởi chạy server quản lý đấu giá.Biên dịch ServerSử dụng Maven để dọn dẹp workspace và đóng gói mã nguồn thành file JAR độc lập (fat JAR).
mvn clean install

Khởi chạy ServerChạy file JAR hoặc target đã biên dịch để kích hoạt backend server.
java -jar server-1.0-SNAPSHOT.jar

💻 2. Cấu Hình & Chạy Client App
Thực hiện các bước sau để biên dịch và khởi chạy ứng dụng giao diện JavaFX trên máy tính.
Biên dịch Client
Biên dịch và đóng gói module desktop client cùng các thư viện phụ thuộc đi kèm.
Trước tiên tạo file client.properties trong client/src/main/resources

bao gồm:
SERVER_IP=your_server_ip (server ip của bạn)
SERVER_PORT=your_port (port của server)

Để tạo file exe chạy luôn:
jpackage --type app-image --name OAS-App --runtime-image target/image --module com.groupproject.client/com.groupproject.client.App --icon logo.ico
(Bỏ --icon logo.ico nếu không có file ảnh tên logo.ico)

Để tạo installer exe:
jpackage --type exe --name OAS-App-Installer --runtime-image target/image --module com.groupproject.client/com.groupproject.client.App --icon logo.ico --win-shortcut --win-menu --win-dir-chooser

-Nếu có thỏa thuận người dùng(eula.rtf):
jpackage --type exe --name OAS-App-Installer --runtime-image target/image --module com.groupproject.client/com.groupproject.client.App --icon logo.ico --win-shortcut --win-menu --win-dir-chooser --license-file eula.rtf

Khởi chạy Client
Kích hoạt chạy ứng dụng để mở màn hình đăng nhập/chính (gọi trực tiếp qua MainController).

cd client
mvn javafx:run

🌐 Triển Khai Production (GCP VM qua systemd)
Để chạy backend server liên tục 24/7 trên một máy ảo Linux của Google Cloud (GCP VM), hệ thống có thể được cấu hình như một dịch vụ chạy ngầm thông qua tệp cấu hình systemd đặt tại (/etc/systemd/system/myserver.service):
Ini, TOML
[Unit]
Description=My Group Project Java Server
After=network.target

[Service]
Type=simple
User=thangtranmanh9
WorkingDirectory=/home/thangtranmanh9
ExecStart=/usr/bin/java -jar /home/thangtranmanh9/server-1.0-SNAPSHOT.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target

Các lệnh quản lý tiến trình chạy ngầm:Bash# Tải lại cấu hình hệ thống sau khi sửa file dịch vụ
sudo systemctl daemon-reload

# Khởi chạy dịch vụ server chạy full-time
sudo systemctl start myserver.service

# Xem log hệ thống và console output của ứng dụng theo thời gian thực
sudo journalctl -u myserver.service -f
📄 Giấy Phép (License)Dự án này được cấp phép theo các điều khoản của MIT License - xem file LICENSE để biết thêm chi tiết.
