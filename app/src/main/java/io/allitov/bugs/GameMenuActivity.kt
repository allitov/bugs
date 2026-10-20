package io.allitov.bugs

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/** Главное игровое меню: запуск раунда и переходы к правилам, авторам и настройкам. */
class GameMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<Button>(R.id.btnRules).setOnClickListener { openPage(MainActivity.PAGE_RULES) }
        findViewById<Button>(R.id.btnAuthors).setOnClickListener { openPage(MainActivity.PAGE_AUTHORS) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { openPage(MainActivity.PAGE_SETTINGS) }
    }

    private fun openPage(page: Int) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SELECTED_PAGE, page)
        startActivity(intent)
    }
}
