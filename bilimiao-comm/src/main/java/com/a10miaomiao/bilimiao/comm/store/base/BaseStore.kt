package com.a10miaomiao.bilimiao.comm.store.base

import android.content.Context
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(InternalCoroutinesApi::class)
interface BaseStore<T> {
    val stateFlow: MutableStateFlow<T>

    val state: T get() = stateFlow.value

    fun copyState(): T

    fun init(context: Context) {
    }

    fun setState (block: T.() -> Unit) {
        stateFlow.value = copyState().apply(block)
    }
}