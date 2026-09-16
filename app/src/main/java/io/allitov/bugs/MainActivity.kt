package io.allitov.bugs

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var spCourse: Spinner
    private lateinit var sbDifficulty: SeekBar
    private lateinit var tvDifficulty: TextView
    private lateinit var cvBirthDate: CalendarView
    private lateinit var ivZodiac: ImageView
    private lateinit var tvResult: TextView

    private val difficulties = listOf("Легкий", "Средний", "Сложный", "Экстрим")
    private var birthDay = 5
    private var birthMonth = 11
    private var birthYear = 2002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etFullName = findViewById(R.id.etFullName)
        rgGender = findViewById(R.id.rgGender)
        spCourse = findViewById(R.id.spCourse)
        sbDifficulty = findViewById(R.id.sbDifficulty)
        tvDifficulty = findViewById(R.id.tvDifficulty)
        cvBirthDate = findViewById(R.id.cvBirthDate)
        ivZodiac = findViewById(R.id.ivZodiac)
        tvResult = findViewById(R.id.tvResult)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        val courses = (1..6).map { "Курс $it" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCourse.adapter = adapter

        sbDifficulty.max = difficulties.size - 1
        sbDifficulty.progress = 1
        tvDifficulty.text = difficulties[1]
        sbDifficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvDifficulty.text = difficulties[progress]
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val cal = Calendar.getInstance().apply {
            set(2005, Calendar.JANUARY, 1)
        }
        cvBirthDate.date = cal.timeInMillis
        cvBirthDate.setOnDateChangeListener { _, year, month, dayOfMonth ->
            birthYear = year
            birthMonth = month
            birthDay = dayOfMonth
        }

        btnRegister.setOnClickListener { registerPlayer() }
    }

    private fun registerPlayer() {
        val fullName = etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            etFullName.error = "Введите ФИО"
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Мужской"
            R.id.rbFemale -> "Женский"
            else -> {
                Toast.makeText(this, "Выберите пол", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val course = spCourse.selectedItemPosition + 1
        val difficulty = difficulties[sbDifficulty.progress]
        val birthDate = "%02d.%02d.%04d".format(birthDay, birthMonth + 1, birthYear)
        val zodiac = ZodiacSign.from(birthDay, birthMonth + 1)

        val player = Player(fullName, gender, course, difficulty, birthDate, zodiac)

        tvResult.text = player.toString()
        ivZodiac.setImageResource(zodiac.imageRes)
    }
}