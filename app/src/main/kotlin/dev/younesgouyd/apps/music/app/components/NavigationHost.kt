package dev.younesgouyd.apps.music.app.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.younesgouyd.apps.music.app.Component
import dev.younesgouyd.apps.music.app.data.RepoStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.*

class NavigationHost(
    repoStore: RepoStore,
    startDestination: Destination
) : Component() {
    override val title: String
    private val destinationFactory: DestinationFactory
    private val navigationController: NavigationController
    private val backStack: BackStack

    init {
        title = ""
        destinationFactory = DestinationFactory(repoStore)
        navigationController = NavigationController()
        backStack = BackStack(
            when (startDestination) {
                Destination.Settings -> destinationFactory.getSettings()
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun show(modifier: Modifier) {
        val currentDestination by backStack.currentDestination.collectAsState()
        val inHome by backStack.inHome.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (!inHome) {
                            IconButton(
                                content = { Icon(Icons.AutoMirrored.Default.ArrowBack, null) },
                                onClick = { navigationController.navigateBack() }
                            )
                        }
                    },
                    title = { Text(text = currentDestination.title, style = MaterialTheme.typography.headlineMedium) }
                )
            },
            content = { paddingValues ->
                currentDestination.show(Modifier.padding(paddingValues))
            }
        )
    }

    override fun clear() {
        while (backStack.isNotEmpty()) {
            backStack.top().clear()
            backStack.pop()
        }
        coroutineScope.cancel()
    }

    enum class Destination { Settings }

    private inner class BackStack(startDestination: Component) {
        val inHome: MutableStateFlow<Boolean>
        val currentDestination: MutableStateFlow<Component>
        private val stack: Stack<Component>

        init {
            stack = Stack<Component>().apply { push(startDestination) }
            currentDestination = MutableStateFlow(startDestination)
            inHome = MutableStateFlow(true)
        }

        fun push(component: Component) {
            stack.push(component)
            currentDestination.update { stack.peek() }
            inHome.update { false }
        }

        fun pop() {
            stack.pop()
            if (stack.isNotEmpty()) {
                currentDestination.update { stack.peek() }
            }
            inHome.update { stack.size == 1 }
        }

        fun top(): Component {
            return stack.peek()
        }

        fun isNotEmpty(): Boolean {
            return stack.isNotEmpty()
        }
    }

    private inner class DestinationFactory(private val repoStore: RepoStore) {
        fun getSettings(): Settings {
            return Settings(
                repoStore = repoStore
            )
        }
    }

    private inner class NavigationController {
        fun navigateTo(destination: Component) {
            backStack.push(destination)
        }

        fun navigateBack() {
            backStack.top().clear()
            backStack.pop()
        }
    }
}
