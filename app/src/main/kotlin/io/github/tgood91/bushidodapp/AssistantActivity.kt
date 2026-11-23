package io.github.tgood91.bushidodapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The assistant: a native screen with a single hamburger menu (☰). The menu slides out the chat
 * window, which talks to a model served by Ollama on the user's computer or local network.
 *
 * It is opened from the "Assistant" long-press shortcut. The dApp itself still opens in Chrome as
 * a Trusted Web Activity; this screen never touches wallets, keys or transactions.
 */
class AssistantActivity : Activity() {

    private lateinit var drawer: DrawerLayout
    private lateinit var messagesView: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var serverLabel: TextView

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<OllamaApi.Message>()
    private var waiting = false

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val baseUrl get() = prefs.getString(KEY_URL, null) ?: OllamaApi.DEFAULT_BASE_URL
    private val model get() = prefs.getString(KEY_MODEL, null) ?: OllamaApi.DEFAULT_MODEL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assistant)

        drawer = findViewById(R.id.drawer)
        messagesView = findViewById(R.id.chat_messages)
        scrollView = findViewById(R.id.chat_scroll)
        input = findViewById(R.id.chat_input)
        sendButton = findViewById(R.id.chat_send)
        serverLabel = findViewById(R.id.chat_server)

        applyInsets(findViewById(R.id.main_content))
        applyInsets(findViewById(R.id.chat_panel))

        findViewById<ImageButton>(R.id.menu_button).setOnClickListener {
            if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START)
            else drawer.openDrawer(GravityCompat.START)
        }
        findViewById<ImageButton>(R.id.chat_close).setOnClickListener { drawer.closeDrawer(GravityCompat.START) }
        findViewById<Button>(R.id.open_dapp).setOnClickListener {
            startActivity(Intent(this, BushidoLauncherActivity::class.java))
        }
        findViewById<Button>(R.id.chat_clear).setOnClickListener { clearChat() }
        serverLabel.setOnClickListener { showSettings() }
        sendButton.setOnClickListener { send() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { send(); true } else false
        }

        updateServerLabel()
        clearChat()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android 13; kept for API 25-32")
    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START)
        else @Suppress("DEPRECATION") super.onBackPressed()
    }

    /** Pads a panel clear of the status bar, navigation bar and keyboard (the app draws edge to edge). */
    private fun applyInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
    }

    private fun clearChat() {
        history.clear()
        history += OllamaApi.Message.system(getString(R.string.assistant_system_prompt))
        messagesView.removeAllViews()
        addBubble(getString(R.string.assistant_welcome), fromUser = false)
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isEmpty() || waiting) return
        input.setText("")
        addBubble(text, fromUser = true)
        history += OllamaApi.Message.user(text)

        val pending = addBubble(getString(R.string.assistant_thinking), fromUser = false)
        setWaiting(true)
        val request = history.toList()
        val client = OllamaClient(baseUrl)
        val model = model
        executor.execute {
            val result = runCatching { client.chat(model, request) }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                setWaiting(false)
                result.onSuccess { reply ->
                    pending.text = reply
                    history += OllamaApi.Message.assistant(reply)
                }.onFailure { e ->
                    // Drop the unanswered question so the next try does not send it twice.
                    history.removeAt(history.lastIndex)
                    pending.text = getString(R.string.assistant_error, baseUrl, e.message ?: e.javaClass.simpleName)
                    pending.setTextColor(getColor(R.color.assistant_error))
                }
                scrollToEnd()
            }
        }
    }

    private fun setWaiting(value: Boolean) {
        waiting = value
        sendButton.isEnabled = !value
    }

    private fun addBubble(text: String, fromUser: Boolean): TextView {
        val bubble = TextView(this).apply {
            this.text = text
            setTextIsSelectable(true)
            setTextColor(getColor(R.color.assistant_text))
            textSize = 15f
            val pad = dp(12)
            setPadding(pad, dp(8), pad, dp(8))
            setBackgroundResource(if (fromUser) R.drawable.bubble_user else R.drawable.bubble_assistant)
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (fromUser) Gravity.END else Gravity.START
            topMargin = dp(6)
            if (fromUser) marginStart = dp(40) else marginEnd = dp(40)
        }
        messagesView.addView(bubble, params)
        scrollToEnd()
        return bubble
    }

    private fun scrollToEnd() = scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }

    private fun updateServerLabel() {
        serverLabel.text = getString(R.string.assistant_server_label, baseUrl, model)
    }

    private fun showSettings() {
        val urlField = EditText(this).apply {
            setText(baseUrl)
            hint = getString(R.string.assistant_settings_url_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSingleLine = true
        }
        val modelField = EditText(this).apply {
            setText(model)
            hint = getString(R.string.assistant_settings_model_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = true
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(urlField)
            addView(modelField)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.assistant_settings_title)
            .setMessage(R.string.assistant_settings_help)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val url = OllamaApi.normalizeBaseUrl(urlField.text.toString())
                val name = modelField.text.toString().trim()
                if (url == null || name.isEmpty()) {
                    Toast.makeText(this, R.string.assistant_settings_invalid, Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString(KEY_URL, url).putString(KEY_MODEL, name).apply()
                    updateServerLabel()
                }
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PREFS = "assistant"
        const val KEY_URL = "ollama_url"
        const val KEY_MODEL = "ollama_model"
    }
}
