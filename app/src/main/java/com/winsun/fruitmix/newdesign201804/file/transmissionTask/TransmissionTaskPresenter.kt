package com.winsun.fruitmix.newdesign201804.file.transmissionTask

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter
import com.winsun.fruitmix.R
import com.winsun.fruitmix.callback.BaseLoadDataCallback
import com.winsun.fruitmix.callback.BaseOperateCallbackImpl
import com.winsun.fruitmix.command.BaseAbstractCommand
import com.winsun.fruitmix.file.data.model.RemoteFolder
import com.winsun.fruitmix.model.operationResult.OperationResult
import com.winsun.fruitmix.model.operationResult.OperationSuccess
import com.winsun.fruitmix.newdesign201804.component.inflateView
import com.winsun.fruitmix.newdesign201804.file.transmission.InjectTransmissionDataSource
import com.winsun.fruitmix.newdesign201804.file.transmission.TransmissionDataSource
import com.winsun.fruitmix.newdesign201804.file.transmission.model.Transmission
import com.winsun.fruitmix.newdesign201804.file.transmissionTask.data.TransmissionTaskDataSource
import com.winsun.fruitmix.newdesign201804.file.transmissionTask.data.TransmissionTaskRepository
import com.winsun.fruitmix.newdesign201804.file.transmissionTask.model.*
import com.winsun.fruitmix.recyclerview.BaseRecyclerViewAdapter
import com.winsun.fruitmix.recyclerview.SimpleViewHolder
import com.winsun.fruitmix.thread.manage.ThreadManager
import com.winsun.fruitmix.thread.manage.ThreadManagerImpl
import com.winsun.fruitmix.util.Util

import kotlinx.android.synthetic.main.transmission_task_item.view.*

class TransmissionTaskPresenter(val transmissionTaskDataSource: TransmissionTaskDataSource,
                                val transmissionDataSource: TransmissionDataSource) {

    private val transmissionTaskAdapter = TransmissionTaskAdapter()

    private val taskContainers = mutableListOf<TaskContainer>()

    fun initView(recyclerView: RecyclerView) {

        recyclerView.adapter = transmissionTaskAdapter

        transmissionTaskDataSource.getAllTransmissionTasks(object : BaseLoadDataCallback<Task> {

            override fun onSucceed(data: MutableList<Task>?, operationResult: OperationResult?) {

                getTransmissionData(data)

            }

            override fun onFail(operationResult: OperationResult?) {

            }

        })


    }

    private fun getTransmissionData(taskData: MutableList<Task>?) {
        transmissionDataSource.getTransmission(object : BaseLoadDataCallback<Transmission> {

            override fun onFail(operationResult: OperationResult?) {

                handleGetTask(taskData)

            }

            override fun onSucceed(data: MutableList<Transmission>?, operationResult: OperationResult?) {

                data?.forEach {

                    val abstractFile = RemoteFolder()
                    abstractFile.uuid = it.dirUUID

                    val btTaskParam = BTTaskParam(it)

                    val task = BTTask(abstractFile, ThreadManagerImpl.getInstance(), btTaskParam,
                            transmissionDataSource)

                    taskData?.add(task)

                }

                handleGetTask(taskData)

            }

        })
    }

    fun onDestroy() {

        taskContainers.forEach {
            it.destroy()
        }

    }

    fun startAllTask() {

        taskContainers.forEach {
            it.task.startTask()
        }

    }

    fun pauseAllTask() {

        taskContainers.forEach {
            it.task.pauseTask()
        }

    }

    fun clearAllFinishedTask() {


    }

    private fun handleGetTask(data: MutableList<Task>?) {

        data?.forEach {

            val taskContainer = TaskContainer(it)

            it.init()

            taskContainers.add(taskContainer)

        }

        transmissionTaskAdapter.setItemList(taskContainers)

        transmissionTaskAdapter.notifyDataSetChanged()

        data?.forEach {

            when (it) {
                is SMBTask -> it.setCurrentState(FinishTaskState(it.abstractFile.size, it))
                is CopyTask -> it.setCurrentState(it.getCurrentState())
                is MoveTask -> it.setCurrentState(it.getCurrentState())
                else -> it.startTask()
            }

        }

    }

    private inner class TransmissionTaskAdapter : RecyclerSwipeAdapter<SimpleViewHolder>() {

        private val mItemList: MutableList<TaskContainer> = mutableListOf()

        fun setItemList(itemList: List<TaskContainer>) {
            mItemList.clear()
            mItemList.addAll(itemList)
        }

        override fun getSwipeLayoutResourceId(position: Int): Int {
            return R.id.swipeLayout
        }

        override fun getItemCount(): Int {
            return mItemList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SimpleViewHolder {

            val view = parent?.inflateView(R.layout.transmission_task_item)

            return SimpleViewHolder(view)

        }

        override fun onBindViewHolder(holder: SimpleViewHolder?, position: Int) {

            val view = holder?.itemView
            val taskContainer = mItemList[position]

            val task = taskContainer.task

            view?.taskFileTypeIv?.setImageResource(task.abstractFile.fileTypeResID)
            view?.taskFileNameTv?.text = task.abstractFile.name
            view?.taskTypeIv?.setImageResource(task.getTypeResID())

            view?.taskStateIcon?.refresh(task.getCurrentState())

            //TODO: check pause download

/*            view?.setOnClickListener {

                val taskState = task.getCurrentState()

                if (taskState is StartingTaskState)
                    task.setCurrentState(PauseTaskState(taskState.progress, taskState.speed, task))
                else if (taskState is PauseTaskState)
                    task.setCurrentState(StartingTaskState(taskState.progress, taskState.speed, task))

            }*/

            view?.deleteTv?.setOnClickListener {

                task.cancelTask()

                transmissionTaskDataSource.deleteTransmissionTask(task, object : BaseOperateCallbackImpl() {
                    override fun onSucceed() {
                        super.onSucceed()

                        taskContainers.remove(taskContainer)

                        transmissionTaskAdapter.setItemList(taskContainers)

                        transmissionTaskAdapter.notifyItemRemoved(holder.adapterPosition)

                    }
                })

            }

        }

    }


    private inner class TaskContainer(val task: Task) : TaskStateObserver {

        init {
            task.registerObserver(this)
        }

        fun destroy() {
            task.unregisterObserver(this)
        }

        override fun notifyStateChanged(currentState: TaskState) {

            val position = taskContainers.indexOf(this)

            Log.d("Task", "notifyStateChanged position: $position")

            transmissionTaskAdapter.notifyItemChanged(position)

        }


    }

}



