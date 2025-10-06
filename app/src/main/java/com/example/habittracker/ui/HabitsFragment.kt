package com.example.habittracker.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.Toast
import java.util.*

class HabitsFragment : Fragment() {

    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_habits, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())

        rv = view.findViewById(R.id.rvHabits)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = HabitAdapter(::toggleDone, ::editHabit, ::deleteHabit)
        rv.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddHabit)
        fab.setOnClickListener {
            fab.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                fab.animate().scaleX(1f).scaleY(1f).setDuration(100)
                AddHabitDialog.show(requireContext()) {
                    Toast.makeText(requireContext(), "Habit added!", Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }

        refresh()
    }

    // 🔄 Refresh RecyclerView
    private fun refresh() {
        adapter.submit(store.getHabits(), store.getCompleted(todayKey()))
    }

    // ✏️ Edit Habit
    private fun editHabit(h: Habit) {
        AddHabitDialog.show(requireContext()) {
            Toast.makeText(requireContext(), "Habit updated!", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    // ❌ Delete Habit
    private fun deleteHabit(h: Habit) {
        val list = store.getHabits()
        list.removeAll { it.id == h.id }
        store.saveHabits(list)
        val set = store.getCompleted(todayKey())
        set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }

    // ✅ Toggle complete
    private fun toggleDone(h: Habit, done: Boolean) {
        val set = store.getCompleted(todayKey())
        if (done) set.add(h.id) else set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }
}

// Adapter + ViewHolder
class HabitAdapter(
    private val onToggle: (Habit, Boolean) -> Unit,
    private val onEdit: (Habit) -> Unit,
    private val onDelete: (Habit) -> Unit
) : RecyclerView.Adapter<HabitVH>() {

    private val items = mutableListOf<Habit>()
    private val completed = mutableSetOf<String>()

    fun submit(list: List<Habit>, completedSet: Set<String>) {
        items.clear(); items.addAll(list)
        completed.clear(); completed.addAll(completedSet)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitVH(v)
    }

    override fun onBindViewHolder(holder: HabitVH, pos: Int) {
        holder.bind(items[pos], completed.contains(items[pos].id), onToggle, onEdit, onDelete)
    }

    override fun getItemCount() = items.size
}

class HabitVH(v: View) : RecyclerView.ViewHolder(v) {
    fun bind(
        h: Habit,
        done: Boolean,
        onToggle: (Habit, Boolean) -> Unit,
        onEdit: (Habit) -> Unit,
        onDelete: (Habit) -> Unit
    ) {
        val cb = itemView.findViewById<CheckBox>(R.id.cbDone)
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val edit = itemView.findViewById<ImageButton>(R.id.btnEdit)
        val del = itemView.findViewById<ImageButton>(R.id.btnDelete)
        val colorBar = itemView.findViewById<View>(R.id.colorBar)

        // 🏷 Title + check
        title.text = h.title
        cb.setOnCheckedChangeListener(null)
        cb.isChecked = done
        cb.setOnCheckedChangeListener { _, isChecked -> onToggle(h, isChecked) }

        //  Color bar tint
        try {
            if (!h.color.isNullOrEmpty()) {
                colorBar.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor(h.color)
                    )
            }
        } catch (_: Exception) { }

        //  Buttons
        edit.setOnClickListener { onEdit(h) }
        del.setOnClickListener { onDelete(h) }
    }
}
