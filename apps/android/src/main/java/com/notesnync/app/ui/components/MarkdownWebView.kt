package com.notesnync.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * Full Markdown (GFM: headings, lists, tables, code, task lists, links, images) plus LaTeX math
 * (`$...$` inline, `$$...$$` block) rendered offline via bundled marked.js + MathJax-SVG in a WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(markdown: String, dark: Boolean, accentHex: String, modifier: Modifier = Modifier) {
    val text = if (dark) "#ECE9F5" else "#211E33"
    val muted = if (dark) "#A9A4B8" else "#6B677A"
    val codeBg = if (dark) "#211E2E" else "#F1EEF8"
    val border = if (dark) "#373345" else "#E2DEEC"
    val html = buildHtml(markdown, text, muted, codeBg, border, accentHex)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = true
            }
        },
        update = { web ->
            web.loadDataWithBaseURL("file:///android_asset/preview/", html, "text/html", "utf-8", null)
        },
    )
}

private fun buildHtml(
    markdown: String,
    text: String,
    muted: String,
    codeBg: String,
    border: String,
    accent: String,
): String {
    val md = JSONObject.quote(markdown) // safe JS string literal (escapes quotes, backslashes, newlines)
    val d = "$" // literal dollar for math delimiters, kept out of Kotlin templating
    return """
<!DOCTYPE html><html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<style>
 html,body{margin:0;padding:0;background:transparent;color:$text;
   font-family:-apple-system,Roboto,'Segoe UI',system-ui,sans-serif;font-size:16px;line-height:1.62;
   -webkit-text-size-adjust:100%;word-wrap:break-word;overflow-wrap:anywhere;}
 body{padding:2px 2px 48px;}
 h1,h2,h3,h4{font-weight:800;line-height:1.25;margin:1.05em 0 .45em;}
 h1{font-size:1.7em} h2{font-size:1.4em} h3{font-size:1.18em}
 p{margin:.5em 0}
 a{color:$accent;text-decoration:none}
 code{background:$codeBg;padding:.14em .36em;border-radius:6px;font-family:'Roboto Mono',monospace;font-size:.88em}
 pre{background:$codeBg;padding:14px;border-radius:14px;overflow:auto}
 pre code{background:none;padding:0}
 blockquote{margin:.6em 0;padding:.35em .9em;border-left:4px solid $accent;background:${accent}18;border-radius:0 12px 12px 0;color:$muted}
 table{border-collapse:collapse;width:100%;margin:.7em 0;font-size:.94em;display:block;overflow-x:auto}
 th,td{border:1px solid $border;padding:7px 11px;text-align:left}
 th{background:$codeBg}
 img{max-width:100%;border-radius:12px}
 ul,ol{padding-left:1.35em;margin:.4em 0}
 li{margin:.2em 0}
 hr{border:none;border-top:1px solid $border;margin:1.2em 0}
 input[type=checkbox]{margin-right:.45em;vertical-align:middle}
 mjx-container{overflow-x:auto;overflow-y:hidden;max-width:100%}
</style>
<script>
 window.MathJax={
   tex:{inlineMath:[['$d','$d'],['\(','\)']],displayMath:[['$d$d','$d$d'],['\[','\]']]},
   svg:{fontCache:'global'},
   options:{skipHtmlTags:['script','noscript','style','textarea','pre','code']},
   startup:{typeset:false}
 };
</script>
<script src="marked.min.js"></script>
<script src="tex-svg.js"></script>
</head>
<body>
<div id="content"></div>
<script>
 var raw = $md;
 try { document.getElementById('content').innerHTML = marked.parse(raw, {gfm:true, breaks:true}); }
 catch(e){ document.getElementById('content').textContent = raw; }
 if (window.MathJax && MathJax.typesetPromise) { MathJax.typesetPromise(); }
</script>
</body></html>
    """.trimIndent()
}
