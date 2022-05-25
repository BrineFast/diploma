package com.diploma.slepov.custom.processor

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.diploma.slepov.custom.view.objectdetection.DetectedInfo
import com.diploma.slepov.custom.view.productsearch.SearchedObject
import java.util.HashSet

/** Класс с описанием главных методов в жизненном цикле приложения **/
class WorkflowModel(application: Application) : AndroidViewModel(application) {

    val currentState = MutableLiveData<WorkflowState>()
    val detectedObject = MutableLiveData<DetectedInfo>()
    val searchedObject = MutableLiveData<SearchedObject>()

    private val objectIdsToSearch = HashSet<Int>()

    var isCameraLive = false
        private set

    private var confirmedObject: DetectedInfo? = null

    private val context: Context
        get() = getApplication<Application>().applicationContext

    /** Перечисление всех возможных состояние **/
    enum class WorkflowState {
        NOT_STARTED,
        DETECTING,
        DETECTED,
        CONFIRMING,
        CONFIRMED,
        SEARCHING,
        SEARCHED
    }

    /** Изменение состояния цикла в зависимости от текущей активности **/
    @MainThread
    fun changeState(currentState: WorkflowState) {
        if (currentState != WorkflowState.CONFIRMED &&
            currentState != WorkflowState.SEARCHING &&
            currentState != WorkflowState.SEARCHED
        ) {
            confirmedObject = null
        }
        this.currentState.value = currentState
    }

    /** Изменение состояния цикла в зависимости от текущей активности **/
    @MainThread
    fun confirmingObject(confirmingObject: DetectedInfo, progress: Float) {
        val confirmed = progress.compareTo(1f) == 0
        if (confirmed) {
            confirmedObject = confirmingObject
            changeState(WorkflowState.SEARCHING)
            startSearch(confirmingObject)
        } else {
            changeState(WorkflowState.CONFIRMING)
        }
    }

    /** Добавление найденных объектов в очередь поиска **/
    private fun startSearch(searchedObject: DetectedInfo) {
        val objectId = searchedObject.objectId ?: throw NullPointerException()
        if (objectIdsToSearch.contains(objectId)) {
            return
        }

        objectIdsToSearch.add(objectId)
        detectedObject.value = searchedObject
    }

    /** Изменение состояния на включение камеры **/
    fun markCameraLive() {
        isCameraLive = true
        objectIdsToSearch.clear()
    }

    /** Изменение состояния на отключение камеры **/
    fun markCameraFrozen() {
        isCameraLive = false
    }

    /** Формирование списка найденных объектов **/
    fun onSearchCompleted(detectedObject: DetectedInfo, products: List<Product>) {
        val confirmed = confirmedObject
        if (detectedObject != confirmed) {
            return
        }

        objectIdsToSearch.remove(detectedObject.objectId)
        changeState(WorkflowState.SEARCHED)
        searchedObject.value = SearchedObject(context.resources, confirmed, products)
    }
}
