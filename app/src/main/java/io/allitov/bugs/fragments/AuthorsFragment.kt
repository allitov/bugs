package io.allitov.bugs.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.allitov.bugs.R

class AuthorsFragment : Fragment() {

    private val authors = listOf(
        Author("Варвара Калашникова", R.drawable.govorusha),
        Author("Александр Литовкин", R.drawable.tikovka),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.view_authors, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ListView>(R.id.lvAuthors).adapter = AuthorAdapter(requireContext())
    }

    private data class Author(val name: String, val photoRes: Int)

    private inner class AuthorAdapter(context: Context) :
        ArrayAdapter<Author>(context, 0, authors) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_author, parent, false)
            val author = authors[position]
            view.findViewById<ImageView>(R.id.ivAuthorPhoto).setImageResource(author.photoRes)
            view.findViewById<TextView>(R.id.tvAuthorName).text = author.name
            return view
        }
    }
}
