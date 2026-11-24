package io.github.tgood91.bushidodapp

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * The native dojo screen: virtue of the day, daily check-in and streak, and live Dojo Pass status.
 * Read-only on chain: no wallet connection, no signing.
 */
class MainActivity : Activity() {

    private lateinit var store: CheckInStore

    private lateinit var passView: TextView
    private lateinit var pracView: TextView
    private lateinit var streakView: TextView
    private lateinit var totalView: TextView
    private lateinit var rankView: TextView
    private lateinit var checkInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = CheckInStore(SharedPrefsCheckInStorage(this))

        passView = findViewById(R.id.passStatus)
        pracView = findViewById(R.id.pracStatus)
        streakView = findViewById(R.id.streakCount)
        totalView = findViewById(R.id.totalCount)
        rankView = findViewById(R.id.rankLabel)
        checkInButton = findViewById(R.id.checkInButton)

        val virtue = Virtues.ofTheDay()
        findViewById<TextView>(R.id.virtueKanji).text = virtue.kanji
        findViewById<TextView>(R.id.virtueName).text = virtue.name
        findViewById<TextView>(R.id.virtueLine).text = virtue.line

        checkInButton.setOnClickListener {
            store.checkIn()
            renderStats()
        }

        renderStats()
        refreshPassStatus()
    }

    override fun onResume() {
        super.onResume()
        renderStats() // the day may have rolled over while the screen was in the background
    }

    private fun refreshPassStatus() {
        passView.text = getString(R.string.dojo_pass_status, getString(R.string.status_checking))
        pracView.text = getString(R.string.dojo_prac_status, getString(R.string.status_checking))
        DojoPassReader.fetchStatus(WALLET) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { s ->
                    passView.text = getString(
                        R.string.dojo_pass_status,
                        getString(if (s.hasPass) R.string.status_held else R.string.status_not_held),
                    )
                    pracView.text = getString(
                        R.string.dojo_prac_status,
                        getString(if (s.isPractitioner) R.string.status_yes else R.string.status_no),
                    )
                }.onFailure {
                    passView.text = getString(R.string.dojo_pass_status, getString(R.string.status_unreachable))
                    pracView.text = getString(R.string.dojo_prac_status, getString(R.string.status_unknown))
                }
            }
        }
    }

    private fun renderStats() {
        val s = store.state()
        streakView.text = s.streak.toString()
        totalView.text = s.total.toString()
        rankView.text = CheckInStore.rankFor(s.streak)
        val done = store.checkedInToday()
        checkInButton.isEnabled = !done
        checkInButton.setText(if (done) R.string.checked_in else R.string.check_in)
    }

    private companion object {
        /** The wallet whose pass is shown. Public address; reading it needs no key. */
        const val WALLET = "0xba397faae1d5fe10c0356f2e585bef34577ab111"
    }
}
