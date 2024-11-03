package com.example.mealmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mealmate.dashboard.SavedCategoriesFragment

class SavedRecipesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.recipes_saved, container, false)

        // Find the "Return" button, make it visible, and set up the click listener
        val returnButton: TextView = view.findViewById(R.id.return_button)
        returnButton.visibility = View.VISIBLE
        returnButton.setText("< Categories")

        // Set an OnClickListener to handle the button click and navigate back
        returnButton.setOnClickListener {
            // Navigate back to SavedCategoriesFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SavedCategoriesFragment())
                .addToBackStack(null) // Add to back stack to allow navigation back
                .commit()

            // Make the "Return" button invisible again
            returnButton.visibility = View.INVISIBLE
        }

        return view
    }
}
