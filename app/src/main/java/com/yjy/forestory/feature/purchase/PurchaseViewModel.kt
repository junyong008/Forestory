package com.yjy.forestory.feature.purchase

import androidx.lifecycle.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.yjy.forestory.model.repository.TicketRepository
import com.yjy.forestory.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _rewardAd = MutableLiveData<RewardedAd?>(null)
    val rewardAd: LiveData<RewardedAd?> get() = _rewardAd

    fun initRewardAd(rewardedAd: RewardedAd) {
        _rewardAd.value = rewardedAd
    }

    fun emptyRewardAd() {
        _rewardAd.value = null
    }


    val tickets = ticketRepository.getTicket().asLiveData()

    private val _isCompleteAdd = MutableLiveData<Event<Int>>()
    val isCompleteAdd: LiveData<Event<Int>> get() = _isCompleteAdd

    fun addTicket(count: Int) {
        viewModelScope.launch {
            ticketRepository.setTicket(tickets.value!! + count)
            ticketRepository.setTicketNeedConsume("") // 소비 돼야 할 미소비 티켓 없애기
            _isCompleteAdd.value = Event(count)
        }
    }

    suspend fun setTicketNeedToConsume(productId: String) {
        ticketRepository.setTicketNeedConsume(productId)
    }

    suspend fun getTicketNeedToConsume(): String? {
        return ticketRepository.getTicketNeedConsume().firstOrNull()
    }
}