package com.example.animation.interactor

import androidx.lifecycle.*
import com.example.animation.animation.AutoFollowGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoFollowController(
    val autoFollowGroup: AutoFollowGroup, //
    val coroutineScope: LifecycleCoroutineScope, //
    val streamId: String,
    val isFollowingCurrentBroadcaster: Boolean,
    val followingCount: Int,
    val followActionCallback: () -> Unit,
    val autoFollowPreferences: AutoFollowPreferences, //
    val autoFollowCancellationCache: AutoFollowCancellationCache, //
    val autoFollowConfig: AutoFollowConfig //
) {

    private val mutableAutofollowState: MutableLiveData<AutoFollowResult> = MutableLiveData()
    val autoFollowState: LiveData<AutoFollowResult> get() = mutableAutofollowState

    private var autoFollowJob: Job? = null
    private var isCancellationTimerOn: Boolean = false

    /**
     * @return true if autoFollow job started
     **/
    fun startAutoFollow(): Boolean {
        if (!autoFollowConfig.isEnabled
            || isFollowingCurrentBroadcaster
            || followingCount > autoFollowConfig.followingLimit
            || autoFollowPreferences.cancellationCount > autoFollowConfig.cancelLimit
            || autoFollowCancellationCache.cancelledAutoFollowsForStreams.contains(streamId)
        ) {
            mutableAutofollowState.postValue(AutoFollowResult.FAILURE)
            return false
        }

        autoFollowJob = coroutineScope.launch {
            delay(autoFollowConfig.startAfter * 1_000L)
            mutableAutofollowState.postValue(AutoFollowResult.IN_PROGRESS)
            isCancellationTimerOn = true
            autoFollowGroup.startAutoFollowAnimation(autoFollowConfig.cancelDuration * 1_000L, object : AutoFollowGroup.AutoFollowListener {
                override fun onAutoFollowCancel() {
                    isCancellationTimerOn = false
                    performAndTrackCancelAutoFollow()
                }

                override fun onAutofollowTimerElapsed() {
                    isCancellationTimerOn = false
                    followActionCallback()
                }

                override fun onAutoFollowEnd() {
                    mutableAutofollowState.postValue(AutoFollowResult.SUCCESS)
                }
            })
        }
        return true
    }

    /**
     * Can be used for cases like opening clean ui or exiting the stream
     **/
    fun cancelAutoFollow() {
        autoFollowJob?.cancel()
        if (isCancellationTimerOn) {
            performAndTrackCancelAutoFollow()
        } else {
            performCancelAutoFollow()
        }
    }

    private fun performAndTrackCancelAutoFollow() {
        performCancelAutoFollow()
        autoFollowPreferences.cancellationCount++
        autoFollowCancellationCache.addCancelledId(streamId)
    }

    private fun performCancelAutoFollow() {
        mutableAutofollowState.postValue(AutoFollowResult.FAILURE)
    }

}

enum class AutoFollowResult {
    IN_PROGRESS, SUCCESS, FAILURE
}

object AutoFollowConfig {
    val isEnabled: Boolean = true
    val followingLimit: Int = 5
    val startAfter: Int = 60
    val cancelDuration: Int = 5
    val cancelLimit: Int = 5
}

object AutoFollowPreferences {
    var cancellationCount: Int = 4
}

object AutoFollowCancellationCache {
    val cancelledAutoFollowsForStreams: List<String> = listOf("id")
    fun addCancelledId(id: String): Unit = Unit
}