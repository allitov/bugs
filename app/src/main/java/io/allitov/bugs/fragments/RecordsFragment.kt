package io.allitov.bugs.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.allitov.bugs.R
import io.allitov.bugs.data.AppDatabase
import io.allitov.bugs.data.ScoreWithPlayer
import io.allitov.bugs.model.GameSettings
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Вкладка «Рекорды»: лучшие результаты раундов с фильтрами по сложности и длительности игры. */
class RecordsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var spFilterDifficulty: Spinner
    private lateinit var spFilterDuration: Spinner

    /** Все загруженные рекорды; к ним применяются фильтры. */
    private var allRecords: List<ScoreWithPlayer> = emptyList()
    private var records: List<ScoreWithPlayer> = emptyList()

    /** null — фильтр не выбран, показываются все рекорды. */
    private var difficultyFilter: String? = null
    private var durationFilter: Int? = null

    /** Длительности раундов, доступные в настройках игры. */
    private val durations = (
        GameSettings.ROUND_DURATION_STEP..GameSettings.MAX_ROUND_DURATION
            step GameSettings.ROUND_DURATION_STEP
        ).toList()

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.view_records, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.lvRecords)
        emptyView = view.findViewById(R.id.tvEmptyRecords)
        spFilterDifficulty = view.findViewById(R.id.spFilterDifficulty)
        spFilterDuration = view.findViewById(R.id.spFilterDuration)

        setupFilterSpinner(
            spFilterDifficulty,
            listOf(FILTER_ALL_DIFFICULTIES) + GameSettings.DIFFICULTY_LEVELS
        ) { position ->
            difficultyFilter =
                if (position == 0) null else GameSettings.DIFFICULTY_LEVELS[position - 1]
            applyFilter()
        }
        setupFilterSpinner(
            spFilterDuration,
            listOf(FILTER_ALL_DURATIONS) + durations.map { formatDuration(it) }
        ) { position ->
            durationFilter = if (position == 0) null else durations[position - 1]
            applyFilter()
        }

        listView.adapter = RecordsAdapter(requireContext())

        // Заготовка пустого списка, пока данные не загружены.
        showRecords(records)
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun setupFilterSpinner(spinner: Spinner, items: List<String>, onSelect: (Int) -> Unit) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                onSelect(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            allRecords = AppDatabase.get(requireContext()).scoreDao().getTopWithPlayers()
            applyFilter()
        }
    }

    /** Применяет выбранные фильтры; если ни один не выбран, показываются все рекорды. */
    private fun applyFilter() {
        records = allRecords.filter { record ->
            (difficultyFilter == null || record.score.difficulty == difficultyFilter) &&
                (durationFilter == null || record.score.roundDurationSeconds == durationFilter)
        }
        showRecords(records)
    }

    private fun showRecords(newRecords: List<ScoreWithPlayer>) {
        records = newRecords
        val adapter = listView.adapter as RecordsAdapter
        adapter.clear()
        adapter.addAll(records)
        adapter.notifyDataSetChanged()
        listView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        emptyView.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes == 0) {
            "$seconds сек"
        } else {
            "$minutes мин $remainingSeconds сек"
        }
    }

    private inner class RecordsAdapter(context: Context) :
        ArrayAdapter<ScoreWithPlayer>(context, 0, ArrayList(records)) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_record, parent, false)
            val record = records[position]
            view.findViewById<TextView>(R.id.tvRecordPlace).text = "${position + 1}."
            view.findViewById<TextView>(R.id.tvRecordPlayerName).text = record.player.fullName
            view.findViewById<TextView>(R.id.tvRecordDifficulty).text =
                "Уровень сложности: ${record.score.difficulty} · " +
                    "Длительность: ${formatDuration(record.score.roundDurationSeconds)}"
            val hits = record.score.hits
            val misses = record.score.misses
            val taps = hits + misses
            val accuracy = if (taps == 0) 0 else hits * 100 / taps
            view.findViewById<TextView>(R.id.tvRecordDetails).text =
                "Точность: $accuracy% · ${dateFormat.format(Date(record.score.playedAt))}"
            view.findViewById<TextView>(R.id.tvRecordScore).text =
                getString(R.string.results_score, record.score.score)
            return view
        }
    }

    private companion object {
        const val FILTER_ALL_DIFFICULTIES = "Все сложности"
        const val FILTER_ALL_DURATIONS = "Любая длительность"
    }
}
