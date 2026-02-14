package ru.wertik.orca.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wertik.orca.compose.android.Orca

class MainActivity : ComponentActivity() {

    private val markdown = """
        # Orca v0.2
        
        Android-first markdown renderer for Compose.
        
        ## Inline formatting
        Support for **bold**, *italic*, ~~strikethrough~~, `inline code`, and [links](https://github.com/commonmark/commonmark-java).
        
        ## List
        - first bullet
        - second bullet
        - third bullet

        ## Task list
        - [x] parser extensions wired
        - [x] compose rendering updated
        - [ ] html blocks

        ## Table
        | module | status | docs |
        |:-------|:------:|-----:|
        | **core** | ready | [api](https://github.com/commonmark/commonmark-java) |
        | compose | ready | `android` |

        ## Image
        ![markdown logo](https://raw.githubusercontent.com/github/explore/main/topics/markdown/markdown.png)

        ---
        
        ## Quote
        > Keep architecture simple and stable first.
        
        ## Code block
        ```kotlin
        fun greet(name: String) {
            println("hello, ${'$'}name")
        }
        ```
        """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val context = LocalContext.current
                    Orca(
                        markdown = markdown,
                        modifier = Modifier.padding(16.dp),
                        onLinkClick = { link ->
                            Toast.makeText(context, "link clicked: $link", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}
