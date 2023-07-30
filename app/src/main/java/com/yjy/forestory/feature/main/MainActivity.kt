package com.yjy.forestory.feature.main

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.github.logansdk.permission.PermissionManager
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yjy.forestory.BuildConfig
import com.yjy.forestory.Const.FREE_TICKET_COUNT
import com.yjy.forestory.Const.PRIVACY_POLICY_URL
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityMainBinding
import com.yjy.forestory.feature.addPost.AddPostActivity
import com.yjy.forestory.feature.backup.BackupActivity
import com.yjy.forestory.feature.purchase.PurchaseActivity
import com.yjy.forestory.feature.screenLock.ScreenLockSettingActivity
import com.yjy.forestory.feature.searchPost.SearchActivity
import com.yjy.forestory.feature.setting.*
import com.yjy.forestory.feature.userProfile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity: BaseActivity<ActivityMainBinding>(R.layout.activity_main),
    RecyclerViewScrollListener {

    private val mainViewModel: MainViewModel by viewModels()
    private var doubleBackToExitPressedOnce = false

    override fun initViewModel() {
        binding.mainViewModel = mainViewModel
        binding.includedLayout.mainViewModel = mainViewModel
    }

    // 뒤로가기 정의
    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            // 햄버거 메뉴가 열려있으면 닫고 반환
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                return
            }

            // 연속으로 두번 호출돼야 어플 종료
            if (doubleBackToExitPressedOnce) {
                finish()
            }

            doubleBackToExitPressedOnce = true
            Snackbar.make(binding.root, getString(R.string.press_twice_to_exit), Snackbar.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        lifecycleScope.launch {

            // 설정값 받아와서 UI 초기화
            initApplicationSettings()

            // 티켓 갯수 초기화
            initTickets()

            // 만약 백업/복원 진행중이라면 백업 액티비티로 강제 이동
            if (mainViewModel.getIsBackupOrRestoreInProgress()) {
                val intent = Intent(this@MainActivity, BackupActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        }

        // viewPager 설정
        val viewPager: ViewPager2 = binding.viewPager
        viewPager.isUserInputEnabled = false // Swipe로 이동하는거 막기
        viewPager.adapter = MainViewPagerAdapter(this)

        // tabLayout 설정
        val tabLayout: TabLayout = binding.tabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.icon =
                    AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_postlist)
                1 -> tab.icon =
                    AppCompatResources.getDrawable(this@MainActivity, R.drawable.ic_postlist_grid)
            }
        }.attach()
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // 버전 명 설정
        binding.includedLayout.textViewVersion.text = BuildConfig.VERSION_NAME
    }

    private suspend fun initApplicationSettings() {
        val currentTheme = mainViewModel.getCurrentTheme()
        if (currentTheme != null) {
            AppCompatDelegate.setDefaultNightMode(currentTheme)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            mainViewModel.setTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        val currentLanguage = mainViewModel.getCurrentLanguage()
        if (currentLanguage != null) {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(currentLanguage)
            AppCompatDelegate.setApplicationLocales(appLocale)
        } else {
            val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            mainViewModel.setLanguage(currentLocale)
        }

        val currentIsNotificationOn = mainViewModel.getCurrentIsNotificationOn()
        if (currentIsNotificationOn == null) {

            // 알림 설정이 null이고 버전이 티라미수 아래면 바로 초기 알람을 ON 하고, 위라면 권한 요청을 묻고 ON OFF를 정한다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkNotificationPermission(false)
            } else {
                mainViewModel.setIsNotificationOn(true)
                binding.includedLayout.switchNotification.isChecked = true
            }
        } else {
            binding.includedLayout.switchNotification.isChecked = currentIsNotificationOn
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission(isShowDialog: Boolean) {

        val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        PermissionManager.with(this, permissions).check { granted, _, _ ->

            if (granted.size == permissions.size) {
                mainViewModel.setIsNotificationOn(true)
                binding.includedLayout.switchNotification.isChecked = true
            } else if (isShowDialog) {
                mainViewModel.setIsNotificationOn(false)
                binding.includedLayout.switchNotification.isChecked = false
                showNotificationSettingDialog(this)
            } else {
                mainViewModel.setIsNotificationOn(false)
                binding.includedLayout.switchNotification.isChecked = false
                showToast(getString(R.string.notification_permission_denied), R.style.errorToast)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotificationSettingDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setMessage(getString(R.string.require_notification_permission))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.setting)) { dialog, _ ->

                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)

                dialog.dismiss()
            }
            .show()
    }

    private suspend fun initTickets() {

        if (mainViewModel.getCurrentTicket() == null) {
            mainViewModel.setTicket(0)
        }
        if (mainViewModel.getCurrentFreeTicket() == null) {
            mainViewModel.setFreeTicket(FREE_TICKET_COUNT)
        }
    }


    override fun setListener() {

        // 게시글 추가 버튼 클릭 리스너
        binding.ibuttonAddPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
        }

        // 맨 처음 게시글 추가 버튼 클릭 리스너
        binding.buttonStartPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
        }

        // 검색 버튼 클릭 리스너
        binding.ibuttonSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }

        // 햄버거 메뉴 버튼 클릭 리스너
        binding.ibuttonMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 햄버거 메뉴 리스너
        binding.includedLayout.apply {

            // 유저 프로필 편집 버튼 클릭
            ibuttonEditUserInfo.setOnClickListener {
                startSettingActivity(UserProfileActivity::class.java)
            }

            // 티켓 충전 버튼 클릭
            buttonCharge.setOnClickListener {
                startSettingActivity(PurchaseActivity::class.java)
            }

            // 댓글 알림 ON OFF 감지
            switchNotification.setOnCheckedChangeListener  { switch, isChecked  ->

                if (isChecked) {

                    // 권한이 없다면 권한 요청
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted()) {
                        switch.isChecked = false
                        checkNotificationPermission(true)

                        return@setOnCheckedChangeListener
                    }

                    mainViewModel!!.setIsNotificationOn(true)
                } else {
                    mainViewModel!!.setIsNotificationOn(false)
                }
            }

            // 테마 설정 클릭
            menuTheme.setOnClickListener {
                startSettingActivity(ThemeSettingActivity::class.java)
            }

            // 언어 설정 클릭
            menuLanguage.setOnClickListener {
                startSettingActivity(LanguageSettingActivity::class.java)
            }

            // 화면 잠금 클릭
            menuScreenLock.setOnClickListener {
                startSettingActivity(ScreenLockSettingActivity::class.java)
            }

            // 데이터 백업/복원 클릭
            menuBackUp.setOnClickListener {
                startSettingActivity(BackupActivity::class.java)
            }

            // 개인정보처리방침 클릭
            menuPrivacyPolicy.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                startActivity(browserIntent)
            }

            // 오픈소스 라이선스 클릭
            menuOpenSource.setOnClickListener {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.setting_open_source))
                startSettingActivity(OssLicensesMenuActivity::class.java)
            }
        }
    }

    private fun startSettingActivity(targetActivityClass: Class<*>) {
        val intent = Intent(this@MainActivity, targetActivityClass)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.stay)
    }

    override fun setObserver() {

        // 게시글이 존재하지 않으면 안내 메시지를 띄운다
        mainViewModel.postCount.observe(this) {

            binding.tabLayout.visibility = if (it > 0) { View.VISIBLE } else { View.INVISIBLE }
            binding.ibuttonAddPost.isVisible = (it > 0)
            binding.linearLayoutNoPost.isVisible = (it <= 0)
        }
    }



    // Fragment 안 리사이클러뷰의 스크롤 변경 이벤트를 받아와서 메인 뷰의 배너를 변경한다.

    // 애니메이션에 필요한 변수 초기화
    private var isAnimating = false // 애니메이션이 보여지는 중인지
    private var isFolded = false // 배너가 축소된 상황인지
    private val animationDuration: Long = 500 // 애니메이션 지속 시간

    private val bannerStartHeight by lazy { dpToPx(170f) }
    private val bannerTargetHeight by lazy { dpToPx(69f) }
    private val tabLayoutStartHeight by lazy { dpToPx(50f) }
    private val tabLayoutTargetHeight by lazy { dpToPx(35f) }
    private val tabLayoutTargetWidth by lazy { dpToPx(150f) }
    private val tabLayoutStartMarginTop by lazy { dpToPx(-25f) }
    private val tabLayoutTargetMarginTop by lazy { dpToPx(10f) }
    private val tabLayoutStartMarginEnd by lazy { dpToPx(48f) }
    private val tabLayoutTargetMarginEnd by lazy { dpToPx(10f) }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onScrollChanged(scrollY: Int) {
        if (isAnimating) return

        /*// 스크롤이 최상단이라면 배너를 보이고, 아니라면 접는다
        if (scrollY == 0 && isFolded) {
            unFoldAnimation()
            isFolded = false
        }*/

        // 사용성 증대를 위해 처음 접속시 말고는 인사 메시지를 계속 보여줄 필요는 없다고 판단, 한번 스크롤을 내리면 더이상 펼치지 않음
        if (scrollY != 0 && !isFolded) {
            foldAnimation()
            isFolded = true
        }
    }

    private fun unFoldAnimation() {
        val bannerWidthAnimator = ValueAnimator.ofInt(binding.viewBanner.height, bannerStartHeight)
        val tabLayoutHeightAnimator = ValueAnimator.ofInt(binding.tabLayout.height, tabLayoutStartHeight)
        val tabLayoutMarginTopAnimator = ValueAnimator.ofInt(binding.tabLayout.marginTop, tabLayoutStartMarginTop)
        val tabLayoutMarginEndAnimator = ValueAnimator.ofInt(binding.tabLayout.marginEnd, tabLayoutStartMarginEnd)
        val alphaAnimator = ValueAnimator.ofFloat(binding.imageViewIcon.alpha, 1.0f)

        val animators = mutableListOf<ValueAnimator>().apply {
            add(bannerWidthAnimator)
            add(tabLayoutHeightAnimator)
            add(tabLayoutMarginTopAnimator)
            add(tabLayoutMarginEndAnimator)
            add(alphaAnimator)
        }

        animators.forEach { animator ->
            animator.apply {
                duration = animationDuration
                interpolator = AccelerateDecelerateInterpolator()

                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue

                    when (animator) {
                        bannerWidthAnimator -> {
                            binding.viewBanner.layoutParams.height = animatedValue as Int
                            binding.viewBanner.requestLayout()
                        }
                        tabLayoutHeightAnimator -> {
                            binding.tabLayout.layoutParams.height = animatedValue as Int
                            binding.tabLayout.requestLayout()
                        }
                        tabLayoutMarginTopAnimator -> {
                            val layoutParams = binding.tabLayout.layoutParams as ViewGroup.MarginLayoutParams
                            layoutParams.topMargin = animatedValue as Int
                            binding.tabLayout.layoutParams = layoutParams
                            binding.tabLayout.requestLayout()
                        }
                        tabLayoutMarginEndAnimator -> {
                            val layoutParams = binding.tabLayout.layoutParams as ViewGroup.MarginLayoutParams
                            layoutParams.marginStart = animatedValue as Int
                            layoutParams.marginEnd = animatedValue
                            binding.tabLayout.layoutParams = layoutParams
                            binding.tabLayout.requestLayout()
                        }
                        alphaAnimator -> {
                            binding.imageViewIcon.alpha = animatedValue as Float
                            binding.textViewInfo.alpha = animatedValue
                        }
                    }
                }

                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        // tabLayout의 좌측 ConstraintLayout을 parent에 연결
                        val layoutParams = binding.tabLayout.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        binding.tabLayout.layoutParams = layoutParams
                        binding.tabLayout.requestLayout()

                        isAnimating = true
                    }
                    override fun onAnimationEnd(animation: Animator) {
                        // tabLayout의 너비를 match_parent 로 설정
                        binding.tabLayout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

                        isAnimating = false
                    }
                    override fun onAnimationCancel(animation: Animator) { isAnimating = false }
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        }
    }
    private fun foldAnimation() {
        val bannerWidthAnimator = ValueAnimator.ofInt(binding.viewBanner.height, bannerTargetHeight)
        val tabLayoutHeightAnimator = ValueAnimator.ofInt(binding.tabLayout.height, tabLayoutTargetHeight)
        val tabLayoutWidthAnimator = ValueAnimator.ofInt(binding.tabLayout.width, tabLayoutTargetWidth)
        val tabLayoutMarginTopAnimator = ValueAnimator.ofInt(binding.tabLayout.marginTop, tabLayoutTargetMarginTop)
        val tabLayoutMarginEndAnimator = ValueAnimator.ofInt(binding.tabLayout.marginEnd, tabLayoutTargetMarginEnd)
        val alphaAnimator = ValueAnimator.ofFloat(binding.imageViewIcon.alpha, 0f)

        val animators = mutableListOf<ValueAnimator>().apply {
            add(bannerWidthAnimator)
            add(tabLayoutHeightAnimator)
            add(tabLayoutWidthAnimator)
            add(tabLayoutMarginTopAnimator)
            add(tabLayoutMarginEndAnimator)
            add(alphaAnimator)
        }

        animators.forEach { animator ->
            animator.apply {
                duration = animationDuration
                interpolator = AccelerateDecelerateInterpolator()

                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue

                    when (animator) {
                        bannerWidthAnimator -> {
                            binding.viewBanner.layoutParams.height = animatedValue as Int
                            binding.viewBanner.requestLayout()
                        }
                        tabLayoutHeightAnimator -> {
                            binding.tabLayout.layoutParams.height = animatedValue as Int
                            binding.tabLayout.requestLayout()
                        }
                        tabLayoutWidthAnimator -> {
                            binding.tabLayout.layoutParams.width = animatedValue as Int
                            binding.tabLayout.requestLayout()
                        }
                        tabLayoutMarginTopAnimator -> {
                            val layoutParams = binding.tabLayout.layoutParams as ViewGroup.MarginLayoutParams
                            layoutParams.topMargin = animatedValue as Int
                            binding.tabLayout.layoutParams = layoutParams
                            binding.tabLayout.requestLayout()
                        }
                        tabLayoutMarginEndAnimator -> {
                            val layoutParams = binding.tabLayout.layoutParams as ViewGroup.MarginLayoutParams
                            layoutParams.marginEnd = animatedValue as Int
                            binding.tabLayout.layoutParams = layoutParams
                            binding.tabLayout.requestLayout()
                        }
                        alphaAnimator -> {
                            binding.imageViewIcon.alpha = animatedValue as Float
                            binding.textViewInfo.alpha = animatedValue
                        }
                    }
                }

                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        // tabLayout의 좌측 ConstraintLayout을 해제
                        val layoutParams = binding.tabLayout.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.startToStart = ConstraintLayout.LayoutParams.UNSET
                        binding.tabLayout.layoutParams = layoutParams
                        binding.tabLayout.requestLayout()

                        isAnimating = true
                    }
                    override fun onAnimationEnd(animation: Animator) { isAnimating = false }
                    override fun onAnimationCancel(animation: Animator) { isAnimating = false }
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        }
    }
}

interface RecyclerViewScrollListener {
    fun onScrollChanged(scrollY:Int)
}
