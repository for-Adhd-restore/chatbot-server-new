package com.forA.chatbot.subscriptions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "apple-appstore", url = "https://api.storekit.itunes.apple.com")
public interface AppleAppStoreClient {

  /**
   *  거래 / 구독 상태 조회
   * Get Transaction Info: 단일 거래 상세 조회
   * https://developer.apple.com/documentation/appstoreserverapi/get-transaction-info
   * */
  @GetMapping("/inApps/v1/transactions/{transactionId}")
  String getTransactionInfo(
      @RequestHeader("Authorization") String appleJwt,
      @PathVariable("transactionId") String transactionId
  );
}
