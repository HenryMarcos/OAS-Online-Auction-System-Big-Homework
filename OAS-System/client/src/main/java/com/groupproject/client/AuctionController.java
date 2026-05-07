package com.groupproject.client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.ResourceBundle;

import com.groupproject.client.Utlis.*;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
public class AuctionController implements Initializable {
    // tao ra mot objet auction session
    private final AuctionSession session = new AuctionSession();
    private CountdownHelper countdownHelper= new CountdownHelper();
    private ItemViewModel viewModel;
    private Item item;
    private BidHistory bidhistory;
    private static final DateTimeFormatter TIME_FMT =  DateTimeFormatter.ofPattern("HH:mm:ss");
    // line chart nay se them cac diem o priceseries vao 
    // priceAxis lam nhiem vu can chinh
    @FXML private Button placebid;
    @FXML private LineChart linechart;
    @FXML private XYChart.Series<String,Number> priceseries;

    private double currentStimulatedPrice=100.0;
    @FXML private TableView<BidHistory> bottomtable;
    @FXML private TableColumn<BidHistory, String> usercol;
    @FXML private TableColumn<BidHistory, Double> pricecol;
    @FXML private TableColumn<BidHistory, String> timecol;
    private ObservableList<BidHistory> historyList;


    @FXML
    private TextField enterprice;
    @FXML 
    private Label description;
    @FXML 
    private ImageView productImageView;
    @FXML
    private Label auctiontimeleft;
    @FXML
    private Label auctioncurrentprice;
    @FXML
    private Label participant;
    @FXML
    private Label auctionproductname;
    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/mainscreen.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage =  (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
    }
    // dong bo cac diem tren do thi
    @Override 
    public void initialize(URL location, ResourceBundle resources) {
        priceseries = new XYChart.Series<>();
        linechart.getData().add(priceseries);
        //listenPriceHistory();
        usercol.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("price"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("time"));
        historyList = FXCollections.observableArrayList();
        bottomtable.setItems(historyList);
        // khoi dong chay 
        
        startMockSeverData();
    }

    public void setItemViewModel(ItemViewModel viewModel) {
        this.viewModel = viewModel;
        this.item= viewModel.getItem();
        countdownHelper.start(item,auctiontimeleft);
        auctionproductname.setText("Name : " + item.getName());
        description.setText(String.format("Description : " + item.getDescription()));
        auctioncurrentprice.textProperty().bind(Bindings.format("Current price : %,.2f USD", viewModel.currentPriceProperty()));
        if (item.getPriceHistory().isEmpty() ) {
            String now= LocalTime.now().format(TIME_FMT);
            AuctionSession.PricePoint p = new AuctionSession.PricePoint(now,item.getStartingPrice());
            //session.getPriceHistory().add(p);
            //item.getPriceHistory().add(p);
        }
        else {
            //session.getPriceHistory().setAll(item.getPriceHistory());
        }

        //priceseries.getData().clear();
        for (AuctionSession.PricePoint p : session.getPriceHistory()) {
           // priceseries.getData().add(createDataPoint(p));
        }
    }
    // xu ly ngoai le khi co truong hop : khong ghi gi, ghi ca chu va so ,...
    @FXML
    private void handlePlaceBid() {
        /*
           //handle phần giao diện khi việc đặt giá đang được gủi lên sever
            //B1 : vô hiệu hóa nút place bid để tránh người dùng bấm nhiều lần 
            placebid.setDisable(true);
            placebid.setText("Đang gửi yêu cầu : ...");
        
        */
        try {
            double newBid= Double.parseDouble(enterprice.getText());
            if (newBid <= item.getCurrentPrice()) {
                return;
            }
        // them diem moi neu gia hien tai la hop le
            String now = LocalTime.now().format(TIME_FMT);
            viewModel.updatePrice(newBid);
            //session.getPriceHistory().add(new AuctionSession.PricePoint(now,newBid));
            //item.getPriceHistory().add(new AuctionSession.PricePoint(now,newBid));
        // handle exception có khả năng xảy ra : không ghi gì, bao gồm số và các ký tự khác 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // tao ra mot diem moi. 
    private  XYChart.Data<String,Number> createDataPoint(AuctionSession.PricePoint p) {
        XYChart.Data<String,Number> datapoint= new XYChart.Data<>(p.getLabel(),p.getPrice());
        return datapoint;

    }
    // ham gia lap mot chuong trinh dau gia gom nhieu nguoi dat gia 
    private void startMockSeverData()  {
        Thread mockThread = new Thread(()->{
            Random random = new Random();
            String[] mockUsers= {"naruto","sasuke", "luffy", "zoro", "goku"};
            try {
                while (true) { 
                    Thread.sleep(1000+ random.nextInt(2000));
                    currentStimulatedPrice += (1+ random.nextInt(5));
                    String randomBidder = mockUsers[random.nextInt(mockUsers.length)];
                    String currenttime= new SimpleDateFormat("HH:mm:ss").format(new Date());
                    Platform.runLater(()->{
                        updateUI(randomBidder,currentStimulatedPrice,currenttime);
                    });
                }
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }

        });
        mockThread.setDaemon(true);
        mockThread.start();
    }
    private void updateUI(String bidder, double price, String time) {
        viewModel.updatePrice(price);
        historyList.add(0, new BidHistory(bidder, price, time));
        priceseries.getData().add(new XYChart.Data<>(time,price));
        if (priceseries.getData().size() > 15) {
            priceseries.getData().remove(0);
        }
    }
}


