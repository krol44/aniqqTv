package com.aniqq.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdapterMainMenu(private val dataSet: MutableList<Start.DataSet>) :
    RecyclerView.Adapter<AdapterMainMenu.MyViewHolder>() {

    private var itemClickListener: ItemClickListener? = null

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var largeTextView: TextView? = null

        init {
            largeTextView = itemView.findViewById(R.id.textViewLarge)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.cell_main_menu, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.largeTextView?.text = dataSet[position].name

        holder.itemView.setOnClickListener {
            itemClickListener!!.onItemClickListener(position)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun itemClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClickListener(position: Int)
    }
}