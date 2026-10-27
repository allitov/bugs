package io.allitov.bugs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.allitov.bugs.data.AppDatabase
import io.allitov.bugs.data.PlayerEntity
import io.allitov.bugs.data.PlayerSession
import io.allitov.bugs.model.ZodiacSign
import kotlinx.coroutines.launch

/** Главное игровое меню: выбор игрока через нижний лист, запуск раунда и переходы к игроку, рекордам, правилам, авторам и настройкам. */
class GameMenuActivity : AppCompatActivity() {

    private lateinit var playerPicker: View
    private lateinit var tvPlayerPicker: TextView

    private var players: List<PlayerEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_menu)

        playerPicker = findViewById(R.id.playerPicker)
        tvPlayerPicker = findViewById(R.id.tvPlayerPicker)
        playerPicker.setOnClickListener { showPlayerPicker() }

        findViewById<Button>(R.id.btnPlay).setOnClickListener { startGame() }
        findViewById<Button>(R.id.btnPlayer).setOnClickListener { openPage(MainActivity.PAGE_PLAYER) }
        findViewById<Button>(R.id.btnRecords).setOnClickListener { openPage(MainActivity.PAGE_RECORDS) }
        findViewById<Button>(R.id.btnRules).setOnClickListener { openPage(MainActivity.PAGE_RULES) }
        findViewById<Button>(R.id.btnAuthors).setOnClickListener { openPage(MainActivity.PAGE_AUTHORS) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { openPage(MainActivity.PAGE_SETTINGS) }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentPlayer()
        loadPlayers()
    }

    private fun updateCurrentPlayer() {
        val player = PlayerSession.current
        tvPlayerPicker.text = if (player != null) {
            player.fullName
        } else {
            getString(R.string.menu_select_player_hint)
        }
    }

    /** Загружает список игроков и восстанавливает последнего выбранного (id хранится в настройках). */
    private fun loadPlayers() {
        lifecycleScope.launch {
            players = AppDatabase.get(this@GameMenuActivity).playerDao().getAll()
            if (PlayerSession.current == null) {
                val lastId = PlayerSession.loadLastPlayerId(this@GameMenuActivity)
                players.find { it.id == lastId }?.let { PlayerSession.current = it }
            }
            updateCurrentPlayer()
        }
    }

    /** Показывает нижний лист со списком зарегистрированных игроков и кнопкой регистрации нового. */
    private fun showPlayerPicker() {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_player_picker, null)

        val listView = view.findViewById<ListView>(R.id.listPlayers)
        view.findViewById<TextView>(R.id.tvPlayersEmpty).isVisible = players.isEmpty()
        listView.adapter = PlayerPickerAdapter { player ->
            confirmDeletePlayer(player) {
                sheet.dismiss()
                showPlayerPicker()
            }
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val player = players.getOrNull(position) ?: return@setOnItemClickListener
            selectPlayer(player)
            sheet.dismiss()
        }

        view.findViewById<Button>(R.id.btnNewPlayer).setOnClickListener {
            sheet.dismiss()
            openPage(MainActivity.PAGE_PLAYER)
        }

        sheet.setContentView(view)
        sheet.setOnShowListener { dialog ->
            (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        sheet.show()
    }

    private fun selectPlayer(player: PlayerEntity) {
        PlayerSession.current = player
        PlayerSession.persist(this, player)
        updateCurrentPlayer()
        playerPicker.setBackgroundResource(R.drawable.bg_spinner)
    }

    /** Спрашивает подтверждение и удаляет профиль игрока вместе с его рекордами. */
    private fun confirmDeletePlayer(player: PlayerEntity, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_player_title)
            .setMessage(getString(R.string.delete_player_message, player.fullName))
            .setPositiveButton(R.string.action_delete) { _, _ -> deletePlayer(player, onDeleted) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun deletePlayer(player: PlayerEntity, onDeleted: () -> Unit) {
        lifecycleScope.launch {
            val dao = AppDatabase.get(this@GameMenuActivity).playerDao()
            dao.delete(player)
            players = dao.getAll()
            if (PlayerSession.current?.id == player.id) {
                PlayerSession.current = null
                PlayerSession.persist(this@GameMenuActivity, null)
                updateCurrentPlayer()
            }
            Toast.makeText(
                this@GameMenuActivity,
                getString(R.string.player_deleted, player.fullName),
                Toast.LENGTH_SHORT
            ).show()
            onDeleted()
        }
    }

    /** Адаптер списка игроков: иконка знака зодиака, имя, курс, отметка текущего игрока и кнопка удаления. */
    private inner class PlayerPickerAdapter(
        private val onDelete: (PlayerEntity) -> Unit
    ) : BaseAdapter() {
        override fun getCount(): Int = players.size
        override fun getItem(position: Int): PlayerEntity = players[position]
        override fun getItemId(position: Int): Long = players[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = convertView
                ?: layoutInflater.inflate(R.layout.item_player_picker, parent, false)
            val player = getItem(position)
            val zodiac = runCatching { ZodiacSign.valueOf(player.zodiacSign) }.getOrNull()
            item.findViewById<ImageView>(R.id.ivZodiac)
                .setImageResource(zodiac?.imageRes ?: R.drawable.bug_rare)
            item.findViewById<TextView>(R.id.tvName).text = player.fullName
            item.findViewById<TextView>(R.id.tvDetails).text =
                getString(R.string.item_player_details, player.course)
            item.findViewById<View>(R.id.ivSelected).isVisible =
                player.id == PlayerSession.current?.id
            item.findViewById<ImageView>(R.id.ivDelete).setOnClickListener {
                onDelete(getItem(position))
            }
            return item
        }
    }

    private fun startGame() {
        if (PlayerSession.current == null) {
            Toast.makeText(this, R.string.menu_select_player_required, Toast.LENGTH_LONG).show()
            playerPicker.setBackgroundResource(R.drawable.bg_spinner_error)
            showPlayerPicker()
            return
        }
        startActivity(Intent(this, GameActivity::class.java))
    }

    private fun openPage(page: Int) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SELECTED_PAGE, page)
        startActivity(intent)
    }
}
