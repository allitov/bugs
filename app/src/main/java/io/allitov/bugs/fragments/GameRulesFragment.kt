package io.allitov.bugs.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import io.allitov.bugs.R

class GameRulesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.view_game_rules, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rulesHtml = resources.openRawResource(R.raw.game_rules)
            .bufferedReader()
            .use { it.readText() }

        view.findViewById<TextView>(R.id.tvRules).text = HtmlCompat.fromHtml(
            rulesHtml,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}
