package com.example.habittracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.habittracker.notify.HabitReminderScheduler

class HabitsFragment : Fragment() {

    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter

    companion object {
        const val REQUEST_KEY = "add_habit_result"
        const val RESULT_REFRESH = "refresh"
    }

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
                openHabitEditor(null)
            }
        }

        parentFragmentManager.setFragmentResultListener(
            REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean(RESULT_REFRESH, false)) {
                refresh()
            }
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (this::store.isInitialized) {
            refresh()
        }
    }

    //  Refresh RecyclerView
    private fun refresh() {
        val habits = store.getHabits()
        val completed = store.getCompleted(todayKey())
        adapter.submit(habits, completed)
        store.setHabitTotalForDay(todayKey(), habits.size)
    }

    //  Edit Habit
    private fun editHabit(h: Habit) {
        openHabitEditor(h)
    }

    private fun openHabitEditor(habit: Habit?) {
        val bundle = habit?.let { bundleOf(AddHabitFragment.ARG_HABIT_ID to it.id) }
        val navController = findNavController()
        // Navigate using the safe action defined in the navigation graph
        navController.navigate(R.id.action_habitsFragment_to_addHabitFragment, bundle)
    }

    //  Delete Habit
    private fun deleteHabit(h: Habit) {
        val list = store.getHabits()
        list.removeAll { it.id == h.id }
        store.saveHabits(list)
        val set = store.getCompleted(todayKey())
        set.remove(h.id)
        store.setCompleted(todayKey(), set)
        HabitReminderScheduler.cancel(requireContext(), h.id)
        refresh()
    }

    // Toggle complete
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
