package com.example.habittracker.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.habittracker.R
import com.example.habittracker.data.Habit
import com.example.habittracker.data.PrefStore
import com.example.habittracker.data.todayKey
import java.util.*

class HabitsFragment : Fragment() {
    private lateinit var store: PrefStore
    private lateinit var rv: RecyclerView
    private lateinit var adapter: HabitAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = PrefStore(requireContext())
        rv = view.findViewById(R.id.rvHabits)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = HabitAdapter(::toggleDone, ::editHabit, ::deleteHabit)
        rv.adapter = adapter

        view.findViewById<View>(R.id.fabAddHabit).setOnClickListener { showAddDialog() }
        refresh()
    }

    private fun refresh() {
        adapter.submit(store.getHabits(), store.getCompleted(todayKey()))
    }

    private fun showAddDialog(existing: Habit? = null) {
        val v = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        val et = v.findViewById<EditText>(R.id.etTitle)
        if (existing != null) et.setText(existing.title)

        val dlg = AlertDialog.Builder(requireContext()).setView(v).create()
        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }
        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isEmpty()) { et.error = "Required"; return@setOnClickListener }
            val list = store.getHabits()
            if (existing == null) list.add(Habit(UUID.randomUUID().toString(), t)) else existing.title = t
            store.saveHabits(list)
            dlg.dismiss()
            refresh()
        }
        dlg.show()
    }

    private fun editHabit(h: Habit) = showAddDialog(h)

    private fun deleteHabit(h: Habit) {
        val list = store.getHabits()
        list.removeAll { it.id == h.id }
        store.saveHabits(list)
        // also remove from today's completion
        val set = store.getCompleted(todayKey())
        set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }

    private fun toggleDone(h: Habit, done: Boolean) {
        val set = store.getCompleted(todayKey())
        if (done) set.add(h.id) else set.remove(h.id)
        store.setCompleted(todayKey(), set)
        refresh()
    }
}

private class HabitAdapter(
    val onToggle: (Habit, Boolean) -> Unit,
    val onEdit: (Habit) -> Unit,
    val onDelete: (Habit) -> Unit
) : RecyclerView.Adapter<HabitVH>() {
    private val items = mutableListOf<Habit>()
    private val completed = mutableSetOf<String>()
    fun submit(list: List<Habit>, completedSet: Set<String>) {
        items.clear(); items.addAll(list)
        completed.clear(); completed.addAll(completedSet)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(p: ViewGroup, vType: Int) =
        HabitVH(LayoutInflater.from(p.context).inflate(R.layout.item_habit, p, false))
    override fun onBindViewHolder(h: HabitVH, pos: Int) = h.bind(items[pos], completed.contains(items[pos].id), onToggle, onEdit, onDelete)
    override fun getItemCount() = items.size
}

private class HabitVH(v: View) : RecyclerView.ViewHolder(v) {
    fun bind(h: Habit, done: Boolean, onToggle: (Habit, Boolean) -> Unit, onEdit: (Habit) -> Unit, onDelete: (Habit) -> Unit) {
        val cb = itemView.findViewById<CheckBox>(R.id.cbDone)
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val edit = itemView.findViewById<ImageButton>(R.id.btnEdit)
        val del = itemView.findViewById<ImageButton>(R.id.btnDelete)
        title.text = h.title
        cb.setOnCheckedChangeListener(null)
        cb.isChecked = done
        cb.setOnCheckedChangeListener { _, isChecked -> onToggle(h, isChecked) }
        edit.setOnClickListener { onEdit(h) }
        del.setOnClickListener { onDelete(h) }
    }
}
