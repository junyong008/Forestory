package com.yjy.forestory.feature.purchase

import EventObserver
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityPurchaseBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject


@AndroidEntryPoint
class PurchaseActivity: BaseActivity<ActivityPurchaseBinding>(R.layout.activity_purchase),
    PurchasesUpdatedListener {

    private val purchaseViewModel: PurchaseViewModel by viewModels()
    private lateinit var billingClient: BillingClient
    private var productDetailsList: List<ProductDetails> = listOf()

    override fun initViewModel() {
        binding.purchaseViewModel = purchaseViewModel
    }

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        setupRewardAd()
        setupBillingClient()
    }

    // 애드몹 설정
    private fun setupRewardAd() {
        if (purchaseViewModel.rewardAd.value == null) {
            RewardedAd.load(this, "ca-app-pub-3155565379106661/9963292648", AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {

                    override fun onAdFailedToLoad(adError: LoadAdError) { purchaseViewModel.emptyRewardAd() }

                    override fun onAdLoaded(ad: RewardedAd) {

                        purchaseViewModel.apply {
                            initRewardAd(ad)
                            rewardAd.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() { emptyRewardAd() }
                                override fun onAdFailedToShowFullScreenContent(p0: AdError) { emptyRewardAd() }
                                override fun onAdShowedFullScreenContent() { emptyRewardAd() }
                            }
                        }
                    }
                })
        }
    }

    // 리워드 애드 보여주기
    private fun showRewardedAd() {

        purchaseViewModel.rewardAd.value?.let {
            it.show(this) { rewardItem ->
                purchaseViewModel.addTicket(rewardItem.amount)
            }
        }
    }

    // billingClient 최초 설정 : 구글 결제 서비스와 연동하고 인앱 결제 항목들을 가져온다
    private fun setupBillingClient() {

        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        // 구글 결제 서비스에 연결
        billingClient.startConnection(object : BillingClientStateListener {

            // 결제 서비스와 연결이 끊어진 경우
            override fun onBillingServiceDisconnected() {
                showToast(getString(R.string.fail_to_connect_payment), R.style.errorToast)
            }

            // 결제 서비스 연결 성공시
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                    // billing client가 준비가 되었다면 구매 가능한 항목들을 서버로부터 받아온다
                    queryProductDetails()

                    // 이전에 구매했는데 Configuration Change, 강제 종료 상황 등으로 미결산된 결제 처리
                    checkUnconsumedPurchases()
                }
            }
        })
    }

    // 구매 가능한 항목들을 받아오는 함수
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId("tickets_2").setProductType(BillingClient.ProductType.INAPP).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tickets_10").setProductType(BillingClient.ProductType.INAPP).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tickets_20").setProductType(BillingClient.ProductType.INAPP).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tickets_60").setProductType(BillingClient.ProductType.INAPP).build(),
            QueryProductDetailsParams.Product.newBuilder().setProductId("tickets_100").setProductType(BillingClient.ProductType.INAPP).build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                this.productDetailsList = productDetailsList
                initPrices()
            }
        }
    }

    // 각 아이템의 가격을 받아와 UI를 초기화 한다
    private fun initPrices() {

        val buttonMap = mapOf(
            "tickets_2" to binding.buttonItem1,
            "tickets_10" to binding.buttonItem2,
            "tickets_20" to binding.buttonItem3,
            "tickets_60" to binding.buttonItem4,
            "tickets_100" to binding.buttonItem5
        )

        for (product in productDetailsList) {
            val price = product.oneTimePurchaseOfferDetails?.formattedPrice

            buttonMap[product.productId]?.text = price
        }
    }

    // 이전에 구매했는데 Configuration Change, 강제 종료 상황 등으로 미결산된 결제 처리
    private fun checkUnconsumedPurchases() {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {

                        lifecycleScope.launch {

                            if (!purchase.isAcknowledged) {

                                // 승인되지 않은 거래가 있으면 거래 마저 처리
                                handlePurchase(purchase)
                            } else if (!purchaseViewModel.getTicketNeedToConsume().isNullOrEmpty()) {

                                // 승인은 됐으나 소비 처리 되지 않은 거래가 있으면 마저 소비
                                val productId = JSONObject(purchase.originalJson).getString("productId")
                                consumeProduct(productId)
                            }
                        }
                    }
                }
            }
        }
    }

    // productId 로 항목 소비처리
    private fun consumeProduct(productId: String) {

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchasesList) {
                    if (JSONObject(purchase.originalJson).getString("productId").contains(productId)) {
                        consumePurchase(purchase)
                        break
                    }
                }
            }
        }
    }


    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 아이템 구매 : 하드코딩 - 리사이클러뷰로 구현하기엔 항목이 고정되고 추후 큰 변동이 없을거라 판단하여 하드코딩으로 구현
        binding.buttonItem0.setOnClickListener { showRewardedAd() }
        binding.buttonItem1.setOnClickListener { purchase("tickets_2")  }
        binding.buttonItem2.setOnClickListener { purchase("tickets_10") }
        binding.buttonItem3.setOnClickListener { purchase("tickets_20") }
        binding.buttonItem4.setOnClickListener { purchase("tickets_60") }
        binding.buttonItem5.setOnClickListener { purchase("tickets_100") }
    }


    // 아이템 구매
    private fun purchase(productId: String) {
        // 구매하려고 하는 제품의 정보를 가져와 구매를 진행한다
        productDetailsList.find { it.productId == productId }?.let {
            val purchaseParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(it)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(purchaseParams))
                .build()

            billingClient.launchBillingFlow(this, flowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        // 구매가 정상적으로 이루어졌다면
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            // 구매 정보를 하나씩 처리한다.
            lifecycleScope.launch {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        } else {
            showToast(getString(R.string.fail_to_purchase), R.style.errorToast)
        }
    }

    // 구매 정보를 처리하는 함수
    private suspend fun handlePurchase(purchase: Purchase) {

        // 정상적으로 구매가 완료되지 않은 경우
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            showToast(getString(R.string.fail_to_purchase), R.style.errorToast)
            return
        }

        // 구매 항목이 이미 승인된 경우는 처리하지 않음
        if (purchase.isAcknowledged) {
            return
        }

        // 항목을 승인하기 전에 이 거래는 반드시 승인 돼야 함을 로컬에 저장. 항목을 승인하기 전에 하는 이유는 승인 직후 강제 종료 되는 경우 대처 불가
        // 이 부분이 도달하지 않더라도 승인되지 않은 거래는 추후 isAcknowledged로 검출 가능, 고로 승인 이후 강제 종료로 인한 미소비 아이템을 잡기 위함
        purchaseViewModel.setTicketNeedToConsume(JSONObject(purchase.originalJson).getString("productId"))

        // 구매 항목 승인
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->

            // 승인 결과 확인
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                showToast(getString(R.string.fail_to_purchase), R.style.errorToast)
                return@acknowledgePurchase
            }

            // 항목을 소비해 재구매가 가능하도록 설정 및 재화 지급
            consumePurchase(purchase)
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(consumeParams) { consumeBillingResult, _ ->
            if (consumeBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                // 소비 처리가 완료되면 로컬에 티켓 넣어주기
                when(JSONObject(purchase.originalJson).getString("productId")) {
                    "tickets_2" -> { purchaseViewModel.addTicket(2) }
                    "tickets_10" -> { purchaseViewModel.addTicket(10) }
                    "tickets_20" -> { purchaseViewModel.addTicket(20) }
                    "tickets_60" -> { purchaseViewModel.addTicket(60) }
                    "tickets_100" -> { purchaseViewModel.addTicket(100) }
                }
            }
        }
    }

    override fun setObserver() {

        // 리워드 애드 준비 상태에 따라서 버튼 활성화 유무 설정
        purchaseViewModel.rewardAd.observe(this) {

            binding.buttonItem0.apply {
                isEnabled = (it != null)
                text = if (it != null) { getString(R.string.view_ads) } else { getString(R.string.preparing) }
            }
        }
    }

    override fun setEventObserver() {

        // 태그 구매후 추가가 완료됐음을 알림
        purchaseViewModel.isCompleteAdd.observe(this, EventObserver { count ->
            showToast(getString(R.string.purchase_complete, count), R.style.successToast)
        })
    }

    // 반드시 연결 해제가 필요하다. 아니면 계속 billingClient가 중첩돼서 구매가 정상적으로 이뤄지지 않을 수 있음
    override fun onDestroy() {
        super.onDestroy()
        billingClient.endConnection()
    }
}