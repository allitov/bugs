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
        val zodiac = zodiacSign(birthDay, birthMonth + 1)

        val player = Player(fullName, gender, course, difficulty, birthDate, zodiac)

        tvResult.text = player.toString()
        val img = zodiacImage(zodiac)
        if (img != 0) ivZodiac.setImageResource(img)
    }

    private fun zodiacSign(day: Int, month: Int): String = when (month) {
        1  -> if (day <= 19) "Козерог" else "Водолей"
        2  -> if (day <= 18) "Водолей" else "Рыбы"
        3  -> if (day <= 20) "Рыбы" else "Овен"
        4  -> if (day <= 19) "Овен" else "Телец"
        5  -> if (day <= 20) "Телец" else "Близнецы"
        6  -> if (day <= 20) "Близнецы" else "Рак"
        7  -> if (day <= 22) "Рак" else "Лев"
        8  -> if (day <= 22) "Лев" else "Дева"
        9  -> if (day <= 22) "Дева" else "Весы"
        10 -> if (day <= 22) "Весы" else "Скорпион"
        11 -> if (day <= 21) "Скорпион" else "Стрелец"
        12 -> if (day <= 21) "Стрелец" else "Козерог"
        else -> ""
    }

    private fun zodiacImage(sign: String): Int = when (sign) {
        "Овен"     -> R.drawable.zodiac_aries
        "Телец"    -> R.drawable.zodiac_taurus
        "Близнецы" -> R.drawable.zodiac_gemini
        "Рак"      -> R.drawable.zodiac_cancer
        "Лев"      -> R.drawable.zodiac_leo
        "Дева"     -> R.drawable.zodiac_virgo
        "Весы"     -> R.drawable.zodiac_libra
        "Скорпион" -> R.drawable.zodiac_scorpio
        "Стрелец"  -> R.drawable.zodiac_sagittarius
        "Козерог"  -> R.drawable.zodiac_capricorn
        "Водолей"  -> R.drawable.zodiac_aquarius
        "Рыбы"     -> R.drawable.zodiac_pisces
        else -> 0
    }
}