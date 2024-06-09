package com.agape.googledriveclone.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class SingleLiveEvent<T> : MutableLiveData<T>() {
    private val observers = mutableListOf<ObserverWrapper<in T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = ObserverWrapper(observer)
        observers.add(wrapper)
        super.observe(owner, wrapper)
    }

    override fun setValue(t: T?) {
        observers.forEach { it.newValue() }
        super.setValue(t)
    }

    override fun postValue(value: T) {
        observers.forEach { it.newValue() }
        super.postValue(value)
    }

    private class ObserverWrapper<T>(private val observer: Observer<T>) : Observer<T> {
        private var pending = false
        fun newValue() {
            pending = true
        }

        override fun onChanged(value: T) {
            if (pending) {
                pending = false
                observer.onChanged(value)
            }
        }
    }
}
