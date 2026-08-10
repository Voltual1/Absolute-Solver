package cn.a10miaomiao.bilimiao.compose.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.navigation.NavOptions
import cn.a10miaomiao.bilimiao.compose.R
import org.kodein.di.DI
import org.kodein.di.compose.localDI
import org.koin.androidx.compose.koinViewModel

val defaultNavOptions get() = NavOptions.Builder()
    .setEnterAnim(R.anim.miao_fragment_open_enter)
    .setExitAnim(R.anim.miao_fragment_open_exit)
    .setPopEnterAnim(R.anim.miao_fragment_close_enter)
    .setPopExitAnim(R.anim.miao_fragment_close_exit)
    .build()

@Composable
inline fun <reified VM : ViewModel> diViewModel(
    di: DI = localDI(),
    key: String? = null,
): VM {
    // 运行时检测：如果当前 ViewModel 已注册在 Koin 容器中，自动走 Koin 注入路径
    val koin = org.koin.core.context.GlobalContext.getOrNull()
    if (koin != null) {
        val hasKoinDefinition = koin.getOrNull<VM>() != null
        if (hasKoinDefinition) {
            return koinViewModel(key = key)
        }
    }

    // 否则退回到原有的 Kodein 反射实例化
    return diViewModel(di, key) {
        val constructor = VM::class.java.getDeclaredConstructor(
            DI::class.java
        )
        constructor.newInstance(it)
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
inline fun <reified VM : ViewModel> diViewModel(
    di: DI = localDI(),
    key: String? = null,
    crossinline initializer: ((di: DI) -> VM),
): VM {
    val koin = org.koin.core.context.GlobalContext.getOrNull()
    if (koin != null) {
        val hasKoinDefinition = koin.getOrNull<VM>() != null
        if (hasKoinDefinition) {
            return koinViewModel(key = key)
        }
    }

    return androidx.lifecycle.viewmodel.compose.viewModel<VM>(
        key = remember(key, di) {
            val diHex = di.hashCode().toHexString()
            (key ?: VM::class.simpleName) + diHex
        },
        initializer = {
            initializer(di)
        }
    )
}