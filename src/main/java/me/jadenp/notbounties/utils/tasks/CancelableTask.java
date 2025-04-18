package me.jadenp.notbounties.utils.tasks;

import com.cjcrafter.foliascheduler.TaskImplementation;

import java.util.HashMap;
import java.util.Map;

public abstract class CancelableTask implements Runnable {
    private TaskImplementation<Void> taskImplementation;
    private final int taskId;
    private static int taskCounter = 0;
    public static Map<Integer, CancelableTask> cancelableTaskMap = new HashMap<>();

    public static void shutdown() {
        cancelableTaskMap.values().forEach(CancelableTask::cancel);
    }

    protected CancelableTask() {
        taskId = taskCounter++;
        cancelableTaskMap.put(taskId, this);
    }

    public void setTaskImplementation(TaskImplementation<Void> taskImplementation) {
        this.taskImplementation = taskImplementation;
    }

    public void cancel() {
        taskImplementation.cancel();
        cancelableTaskMap.remove(taskId);
    }

    public int getTaskId() {
        return taskId;
    }

}
