package com.groupproject.shared.network;
/*
ĐÂY LÀ HÀM DÙNG CHUNG CHỊU TRÁCH NHIỆM XỬ LÝ TIỀN CỦA BIDDER 
Cách hoạt động như sau :
-Nó sẽ có 2 loại tiền là :  locked_balance, auctual_balance và available balance = actual-locked
- Khi mà bạn đang dẫn đầu một phiên đấu giá A với mức giá bạn đặt là x thì locked = x 
- Tiếp đến nếu bạn muốn tham gia phiên đấu giá B : 
    + kiểm tra available = actual -x > giá hiện tại của phiên đấu giá B hay không ? -->có, đặt được và locked+= y (y: số tiền bạn đặt trong phiên đấu giá B)
                                                                                    -->không, không được phép đặt 
    + nêu như phiên đấu giá A có người khác đặt giá cao hơn bạn thì có locked sẽ được reset về 0 và available của bạn lúc này = actual     
*/
public interface Wallet  {
    boolean hasWalletUpdated();
    double getAvailableBalance();
}
