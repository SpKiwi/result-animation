package com.example.animation.interactor

import androidx.lifecycle.*
import com.example.animation.animation.AutoFollowGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoFollowController(
    val coroutineScope: LifecycleCoroutineScope,
    val favoritesManagerWrapper: FavoritesManagerWrapper,
    val autoFollowPreferences: AutoFollowPreferences,
    val autoFollowCancellationCache: AutoFollowCancellationCache,
    val autoFollowConfig: AutoFollowConfig
) {

    private val mutableAutofollowState: MutableLiveData<AutoFollowResult> = MutableLiveData()
    val autoFollowState: LiveData<AutoFollowResult> get() = mutableAutofollowState

    private var autoFollowJob: Job? = null
    private var isCancellationTimerOn: Boolean = false
    private var streamId: String = ""

    /**
     * @return true if autoFollow job started
     **/
    fun startAutoFollow(
        autoFollowGroup: AutoFollowGroup,
        streamId: String,
        publisherId: String,
        followAction: Runnable
    ): Boolean {
        if (!autoFollowConfig.isEnabled
            || favoritesManagerWrapper.isFollowedByMe(publisherId) // todo check this as well and cancel animation if needed
            || favoritesManagerWrapper.getMyFollowingCount() > autoFollowConfig.followingLimit
            || autoFollowPreferences.cancellationCount > autoFollowConfig.cancelLimit
            || autoFollowCancellationCache.cancelledAutoFollowsForStreams.contains(streamId)
        ) {
            mutableAutofollowState.postValue(AutoFollowResult.FAILURE)
            return false
        }

        this.streamId = streamId

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
                    followAction.run()
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

object FavoritesManagerWrapper {
    fun isFollowedByMe(publisherId: String) = false
    fun getMyFollowingCount(): Int = 1
}