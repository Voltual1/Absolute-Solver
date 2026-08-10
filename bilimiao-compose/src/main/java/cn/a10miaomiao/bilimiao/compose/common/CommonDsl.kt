package cn.a10miaomiao.bilimiao.compose.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.navigation.NavOptions
import cn.a10miaomiao.bilimiao.compose.R
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

val defaultNavOptions get() = NavOptions.Builder()
    .setEnterAnim(R.anim.miao_fragment_open_enter)
    .setExitAnim(R.anim.miao_fragment_open_exit)
    .setPopEnterAnim(R.anim.miao_fragment_close_enter)
    .setPopExitAnim(R.anim.miao_fragment_close_exit)
    .build()

@Composable
inline fun <reified VM : ViewModel> diViewModel(
    key: String? = null,
    noinline parameters: (() -> ParametersHolder)? = null,
): VM {
    return koinViewModel(
        key = key,
        parameters = parameters
    )
}