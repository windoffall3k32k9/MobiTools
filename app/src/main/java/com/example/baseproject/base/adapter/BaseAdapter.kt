package com.example.baseproject.base.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseAdapter<T, V : ViewBinding> :
    RecyclerView.Adapter<BaseAdapter<T, V>.BaseViewHolder>() {

    val dataSet by lazy {
        initData()
    }
    var onClick: ((T) -> Unit)? = null
    var onClickIndex: ((T, Int) -> Unit)? = null

    var mRecyclerView: RecyclerView? = null

    var preSel = -1
    var selectItem = -1

    fun setSelect(index: Int) {
        preSel = selectItem
        selectItem = index

        notifyItemChanged(preSel)
        notifyItemChanged(selectItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return BaseViewHolder(provideViewBinding(parent))
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        binData(holder.item, dataSet[position])
        binDataIndex(holder.item, dataSet[position], position)
    }

    open fun setData(listItem: List<T>) {
        dataSet.clear()
        dataSet.addAll(listItem)
        notifyDataSetChanged()
    }

    fun getListData():ArrayList<T>{
        return dataSet
    }

    open fun addMoreItem(listItem: ArrayList<T>) {
        val from = dataSet.size
        dataSet.addAll(listItem)
        notifyItemRangeInserted(from, listItem.size)
    }

    open fun addData(item: T) {
        dataSet.add(item)
        notifyItemInserted(dataSet.size - 1)
    }

    open fun addData(datas: List<T>) {
        dataSet.addAll(datas)
        notifyDataSetChanged()
    }

    open fun addData(item: T, index : Int) {
        dataSet.add(index, item)
        notifyItemInserted(dataSet.size - 1)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    open fun binData(viewBinding: V, item: T){}
    open fun binDataIndex(viewBinding: V, item: T, i : Int){}

    abstract fun provideViewBinding(parent: ViewGroup): V
    open fun initData(): ArrayList<T> {
        return arrayListOf()
    }

    inner class BaseViewHolder(val item: V) : RecyclerView.ViewHolder(item.root)

}