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
import ru.wertik.orca.compose.Orca
import ru.wertik.orca.core.OrcaMarkdownParser

class MainActivity : ComponentActivity() {

    private val markdown = """
        # Orca v0.8

        Compose Multiplatform markdown renderer.

        ## Inline formatting
        Support for **bold**, *italic*, ~~strikethrough~~, `inline code`, and [links](https://github.com/JetBrains/markdown "JetBrains Markdown").

        ## Superscript and subscript
        E = mc^2^ and H~2~O are now rendered properly.

        ## Emoji shortcodes
        :rocket: Launch ready! :fire: Hot feature :sparkles: Looking good :thumbsup:

        ## List
        - first bullet
        - second bullet
        - third bullet

        ## Task list
        - [x] parser extensions wired
        - [x] compose rendering updated
        - [x] admonition support
        - [x] inline image rendering
        - [ ] latex math rendering

        ## Admonitions

        > [!NOTE]
        > Orca now supports GitHub-style admonitions.

        > [!TIP]
        > Use admonitions to highlight important information in your docs.

        > [!WARNING]
        > Breaking changes may occur in future major versions.

        > [!CAUTION]
        > Do not use in production without testing first.

        ## Table
        | module | status | docs |
        |:-------|:------:|-----:|
        | **core** | ready | [api](https://github.com/JetBrains/markdown) |
        | compose | ready | `android` |

        ## Image
        ![markdown logo](https://raw.githubusercontent.com/github/explore/main/topics/markdown/markdown.png)

        ---

        ## Quote
        > Keep architecture simple and stable first.

        ## HTML block
        <p>This is a <b>bold</b> and <i>italic</i> HTML paragraph with a <a href="https://example.com">link</a>.</p>

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
                        parser = OrcaMarkdownParser(),
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
